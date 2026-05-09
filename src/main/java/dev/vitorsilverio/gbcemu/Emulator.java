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
import dev.vitorsilverio.gbcemu.memory.*;
import dev.vitorsilverio.gbcemu.misc.*;
import dev.vitorsilverio.gbcemu.peripherals.Joypad;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    private final EmulatorWindow window;
    private final GameSharkDevice gameSharkDevice;
    private final boolean throttled;
    private final boolean cartridgeCgbCompatible;
    private final DebugController debugController = new DebugController();
    private final Bus bus;
    private volatile boolean paused;
    private volatile boolean stopped;
    private int dots;
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
            Bios bios = new Bios(biosFile);
            bus.addMemorySpace(bios);
        }
        Cart cart = CartFactory.fromFile(romFile, saveFile);
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
        var workRam = new WorkRam();
        bus.addMemorySpace(workRam);
        var echoRam = new EchoRam(workRam);
        bus.addMemorySpace(echoRam);
        var zeroPage = new ZeroPage();
        bus.addMemorySpace(zeroPage);
        bus.addMemorySpace(cart);
        bus.addMemorySpace(new Key0(ppu::setCgbMode));
        bus.addMemorySpace(new Key1());
        bus.addMemorySpace(new InfraredPort());
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
            if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                cpu.tick();
            } else {
                tickSystemCycle();
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
            frameStart = System.nanoTime();
        } else if (dots >= DOTS_PER_FRAME) {
            dots = 0;
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

    public List<Snapshot> createSystemSnapthot() {
        var version = 1;
        var systemSnapShot = new ArrayList<Snapshot>();
        systemSnapShot.add(cpu.createSnapshot(version));
        systemSnapShot.add(ppu.getVideoRam().createSnapshot(version));
        systemSnapShot.add(ppu.getOam().createSnapshot(version));

        systemSnapShot.addAll(bus.getMemorySpaces().stream()
                .filter(m -> m instanceof Snapshottable)
                .map(m -> ((Snapshottable)m).createSnapshot(version)).toList());
        return systemSnapShot;
    }

    public void restoreSystemSnapshot(List<Snapshot> systemSnapShot) throws Exception {
        pause();
        for (Snapshot snapshot: systemSnapShot) {
            switch (snapshot.className()){
                case ("dev.vitorsilverio.gbcemu.cpu.Cpu"):
                    cpu.restoreSnapshot(snapshot);
                    break;
                case ("dev.vitorsilverio.gbcemu.ppu.VideoRam"):
                    ppu.getVideoRam().restoreSnapshot(snapshot);
                    break;
                case ("dev.vitorsilverio.gbcemu.ppu.OamRAM"):
                    ppu.getOam().restoreSnapshot(snapshot);
                    break;
                default:
                    Class<?> clazz = Class.forName(snapshot.className());
                    if (clazz.isAssignableFrom(MemorySpace.class)) {
                        var snapshotable = (Snapshottable)bus.findSpace(clazz).orElseThrow();
                        snapshotable.restoreSnapshot(snapshot);
                    }
            }
        }
        resume();
    }
}
