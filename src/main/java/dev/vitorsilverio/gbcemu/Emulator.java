package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.cartridge.CartFactory;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.controller.IdleController;
import dev.vitorsilverio.gbcemu.controller.KeyboardController;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.debug.DebugController;
import dev.vitorsilverio.gbcemu.debug.DebugWindow;
import dev.vitorsilverio.gbcemu.interrupt.InterruptManager;
import dev.vitorsilverio.gbcemu.memory.*;
import dev.vitorsilverio.gbcemu.misc.*;
import dev.vitorsilverio.gbcemu.peripherals.Joypad;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.snapshot.EmulatorState;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateFile;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateMetadata;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;

import java.io.File;
import java.time.Instant;

public class Emulator {

    private static final int DOTS_PER_FRAME = 70224;
    private static final long NANOS_PER_FRAME = 16_742_706L;

    private final Cpu cpu;
    private final Timer timer;
    private final Ppu ppu;
    private final Apu apu;
    private final Serial serial;
    private final HDMA hdma;
    private final DMA dma;
    private final Bios bios;
    private final WorkRam workRam;
    private final ZeroPage zeroPage;
    private final Key0 key0;
    private final Key1 key1;
    private final InfraredPort infraredPort;
    private final EmulatorWindow window;
    private final GameSharkDevice gameSharkDevice;
    private final Cart cart;
    private final File romFile;
    private final boolean throttled;
    private final boolean cartridgeCgbCompatible;
    private final DebugController debugController = new DebugController();
    private final Bus bus;
    private final Object stateLock = new Object();
    private volatile boolean paused;
    private volatile boolean stopped;
    private int dots;
    private long frameNumber;
    private long frameStart = System.nanoTime();


    public Emulator(File biosFile, File romFile, File saveFile) {
        this(biosFile, romFile, saveFile, false, null);
    }

    public Emulator(File biosFile, File romFile, File saveFile, boolean headless) {
        this(biosFile, romFile, saveFile, headless, null);
    }

    public Emulator(File biosFile, File romFile, File saveFile, boolean headless, EmulatorWindow window) {
        this.bus = new Bus();
        this.gameSharkDevice = new GameSharkDevice();
        this.bus.addMemorySpace(gameSharkDevice);
        if (biosFile != null) {
            bios = new Bios(biosFile);
            bus.addMemorySpace(bios);
        } else {
            bios = null;
        }
        this.romFile = romFile;
        this.cart = CartFactory.fromFile(romFile, saveFile);
        this.cartridgeCgbCompatible = cart.getHeader().isCgbCompatible();
        this.cpu = new Cpu(bus);
        this.timer = new Timer(bus);
        this.ppu = new Ppu(bus, biosFile != null || cartridgeCgbCompatible);
        this.apu = headless ? Apu.muted() : new Apu();
        this.hdma = new HDMA(bus);
        this.dma = new DMA(bus);
        this.throttled = !headless;
        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        Controller controller = headless ? new IdleController() : new KeyboardController();
        var joypad = new Joypad(bus, controller);
        bus.addMemorySpace(joypad);
        workRam = new WorkRam();
        bus.addMemorySpace(workRam);
        var echoRam = new EchoRam(workRam);
        bus.addMemorySpace(echoRam);
        zeroPage = new ZeroPage();
        bus.addMemorySpace(zeroPage);
        bus.addMemorySpace(cart);
        key0 = new Key0(ppu::setCgbMode);
        key1 = new Key1();
        infraredPort = new InfraredPort();
        bus.addMemorySpace(key0);
        bus.addMemorySpace(key1);
        bus.addMemorySpace(infraredPort);
        bus.addMemorySpace(new UnusedIoRegisters());
        this.window = headless ? null : window;
        if (this.window != null) {
            this.window.attach(ppu, (KeyboardController) controller);
        }
        this.serial = new Serial(bus);
        bus.addMemorySpace(serial);
        cpu.setCycleCallback(this::tickSystemCycle);
    }


    public void start() {
        while (!stopped) {
            if (paused) {
                sleepNanos(2_000_000);
                continue;
            }
            if (debugController.shouldBreakAtPc(cpu.getPc())) {
                paused = true;
                continue;
            }
            synchronized (stateLock) {
                if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                    cpu.tick();
                } else {
                    tickSystemCycle();
                }
            }
        }
        if (window != null) {
            window.detach(ppu);
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        paused = false;
        frameStart = System.nanoTime();
    }

    public void stop() {
        stopped = true;
        paused = false;
    }

    public void openDebugger() {
        DebugWindow.open(cpu, cpu.getBus(), ppu, debugController, this::pause, this::resume);
    }

