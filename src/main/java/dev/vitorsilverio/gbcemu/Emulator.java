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
import dev.vitorsilverio.gbcemu.snapshot.RewindBuffer;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateFile;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateMetadata;

import java.io.File;
import java.time.Instant;

public class Emulator {

    private static final int DOTS_PER_FRAME = 70224;
    private static final long NANOS_PER_FRAME = 16_742_706L;
    private static final int REWIND_SECONDS = 15;
    private static final int REWIND_CAPTURE_INTERVAL_FRAMES = 30;
    private static final int REWIND_CAPACITY = (60 / REWIND_CAPTURE_INTERVAL_FRAMES) * REWIND_SECONDS;

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
    private final RewindBuffer rewindBuffer = new RewindBuffer(REWIND_CAPACITY);
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
            recordRewindSnapshot();
            frameStart = System.nanoTime();
        } else if (dots >= DOTS_PER_FRAME) {
            dots = 0;
            frameNumber++;
            recordRewindSnapshot();
        }
    }

    private void recordRewindSnapshot() {
        if (frameNumber % REWIND_CAPTURE_INTERVAL_FRAMES != 0) {
            return;
        }
        rewindBuffer.add(createSaveStateFile());
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
                    interruptManager().saveState(),
                    bios == null ? null : bios.saveState(),
                    cpu.saveState(),
                    timer.saveState(),
                    ppu.saveState(),
                    ppu.getVideoRam().saveState(),
                    ppu.getOam().saveState(),
                    apu.saveState(),
                    serial.saveState(),
                    hdma.saveState(),
                    dma.saveState(),
                    workRam.saveState(),
                    zeroPage.saveState(),
                    cart.saveState(),
                    key0.saveState(),
                    key1.saveState(),
                    infraredPort.saveState()
            );
        }
    }

    public void restoreSaveStateFile(SaveStateFile saveStateFile) {
        restoreEmulatorState(saveStateFile.state());
    }

    public boolean rewindOneSnapshot() {
        var snapshot = rewindBuffer.popLatest();
        snapshot.ifPresent(this::restoreSaveStateFile);
        return snapshot.isPresent();
    }

    public void restoreEmulatorState(EmulatorState emulatorState) {
        pause();
        synchronized (stateLock) {
            interruptManager().loadState(emulatorState.interruptManager());
            if (bios != null && emulatorState.bios() != null) {
                bios.loadState(emulatorState.bios());
            }
            cpu.loadState(emulatorState.cpu());
            timer.loadState(emulatorState.timer());
            ppu.loadState(emulatorState.ppu());
            ppu.getVideoRam().loadState(emulatorState.videoRam());
            ppu.getOam().loadState(emulatorState.oam());
            apu.loadState(emulatorState.apu());
            serial.loadState(emulatorState.serial());
            hdma.loadState(emulatorState.hdma());
            dma.loadState(emulatorState.dma());
            workRam.loadState(emulatorState.workRam());
            zeroPage.loadState(emulatorState.zeroPage());
            cart.loadState(emulatorState.cart());
            key0.loadState(emulatorState.key0());
            key1.loadState(emulatorState.key1());
            infraredPort.loadState(emulatorState.infrared());
        }
        resume();
    }

    private InterruptManager interruptManager() {
        return bus.findMemorySpace(InterruptManager.class).orElseThrow();
    }

}