    public void openCheats() {
        new CheatsWindow(gameSharkDevice);
    }

    private void tickSystemCycle() {
        if (hdma.isActive()) {
            if (hdma.isHBlankMode() && ppu.isHBlank()) {
                hdma.tick();
            }
            if (hdma.isGeneralPurposeMode()) {
                hdma.tick();
            }
        }
        if (dma.isActive()) {
            dma.tick();
        }

        timer.tick();
        serial.tick();
        ppu.tick();
        apu.tick();
        if (window != null && ppu.consumeFrameReady()) {
            window.renderFrame(ppu);
        }
        dots++;
        if (throttled && dots >= DOTS_PER_FRAME) {
            long elapsed = System.nanoTime() - frameStart;
            if (elapsed < NANOS_PER_FRAME) {
                sleepNanos(NANOS_PER_FRAME - elapsed);
            }
            dots = 0;
            frameNumber++;
            frameStart = System.nanoTime();
        } else if (dots >= DOTS_PER_FRAME) {
            dots = 0;
            frameNumber++;
        }
    }

    private void sleepNanos(long nanos) {
        try {
            Thread.sleep(nanos / 1_000_000L, (int) (nanos % 1_000_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void skipBios() {
        cpu.getBus().write(0xFF50, (byte) 0x01);
        ppu.setCgbMode(cartridgeCgbCompatible);
        cpu.setPc(0x100);
        cpu.setSp(0xfffe);
        cpu.setA((byte) 0x01);
        cpu.setB((byte) 0x00);
        cpu.setC((byte) 0x13);
        cpu.setD((byte) 0x00);
        cpu.setE((byte) 0xD8);
        cpu.setH((byte) 0x01);
        cpu.setL((byte) 0x4D);

    }

    public SaveStateFile createSaveStateFile() {
        synchronized (stateLock) {
            return new SaveStateFile(
                    SaveStateFile.CURRENT_FORMAT_VERSION,
                    new SaveStateMetadata(
                            Instant.now(),
                            cart.getHeader().getTitle(),
                            romFile == null ? "" : romFile.getAbsolutePath(),
                            cart.getHeader().getCartridgeType().name(),
                            frameNumber,
                            cpu.getPc(),
                            ppu.copyFrameBufferArgb(),
                            160,
                            144
                    ),
                    createEmulatorState()
            );
        }
    }

    public EmulatorState createEmulatorState() {
        synchronized (stateLock) {
            var version = EmulatorState.CURRENT_VERSION;
            return new EmulatorState(
                    version,
                    bus.findMemorySpace(InterruptManager.class).map(interrupts -> interrupts.createSnapshot(version)).orElse(null),
                    bios == null ? null : bios.createSnapshot(version),
                    cpu.createSnapshot(version),
                    timer.createSnapshot(version),
                    ppu.createSnapshot(version),
                    ppu.getVideoRam().createSnapshot(version),
                    ppu.getOam().createSnapshot(version),
                    serial.createSnapshot(version),
                    hdma.createSnapshot(version),
                    dma.createSnapshot(version),
                    workRam.createSnapshot(version),
                    zeroPage.createSnapshot(version),
                    cart.createSnapshot(version),
                    key0.createSnapshot(version),
                    key1.createSnapshot(version),
                    infraredPort.createSnapshot(version)
            );
        }
    }

    public void restoreSaveStateFile(SaveStateFile saveStateFile) {
        restoreEmulatorState(saveStateFile.state());
    }

    public void restoreEmulatorState(EmulatorState emulatorState) {
        pause();
        synchronized (stateLock) {
            restore(interruptManager(), emulatorState.interruptManager());
            restore(bios, emulatorState.bios());
            restore(cpu, emulatorState.cpu());
            restore(timer, emulatorState.timer());
            restore(ppu, emulatorState.ppu());
            restore(ppu.getVideoRam(), emulatorState.videoRam());
            restore(ppu.getOam(), emulatorState.oam());
            restore(serial, emulatorState.serial());
            restore(hdma, emulatorState.hdma());
            restore(dma, emulatorState.dma());
            restore(workRam, emulatorState.workRam());
            restore(zeroPage, emulatorState.zeroPage());
            restore(cart, emulatorState.cart());
            restore(key0, emulatorState.key0());
            restore(key1, emulatorState.key1());
            restore(infraredPort, emulatorState.infrared());
        }
        resume();
    }

    private InterruptManager interruptManager() {
        return bus.findMemorySpace(InterruptManager.class).orElseThrow();
    }

    private void restore(Snapshottable snapshottable, Snapshot snapshot) {
        if (snapshottable == null || snapshot == null) {
            return;
        }
        snapshottable.restoreSnapshot(snapshot);
    }

}
