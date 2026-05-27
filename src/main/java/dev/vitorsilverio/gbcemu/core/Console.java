package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.audio.AudioOutput;
import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.cartridge.CartFactory;
import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.connection.DisconnectedPhysicalConnection;
import dev.vitorsilverio.gbcemu.controller.CompositeController;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.controller.GamepadController;
import dev.vitorsilverio.gbcemu.controller.IdleController;
import dev.vitorsilverio.gbcemu.controller.KeyboardController;
import dev.vitorsilverio.gbcemu.controller.RumbleSink;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.CpuState;
import dev.vitorsilverio.gbcemu.debug.CpuDebugWindow;
import dev.vitorsilverio.gbcemu.debug.DebugController;
import dev.vitorsilverio.gbcemu.debug.Disassembler;
import dev.vitorsilverio.gbcemu.gui.*;
import dev.vitorsilverio.gbcemu.interrupt.InterruptManager;
import dev.vitorsilverio.gbcemu.interrupt.InterruptState;
import dev.vitorsilverio.gbcemu.memory.*;
import dev.vitorsilverio.gbcemu.misc.*;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.peripherals.*;
import dev.vitorsilverio.gbcemu.ppu.CgbCompatibilityPaletteSelection;
import dev.vitorsilverio.gbcemu.ppu.CgbCompatibilityPaletteSelector;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.ppu.PpuMode;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;
import dev.vitorsilverio.gbcemu.snapshot.EmulatorState;
import dev.vitorsilverio.gbcemu.snapshot.RewindBuffer;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateFile;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateMetadata;
import dev.vitorsilverio.gbcemu.util.DebugJson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.BooleanSupplier;

public class Console {

    private static final int DOTS_PER_FRAME = 70224;
    private static final long NANOS_PER_FRAME = 16_742_706L;
    private static final double TARGET_FPS = 1_000_000_000.0 / NANOS_PER_FRAME;
    private final Cpu cpu;
    private final Timer timer;
    private final Ppu ppu;
    private final Apu apu;
    private final AudioOutput audioOutput;
    private final Serial serial;
    private final HDMA hdma;
    private final DMA dma;
    private final Bios bios;
    private final WorkRam workRam;
    private final ZeroPage zeroPage;
    private final Key0 key0;
    private final Key1 key1;
    private final InfraredPort infraredPort;
    private final CgbUndocumentedRegisters cgbUndocumentedRegisters;
    private final EmulatorWindow window;
    private EmulatorWindow detachedDisplayWindow;
    private final KeyboardController keyboardController;
    private final GamepadController gamepadController;
    private final Controller controller;
    private final GameSharkDevice gameSharkDevice;
    private final Cart cart;
    private final SuperGameBoy superGameBoy;
    private final File romFile;
    private final File saveFile;
    private final boolean throttled;
    private final boolean externallyThrottled;
    private final boolean cartridgeCgbCompatible;
    private final DebugController debugController = new DebugController();
    private final Bus bus;
    private RewindBuffer rewindBuffer;
    private AppSettings settings;
    private final Object stateLock = new Object();
    private volatile boolean paused;
    private volatile boolean stopped;
    private int dots;
    private long frameNumber;
    private volatile long systemCycles;
    private long frameStart = System.nanoTime();
    private long nextFrameDeadline = frameStart + NANOS_PER_FRAME;
    private long performanceStatsStart = System.nanoTime();
    private int performanceStatsFrames;
    private DebugStepMode debugStepMode = DebugStepMode.NONE;
    private long debugStepTargetFrame;
    private int debugStepStartLine;
    private long stopAfterFrames = -1;
    private boolean fastForwardAudioMuted;
    private boolean rumbleOutputActive;
    private static final DateTimeFormatter DEBUG_DUMP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private final LinkCable linkCable;


    public Console(File biosFile, File romFile, File saveFile) {
        this(biosFile, romFile, saveFile, false, null);
    }

    public Console(File biosFile, File romFile, File saveFile, boolean headless) {
        this(biosFile, romFile, saveFile, headless, null);
    }

    public Console(File biosFile, File romFile, File saveFile, boolean headless, EmulatorWindow window) {
        this(biosFile, romFile, saveFile, headless, window, AppSettings.defaults());
    }

    public Console(File biosFile, File romFile, File saveFile, boolean headless, EmulatorWindow window, AppSettings settings) {
        this(biosFile, romFile, saveFile, headless, window, settings, null, null, null);
    }

    public Console(
            File biosFile,
            File romFile,
            File saveFile,
            boolean headless,
            EmulatorWindow window,
            AppSettings settings,
            Controller controllerOverride,
            KeyListener keyListenerOverride,
            LinkCable linkCableOverride
    ) {
        this(biosFile, romFile, saveFile, headless, window, settings, controllerOverride, keyListenerOverride, linkCableOverride, false);
    }

    public Console(
            File biosFile,
            File romFile,
            File saveFile,
            boolean headless,
            EmulatorWindow window,
            AppSettings settings,
            Controller controllerOverride,
            KeyListener keyListenerOverride,
            LinkCable linkCableOverride,
            boolean secondaryDisplay
    ) {
        this(biosFile, romFile, saveFile, headless, window, settings, controllerOverride, keyListenerOverride, linkCableOverride, secondaryDisplay, false);
    }

    public Console(
            File biosFile,
            File romFile,
            File saveFile,
            boolean headless,
            EmulatorWindow window,
            AppSettings settings,
            Controller controllerOverride,
            KeyListener keyListenerOverride,
            LinkCable linkCableOverride,
            boolean secondaryDisplay,
            boolean externallyThrottled
    ) {
        this.settings = settings.normalized();
        this.rewindBuffer = new RewindBuffer(this.settings.rewindCapacity());
        this.bus = new Bus();
        this.debugController.setWatchpointsChangedListener(this::updateMemoryAccessListener);
        this.gameSharkDevice = new GameSharkDevice();
        this.bus.addMemorySpace(gameSharkDevice);
        this.romFile = romFile;
        this.saveFile = saveFile;
        this.cart = CartFactory.fromFile(romFile, saveFile, this::currentRtcEpochSeconds);
        this.cartridgeCgbCompatible = cart.getHeader().isCgbCompatible();
        boolean superGameBoyEnabled = this.settings.superGameBoyBordersEnabled() && cart.getHeader().isSgbEnhanced();
        this.cpu = new Cpu(bus);
        this.timer = new Timer(bus);
        this.ppu = new Ppu(bus, !superGameBoyEnabled && (biosFile != null || cartridgeCgbCompatible));
        this.superGameBoy = new SuperGameBoy(superGameBoyEnabled, ppu);
        this.audioOutput = headless ? AudioOutput.muted() : AudioOutput.createDefault(48_000);
        this.audioOutput.applyEnhancement(this.settings.normalizedAudioEnhancement());
        this.apu = new Apu(audioOutput);
        this.apu.applyDebugVolumes(
                this.settings.audioMasterVolume(),
                this.settings.audioLeftVolume(),
                this.settings.audioRightVolume(),
                this.settings.audioChannelVolumes(),
                this.settings.audioChannelMuted()
        );
        this.timer.setDivApuListener(this.apu::clockFrameSequencer);
        this.hdma = new HDMA(bus);
        this.dma = new DMA(bus);
        this.throttled = !headless;
        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        keyboardController = headless || controllerOverride != null ? null : new KeyboardController(this.settings);
        gamepadController = headless || controllerOverride != null ? null : new GamepadController(this.settings.gamepadConfig(0));
        this.externallyThrottled = externallyThrottled;
        this.controller = controllerOverride != null
                ? controllerOverride
                : headless ? new IdleController() : new CompositeController(keyboardController, gamepadController);
        var joypad = new Joypad(bus, this.controller, superGameBoy);
        bus.addMemorySpace(joypad);
        workRam = new WorkRam();
        bus.addMemorySpace(workRam);
        var echoRam = new EchoRam(workRam);
        bus.addMemorySpace(echoRam);
        zeroPage = new ZeroPage();
        bus.addMemorySpace(zeroPage);
        key0 = new Key0(ppu::setCgbMode);
        key1 = new Key1();
        infraredPort = new InfraredPort();
        cgbUndocumentedRegisters = new CgbUndocumentedRegisters();
        if (biosFile != null) {
            bios = new Bios(biosFile, key0::lock);
            bus.addMemorySpace(bios);
        } else {
            bios = null;
        }
        bus.addMemorySpace(cart);
        bus.addMemorySpace(key0);
        bus.addMemorySpace(key1);
        bus.addMemorySpace(infraredPort);
        bus.addMemorySpace(cgbUndocumentedRegisters);
        bus.addMemorySpace(new UnusedIoRegisters());
        this.window = headless ? null : window;
        if (this.window != null) {
            KeyListener keyListener = keyListenerOverride != null ? keyListenerOverride : keyboardController;
            if (secondaryDisplay) {
                this.window.attachSecondary(ppu, keyListener, superGameBoy);
            } else {
                this.window.attach(ppu, keyListener, superGameBoy);
            }
        }
        this.linkCable = linkCableOverride != null
                ? linkCableOverride
                : new LinkCable(new DisconnectedPhysicalConnection());
        this.serial = new Serial(bus, linkCable);
        bus.addMemorySpace(serial);
        cpu.setCycleCallback(this::tickSystemCycle);
    }


    int tick() {
        long cyclesBefore = systemCycles;
        boolean stepInstruction = debugController.consumeInstructionStep();
        startPendingDebugStep();
        if (stopped || paused && !stepInstruction) {
            return 0;
        }
        if (!stepInstruction && debugController.shouldBreakAtPc(cpu.getPc())) {
            paused = true;
            debugStepMode = DebugStepMode.NONE;
            return 0;
        }
        if (!stepInstruction && debugController.shouldBreakOnMemoryAccess()) {
            paused = true;
            debugStepMode = DebugStepMode.NONE;
            return 0;
        }
        synchronized (stateLock) {
            if (stopped) {
                return 0;
            }
            if (hdmaBlocksCpu()) {
                tickSystemCycle();
            } else {
                cpu.tick();
            }
        }
        if (stepInstruction) {
            paused = true;
        }
        completeDebugStepIfNeeded();
        return cyclesAdvancedSince(cyclesBefore);
    }

    private int cyclesAdvancedSince(long cyclesBefore) {
        long advanced = systemCycles - cyclesBefore;
        if (advanced <= 0) {
            return 0;
        }
        return advanced > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) advanced;
    }

    long frameNumber() {
        return frameNumber;
    }

    long systemCycles() {
        return systemCycles;
    }

    boolean isStopped() {
        return stopped;
    }

    void resetExternalThrottleClock() {
        frameStart = System.nanoTime();
        nextFrameDeadline = frameStart + NANOS_PER_FRAME;
        performanceStatsStart = frameStart;
        performanceStatsFrames = 0;
    }

    void markFrameClock(long now) {
        frameStart = now;
    }

    long targetFrameNanos() {
        if (!isFastForwardActive()) {
            return NANOS_PER_FRAME;
        }
        return Math.max(1L, NANOS_PER_FRAME / Math.max(1, settings.turboMultiplier()));
    }

    void detachDisplay() {
        if (window != null) {
            window.detach(ppu);
        }
        closeDetachedDisplay();
    }

    public void openDetachedDisplay(String title) {
        if (detachedDisplayWindow != null && detachedDisplayWindow.isOpen()) {
            detachedDisplayWindow.show();
            return;
        }
        detachedDisplayWindow = EmulatorWindow.detachedDisplay(title, settings);
        detachedDisplayWindow.attach(ppu, null, superGameBoy);
        detachedDisplayWindow.show();
        detachedDisplayWindow.renderFrame(ppu);
    }

    private void closeDetachedDisplay() {
        if (detachedDisplayWindow != null) {
            detachedDisplayWindow.dispose();
            detachedDisplayWindow = null;
        }
    }

    private void startPendingDebugStep() {
        if (!paused || debugStepMode != DebugStepMode.NONE) {
            return;
        }
        if (debugController.consumeFrameStep()) {
            debugStepMode = DebugStepMode.FRAME;
            debugStepTargetFrame = frameNumber + 1;
            paused = false;
            return;
        }
        if (debugController.consumeScanlineStep()) {
            debugStepMode = DebugStepMode.SCANLINE;
            debugStepStartLine = ppu.debugSnapshot().line();
            paused = false;
            return;
        }
        if (debugController.consumeRunUntilHBlank()) {
            debugStepMode = DebugStepMode.HBLANK;
            paused = false;
            return;
        }
        if (debugController.consumeRunUntilVBlank()) {
            debugStepMode = DebugStepMode.VBLANK;
            paused = false;
        }
    }

    private void completeDebugStepIfNeeded() {
        if (debugStepMode == DebugStepMode.FRAME && frameNumber >= debugStepTargetFrame) {
            paused = true;
            debugStepMode = DebugStepMode.NONE;
            return;
        }
        if (debugStepMode == DebugStepMode.SCANLINE && ppu.debugSnapshot().line() != debugStepStartLine) {
            paused = true;
            debugStepMode = DebugStepMode.NONE;
            return;
        }
        PpuMode mode = ppu.debugSnapshot().mode();
        if (debugStepMode == DebugStepMode.HBLANK && mode == PpuMode.HBLANK) {
            paused = true;
            debugStepMode = DebugStepMode.NONE;
            return;
        }
        if (debugStepMode == DebugStepMode.VBLANK && mode == PpuMode.VBLANK) {
            paused = true;
            debugStepMode = DebugStepMode.NONE;
        }
    }

    public void pause() {
        if (isLinkConnectionActive()) {
            return;
        }
        pauseInternal();
    }

    void pauseLinkedSession() {
        pauseInternal();
    }

    private void pauseInternal() {
        paused = true;
    }

    public boolean isPaused() {
        return paused;
    }

    public void resume() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        paused = false;
        frameStart = System.nanoTime();
        nextFrameDeadline = frameStart + NANOS_PER_FRAME;
        performanceStatsStart = frameStart;
        performanceStatsFrames = 0;
    }

    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        paused = false;
        synchronized (stateLock) {
            cart.flushSave();
        }
        if (controller instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        } else if (gamepadController != null) {
            gamepadController.close();
        }
        updateRumbleOutput(false);
        linkCable.disconnect();
        apu.close();
        closeDetachedDisplay();
    }

    public void stopAfterFrames(long frames) {
        stopAfterFrames = frames <= 0 ? -1 : frames;
    }

    public void openCheats() {
        new CheatsWindow(gameSharkDevice);
    }

    public void openAudioDebugger() {
        new AudioDebugWindow(apu);
    }

    public String serialTranscript() {
        return serial.transcript();
    }

    public void applySettings(AppSettings settings) {
        this.settings = settings.normalized();
        apu.applyDebugVolumes(
                this.settings.audioMasterVolume(),
                this.settings.audioLeftVolume(),
                this.settings.audioRightVolume(),
                this.settings.audioChannelVolumes(),
                this.settings.audioChannelMuted()
        );
        audioOutput.applyEnhancement(this.settings.normalizedAudioEnhancement());
        if (detachedDisplayWindow != null && detachedDisplayWindow.isOpen()) {
            detachedDisplayWindow.applySettings(this.settings);
        }
        rewindBuffer = new RewindBuffer(this.settings.rewindCapacity());
        if (keyboardController != null) {
            keyboardController.applySettings(this.settings);
        }
        if (gamepadController != null) {
            gamepadController.applySettings(this.settings.gamepadConfig(0));
        }
    }

    private long currentRtcEpochSeconds() {
        return Instant.now().getEpochSecond() + settings.rtcOffsetTotalSeconds();
    }

    public void openMemoryDebugger() {
        new MemoryDebugWindow(bus, this::isPaused);
    }

    public void openPpuDebugger() {
        new PpuDebugWindow(ppu, superGameBoy);
    }

    public void openCpuDebugger() {
        new CpuDebugWindow(cpu, bus, ppu, debugController, linkCable);
    }

    public void openCartDebugger() {
        new CartDebugWindow(cart);
    }

    Bus debugBus() {
        return bus;
    }

    Cpu debugCpu() {
        return cpu;
    }

    Apu debugApu() {
        return apu;
    }

    Cart debugCart() {
        return cart;
    }

    DebugController debugController() {
        return debugController;
    }

    LinkCable debugLinkCable() {
        return linkCable;
    }

    BooleanSupplier debugPausedSupplier() {
        return this::isPaused;
    }

    Ppu debugPpu() {
        return ppu;
    }

    SuperGameBoy debugSuperGameBoy() {
        return superGameBoy;
    }

    private void updateMemoryAccessListener() {
        bus.setMemoryAccessListener(debugController.hasWatchpoints() ? debugController.memoryAccessListener() : null);
    }

    public File dumpDebugBundle() {
        return dumpDebugBundle("");
    }

    public File romFile() {
        return romFile;
    }

    File dumpDebugBundle(String label) {
        synchronized (stateLock) {
            String suffix = label == null || label.isBlank() ? "" : "-" + label.replaceAll("[^A-Za-z0-9._-]", "_");
            String baseName = "debug-bundle-" + DEBUG_DUMP_TIMESTAMP.format(Instant.now()) + suffix;
            String frameFilename = baseName + "-frame.png";
            String sgbBorderFilename = baseName + "-sgb-border.png";
            String sgbFrameFilename = baseName + "-sgb-frame.png";
            String sgbAttributesFilename = baseName + "-sgb-attributes.png";
            writeDebugFramePng(frameFilename);
            boolean wroteSgbBorder = writeSgbBorderPng(sgbBorderFilename);
            boolean wroteSgbFrame = writeSgbFramePng(sgbFrameFilename);
            boolean wroteSgbAttributes = writeSgbAttributesPng(sgbAttributesFilename);
            return DebugJson.writeTargetFile(
                    baseName + ".json",
                    debugBundleJson(
                            frameFilename,
                            wroteSgbBorder ? sgbBorderFilename : "",
                            wroteSgbFrame ? sgbFrameFilename : "",
                            wroteSgbAttributes ? sgbAttributesFilename : ""
                    ),
                    "Failed to dump debug bundle"
            );
        }
    }

    private void writeDebugFramePng(String filename) {
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            ImageIO.write(debugFrameImage(), "png", new File(target, filename));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump debug frame", e);
        }
    }

    private BufferedImage debugFrameImage() {
        int[] pixels = ppu.copyFrameBufferArgb();
        BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 160, 144, pixels, 0, 160);
        return image;
    }

    private boolean writeSgbBorderPng(String filename) {
        BufferedImage border = superGameBoy.copyBorderImage();
        if (border == null) {
            return false;
        }
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            ImageIO.write(border, "png", new File(target, filename));
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump SGB border", e);
        }
    }

    private boolean writeSgbFramePng(String filename) {
        if (superGameBoy == null || !superGameBoy.isEnabled()) {
            return false;
        }
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            ImageIO.write(superGameBoy.colorizeFrame(ppu.getFrameBuffer()), "png", new File(target, filename));
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump SGB frame", e);
        }
    }

    private boolean writeSgbAttributesPng(String filename) {
        if (superGameBoy == null || !superGameBoy.isEnabled()) {
            return false;
        }
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            ImageIO.write(superGameBoy.debugAttributeImage(), "png", new File(target, filename));
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump SGB attribute map", e);
        }
    }

    public File dumpMemoryBanks() {
        synchronized (stateLock) {
            String directoryName = "debug-memory-banks-" + DEBUG_DUMP_TIMESTAMP.format(Instant.now());
            Path directory = Path.of("target", directoryName);
            try {
                Files.createDirectories(directory);
                String indexJson = writeMemoryBankDumps(directory);
                Files.writeString(directory.resolve("index.json"), indexJson);
                return directory.toFile();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to dump memory banks", e);
            }
        }
    }

    private String writeMemoryBankDumps(Path directory) throws IOException {
        java.util.List<MemoryBank> banks = bus.memoryBanks();
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"metadata\": {\n");
        DebugJson.appendString(builder, "generatedAt", Instant.now().toString(), true, 4);
        DebugJson.appendString(builder, "rom", romFile == null ? "" : romFile.getAbsolutePath(), true, 4);
        DebugJson.appendString(builder, "title", cart.getHeader().getTitle(), true, 4);
        DebugJson.appendLong(builder, "frame", frameNumber, false, 4);
        builder.append("  },\n");
        builder.append("  \"banks\": [\n");
        for (int bankIndex = 0; bankIndex < banks.size(); bankIndex++) {
            MemoryBank bank = banks.get(bankIndex);
            builder.append("    {\n");
            DebugJson.appendString(builder, "name", bank.bankName(), true, 6);
            DebugJson.appendNumber(builder, "bankCount", bank.bankCount(), true, 6);
            DebugJson.appendNumber(builder, "bankSize", bank.bankSize(), true, 6);
            DebugJson.appendNumber(builder, "currentBank", bank.currentBank(), true, 6);
            builder.append("      \"files\": [\n");
            for (int slot = 0; slot < bank.bankCount(); slot++) {
                String filename = memoryBankDumpFilename(bankIndex, bank.bankName(), slot);
                Files.write(directory.resolve(filename), memoryBankBytes(bank, slot));
                builder.append("        {\n");
                DebugJson.appendNumber(builder, "bank", slot, true, 10);
                DebugJson.appendString(builder, "file", filename, false, 10);
                builder.append("        }");
                if (slot < bank.bankCount() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            builder.append("      ]\n");
            builder.append("    }");
            if (bankIndex < banks.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private byte[] memoryBankBytes(MemoryBank bank, int bankNumber) {
        byte[] bytes = new byte[Math.max(0, bank.bankSize())];
        for (int offset = 0; offset < bytes.length; offset++) {
            bytes[offset] = bank.readBank(bankNumber, offset);
        }
        return bytes;
    }

    private String memoryBankDumpFilename(int bankIndex, String bankName, int bankNumber) {
        return String.format(
                "%02d-%s-%03d.bin",
                bankIndex,
                bankName.replaceAll("[^A-Za-z0-9._-]", "_"),
                bankNumber
        );
    }

    private String debugBundleJson(String frameFilename, String sgbBorderFilename, String sgbFrameFilename, String sgbAttributesFilename) {
        CpuState cpuState = cpu.saveState();
        Ppu.DebugSnapshot ppuSnapshot = ppu.debugSnapshot();
        Ppu.FrameDebugStats frameStats = ppu.frameDebugStats();
        InterruptState interruptState = interruptManager().saveState();
        TimerState timerState = timer.saveState();
        SerialState serialState = serial.saveState();
        CartState cartState = cart.saveState();
        DmaState dmaState = dma.saveState();
        HdmaState hdmaState = hdma.saveState();
        Key0State key0State = key0.saveState();
        Key1State key1State = key1.saveState();
        CgbUndocumentedRegistersState cgbUndocumentedState = cgbUndocumentedRegisters.saveState();
        CgbCompatibilityPaletteSelection compatibilityPaletteSelection = CgbCompatibilityPaletteSelector.select(cart.getHeader());
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"metadata\": {\n");
        DebugJson.appendString(builder, "generatedAt", Instant.now().toString(), true, 4);
        DebugJson.appendString(builder, "rom", romFile == null ? "" : romFile.getAbsolutePath(), true, 4);
        DebugJson.appendString(builder, "saveFile", saveFile == null ? "" : saveFile.getAbsolutePath(), true, 4);
        DebugJson.appendString(builder, "title", cart.getHeader().getTitle(), true, 4);
        DebugJson.appendLong(builder, "frame", frameNumber, true, 4);
        DebugJson.appendLong(builder, "systemCycles", systemCycles, true, 4);
        DebugJson.appendBoolean(builder, "biosLoaded", bios != null, true, 4);
        DebugJson.appendBoolean(builder, "cartridgeCgbCompatible", cartridgeCgbCompatible, true, 4);
        DebugJson.appendBoolean(builder, "cartridgeSgbEnhanced", cart.getHeader().isSgbEnhanced(), true, 4);
        DebugJson.appendBoolean(builder, "throttled", throttled, true, 4);
        DebugJson.appendBoolean(builder, "paused", paused, true, 4);
        DebugJson.appendBoolean(builder, "stopped", stopped, true, 4);
        DebugJson.appendString(builder, "framePng", frameFilename, true, 4);
        DebugJson.appendString(builder, "sgbBorderPng", sgbBorderFilename, true, 4);
        DebugJson.appendString(builder, "sgbFramePng", sgbFrameFilename, true, 4);
        DebugJson.appendString(builder, "sgbAttributesPng", sgbAttributesFilename, false, 4);
        builder.append("  },\n");
        builder.append("  \"cpu\": {\n");
        DebugJson.appendHex(builder, "pc", cpuState.pc(), true, 4, 4);
        DebugJson.appendHex(builder, "sp", cpuState.sp(), true, 4, 4);
        DebugJson.appendHex(builder, "af", cpuState.af(), true, 4, 4);
        DebugJson.appendHex(builder, "bc", cpuState.bc(), true, 4, 4);
        DebugJson.appendHex(builder, "de", cpuState.de(), true, 4, 4);
        DebugJson.appendHex(builder, "hl", cpuState.hl(), true, 4, 4);
        DebugJson.appendHex(builder, "a", cpuState.aUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "b", cpuState.bUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "c", cpuState.cUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "d", cpuState.dUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "e", cpuState.eUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "h", cpuState.hUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "l", cpuState.lUnsigned(), true, 4, 2);
        DebugJson.appendBoolean(builder, "zeroFlag", cpuState.zeroFlag(), true, 4);
        DebugJson.appendBoolean(builder, "negativeFlag", cpuState.negativeFlag(), true, 4);
        DebugJson.appendBoolean(builder, "halfCarryFlag", cpuState.halfCarryFlag(), true, 4);
        DebugJson.appendBoolean(builder, "carryFlag", cpuState.carryFlag(), true, 4);
        DebugJson.appendBoolean(builder, "ime", cpuState.ime(), true, 4);
        DebugJson.appendBoolean(builder, "halted", cpuState.halted(), true, 4);
        DebugJson.appendBoolean(builder, "stopped", cpuState.stopped(), true, 4);
        DebugJson.appendNumber(builder, "speedRate", cpuState.speedRate(), false, 4);
        builder.append("  },\n");
        builder.append("  \"interrupts\": {\n");
        DebugJson.appendHex(builder, "ie", interruptState.ieReg() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "if", interruptState.ifReg() & 0xFF, true, 4, 2);
        DebugJson.appendString(builder, "pending", bus.getPendingInterrupt().map(Enum::name).orElse(""), false, 4);
        builder.append("  },\n");
        builder.append("  \"timer\": {\n");
        DebugJson.appendNumber(builder, "systemCounter", timerState.systemCounter(), true, 4);
        DebugJson.appendHex(builder, "div", (timerState.systemCounter() >> 8) & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "tima", timerState.timerCounter() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "tma", timerState.timerModulo() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "tac", timerState.timerControl() & 0xFF, true, 4, 2);
        DebugJson.appendNumber(builder, "overflowDelay", timerState.overflowDelay(), false, 4);
        builder.append("  },\n");
        builder.append("  \"serial\": {\n");
        DebugJson.appendHex(builder, "sb", serialState.sb(), true, 4, 2);
        DebugJson.appendHex(builder, "sc", serialState.sc(), true, 4, 2);
        DebugJson.appendBoolean(builder, "transferActive", serial.isTransferActive(), true, 4);
        DebugJson.appendBoolean(builder, "internalClock", serial.isInternalClockSelected(), true, 4);
        DebugJson.appendBoolean(builder, "fastClock", serial.isFastClockSelected(), true, 4);
        DebugJson.appendBoolean(builder, "masterWaitingResponse", serial.isMasterWaitingResponse(), true, 4);
        DebugJson.appendNumber(builder, "transferCyclesRemaining", serialState.transferCyclesRemaining(), true, 4);
        DebugJson.appendHex(builder, "outgoingByte", serialState.outgoingByte(), true, 4, 2);
        DebugJson.appendHex(builder, "lastCompletedOutgoingByte", serial.lastCompletedOutgoingByte(), true, 4, 2);
        DebugJson.appendHex(builder, "lastCompletedIncomingByte", serial.lastCompletedIncomingByte(), true, 4, 2);
        DebugJson.appendLong(builder, "completedTransfers", serial.completedTransfers(), true, 4);
        DebugJson.appendString(builder, "pendingText", serialState.pendingText(), true, 4);
        DebugJson.appendString(builder, "transcript", serial.transcript(), true, 4);
        appendSerialTransferHistoryJson(builder, 4);
        builder.append("  },\n");
        appendLinkDebugJson(builder);
        appendSuperGameBoyDebugJson(builder);
        appendDmaDebugJson(builder, dmaState, hdmaState);
        appendCgbRegistersDebugJson(builder, key0State, key1State, cgbUndocumentedState, compatibilityPaletteSelection);
        builder.append("  \"ppu\": {\n");
        DebugJson.appendBoolean(builder, "cgbMode", ppuSnapshot.cgbMode(), true, 4);
        DebugJson.appendHex(builder, "lcdc", ppuSnapshot.lcdc(), true, 4, 2);
        DebugJson.appendHex(builder, "stat", ppuSnapshot.stat(), true, 4, 2);
        DebugJson.appendString(builder, "mode", String.valueOf(ppuSnapshot.mode()), true, 4);
        DebugJson.appendNumber(builder, "line", ppuSnapshot.line(), true, 4);
        DebugJson.appendNumber(builder, "column", ppuSnapshot.column(), true, 4);
        DebugJson.appendNumber(builder, "cycles", ppuSnapshot.cycles(), true, 4);
        DebugJson.appendNumber(builder, "scrollX", ppuSnapshot.scrollX(), true, 4);
        DebugJson.appendNumber(builder, "scrollY", ppuSnapshot.scrollY(), true, 4);
        DebugJson.appendNumber(builder, "windowX", ppuSnapshot.windowX(), true, 4);
        DebugJson.appendNumber(builder, "windowY", ppuSnapshot.windowY(), true, 4);
        DebugJson.appendHex(builder, "lineCompare", ppuSnapshot.lineCompare(), false, 4, 2);
        builder.append("  },\n");
        builder.append("  \"frameStats\": {\n");
        DebugJson.appendNumber(builder, "blackPixels", frameStats.blackPixels(), true, 4);
        DebugJson.appendNumber(builder, "whitePixels", frameStats.whitePixels(), true, 4);
        DebugJson.appendNumber(builder, "otherPixels", frameStats.otherPixels(), true, 4);
        DebugJson.appendNumber(builder, "nonEmptyTiles", frameStats.nonEmptyTiles(), false, 4);
        builder.append("  },\n");
        appendDisassemblyDebugJson(builder, cpuState.pc());
        appendCartDebugJson(builder, cartState);
        appendMemoryMapDebugJson(builder);
        appendMemoryBanksDebugJson(builder);
        appendWatchpointsDebugJson(builder);
        builder.append("}\n");
        return builder.toString();
    }

    private void appendLinkDebugJson(StringBuilder builder) {
        var local = linkCable.localState();
        builder.append("  \"link\": {\n");
        DebugJson.appendBoolean(builder, "active", linkCable.isActive(), true, 4);
        DebugJson.appendBoolean(builder, "connected", linkCable.isConnected(), true, 4);
        DebugJson.appendBoolean(builder, "hosting", linkCable.isHosting(), true, 4);
        DebugJson.appendString(builder, "status", linkCable.isActive() ? "Connected" : "Disconnected", true, 4);
        DebugJson.appendString(builder, "mode", linkCable.isActive() ? "direct" : "none", true, 4);
        DebugJson.appendString(builder, "role", linkCable.isHosting() ? "player1" : "player2", true, 4);
        DebugJson.appendBoolean(builder, "localTransferActive", local.transferActive(), true, 4);
        DebugJson.appendBoolean(builder, "localInternalClock", local.internalClock(), true, 4);
        DebugJson.appendBoolean(builder, "localMasterWaitingResponse", local.masterWaitingResponse(), true, 4);
        DebugJson.appendHex(builder, "localSc", local.sc(), true, 4, 2);
        DebugJson.appendHex(builder, "localOutgoingByte", local.outgoingByte(), true, 4, 2);
        DebugJson.appendBoolean(builder, "effectiveMaster", linkCable.isEffectiveMaster(), true, 4);
        DebugJson.appendBoolean(builder, "dualMasterCollision", linkCable.hasDualMasterCollision(), true, 4);
        DebugJson.appendLong(builder, "clockPulsesSent", linkCable.clockPulsesSent(), true, 4);
        DebugJson.appendLong(builder, "clockPulsesReceived", linkCable.clockPulsesReceived(), true, 4);
        DebugJson.appendLong(builder, "clockResponsesSent", linkCable.clockResponsesSent(), true, 4);
        DebugJson.appendLong(builder, "clockResponsesReceived", linkCable.clockResponsesReceived(), true, 4);
        DebugJson.appendLong(builder, "clockPulsesDiscarded", linkCable.clockPulsesDiscarded(), true, 4);
        DebugJson.appendNumber(builder, "pendingIncomingClockCount", linkCable.pendingIncomingClockCount(), true, 4);
        DebugJson.appendBoolean(builder, "pendingInternalClockByte", linkCable.hasPendingInternalClockByte(), true, 4);
        DebugJson.appendBoolean(builder, "inFlightTransfer", linkCable.hasInFlightTransfer(), true, 4);
        DebugJson.appendHex(builder, "lastClockPulseSent", linkCable.lastClockPulseSent(), true, 4, 2);
        DebugJson.appendHex(builder, "lastClockPulseReceived", linkCable.lastClockPulseReceived(), true, 4, 2);
        DebugJson.appendHex(builder, "lastClockResponseSent", linkCable.lastClockResponseSent(), true, 4, 2);
        DebugJson.appendHex(builder, "lastClockResponseReceived", linkCable.lastClockResponseReceived(), true, 4, 2);
        var peer = linkCable.peerState();
        DebugJson.appendBoolean(builder, "peerTransferActive", peer.transferActive(), true, 4);
        DebugJson.appendBoolean(builder, "peerInternalClock", peer.internalClock(), true, 4);
        DebugJson.appendHex(builder, "peerSc", peer.sc(), true, 4, 2);
        DebugJson.appendHex(builder, "peerOutgoingByte", peer.outgoingByte(), true, 4, 2);
        DebugJson.appendBoolean(builder, "bothReadyForTransfer", linkCable.bothSidesReadyForTransfer(), false, 4);
        builder.append("  },\n");
    }

    private void appendSuperGameBoyDebugJson(StringBuilder builder) {
        builder.append("  \"superGameBoy\": {\n");
        DebugJson.appendBoolean(builder, "enabled", superGameBoy.isEnabled(), true, 4);
        DebugJson.appendBoolean(builder, "borderReady", superGameBoy.hasBorder(), true, 4);
        DebugJson.appendBoolean(builder, "transferMaskActive", superGameBoy.isTransferMaskActive(), true, 4);
        DebugJson.appendBoolean(builder, "systemPalettesReady", superGameBoy.systemPalettesReady(), true, 4);
        DebugJson.appendString(builder, "lastCommand", superGameBoy.lastCommandName(), true, 4);
        DebugJson.appendString(builder, "recentCommands", superGameBoy.recentCommandHistory(), true, 4);
        DebugJson.appendHex(builder, "lastHeaderByte", superGameBoy.lastHeaderByte(), true, 4, 2);
        DebugJson.appendString(builder, "lastPacketHex", superGameBoy.lastPacketHex(), true, 4);
        DebugJson.appendNumber(builder, "maskMode", superGameBoy.maskMode(), true, 4);
        DebugJson.appendBoolean(builder, "receivingPacket", superGameBoy.isReceivingPacket(), true, 4);
        DebugJson.appendNumber(builder, "expectedPackets", superGameBoy.expectedPackets(), true, 4);
        DebugJson.appendNumber(builder, "receivedPackets", superGameBoy.receivedPackets(), true, 4);
        DebugJson.appendString(builder, "pendingTransfer", superGameBoy.pendingTransferName(), true, 4);
        DebugJson.appendNumber(builder, "pendingTransferFrames", superGameBoy.pendingTransferFrames(), true, 4);
        DebugJson.appendNumber(builder, "pendingTransferCount", superGameBoy.pendingTransferCount(), true, 4);
        DebugJson.appendNumber(builder, "borderTileNonZeroBytes", superGameBoy.borderTileNonZeroBytes(), true, 4);
        DebugJson.appendNumber(builder, "pictureTransferNonZeroBytes", superGameBoy.pictureTransferNonZeroBytes(), true, 4);
        DebugJson.appendNumber(builder, "pictureTransferUniqueMapEntries", superGameBoy.pictureTransferUniqueMapEntries(), true, 4);
        DebugJson.appendString(builder, "screenPalettes", superGameBoy.screenPalettesSample(), true, 4);
        DebugJson.appendNumber(builder, "screenAttributeUniqueCount", superGameBoy.screenAttributeUniqueCount(), true, 4);
        DebugJson.appendString(builder, "borderTileDataSample", superGameBoy.borderTileDataSample(), true, 4);
        DebugJson.appendString(builder, "pictureTransferMapSample", superGameBoy.pictureTransferMapSample(), true, 4);
        DebugJson.appendString(builder, "pictureTransferPaletteSample", superGameBoy.pictureTransferPaletteSample(), true, 4);
        DebugJson.appendLong(builder, "packetsReceivedTotal", superGameBoy.packetsReceived(), true, 4);
        DebugJson.appendLong(builder, "invalidPackets", superGameBoy.invalidPackets(), true, 4);
        DebugJson.appendLong(builder, "ignoredPackets", superGameBoy.ignoredPackets(), true, 4);
        DebugJson.appendNumber(builder, "pulseCount", superGameBoy.pulseCount(), true, 4);
        DebugJson.appendNumber(builder, "joypadCount", superGameBoy.joypadCount(), true, 4);
        DebugJson.appendNumber(builder, "selectedJoypad", superGameBoy.selectedJoypad(), false, 4);
        builder.append("  },\n");
    }

    private void appendSerialTransferHistoryJson(StringBuilder builder, int indent) {
        DebugJson.appendIndent(builder, indent);
        builder.append("\"recentTransfers\": [\n");
        int count = serial.transferHistoryCount();
        for (int index = 0; index < count; index++) {
            DebugJson.appendIndent(builder, indent + 2);
            builder.append("{\n");
            DebugJson.appendHex(builder, "out", serial.transferHistoryOutgoing(index), true, indent + 4, 2);
            DebugJson.appendHex(builder, "in", serial.transferHistoryIncoming(index), true, indent + 4, 2);
            DebugJson.appendBoolean(builder, "internalClock", serial.transferHistoryInternalClock(index), true, indent + 4);
            DebugJson.appendBoolean(builder, "completedAsMaster", serial.transferHistoryCompletedAsMaster(index), false, indent + 4);
            DebugJson.appendIndent(builder, indent + 2);
            builder.append('}');
            if (index < count - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        DebugJson.appendIndent(builder, indent);
        builder.append("]\n");
    }

    private void appendDmaDebugJson(StringBuilder builder, DmaState dmaState, HdmaState hdmaState) {
        builder.append("  \"dma\": {\n");
        DebugJson.appendBoolean(builder, "active", dmaState.active(), true, 4);
        DebugJson.appendHex(builder, "baseAddress", dmaState.baseAddress(), true, 4, 4);
        DebugJson.appendNumber(builder, "cycles", dmaState.cycles(), false, 4);
        builder.append("  },\n");
        builder.append("  \"hdma\": {\n");
        DebugJson.appendBoolean(builder, "active", hdmaState.active(), true, 4);
        DebugJson.appendNumber(builder, "total", hdmaState.total(), true, 4);
        DebugJson.appendHex(builder, "sourceAddress", hdmaState.sourceAddress(), true, 4, 4);
        DebugJson.appendHex(builder, "destinationAddress", hdmaState.destinationAddress(), true, 4, 4);
        DebugJson.appendNumber(builder, "mode", hdmaState.mode(), true, 4);
        DebugJson.appendNumber(builder, "cycles", hdmaState.cycles(), true, 4);
        DebugJson.appendNumber(builder, "counter", hdmaState.counter(), true, 4);
        DebugJson.appendNumber(builder, "remainingBlocksMinusOne", hdma.remainingBlocksMinusOne(), true, 4);
        DebugJson.appendBoolean(builder, "hblankBlockTransferred", hdma.hblankBlockTransferred(), true, 4);
        DebugJson.appendBoolean(builder, "completed", hdmaState.completed(), false, 4);
        builder.append("  },\n");
    }

    private void appendCgbRegistersDebugJson(
            StringBuilder builder,
            Key0State key0State,
            Key1State key1State,
            CgbUndocumentedRegistersState cgbUndocumentedState,
            CgbCompatibilityPaletteSelection compatibilityPaletteSelection
    ) {
        builder.append("  \"cgbRegisters\": {\n");
        DebugJson.appendHex(builder, "key0", key0State.key0() & 0xFF, true, 4, 2);
        DebugJson.appendBoolean(builder, "key0Locked", key0State.locked(), true, 4);
        DebugJson.appendHex(builder, "key1", key1.read(0xFF4D) & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "vbk", ppu.read(0xFF4F) & 0xFF, true, 4, 2);
        DebugJson.appendBoolean(builder, "prepareSpeedSwitch", key1State.prepareSpeedSwitch(), true, 4);
        DebugJson.appendBoolean(builder, "doubleSpeed", key1State.doubleSpeed(), true, 4);
        DebugJson.appendHex(builder, "svbk", workRam.read(0xFF70) & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "infrared", infraredPort.read(0xFF56) & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "ff72", cgbUndocumentedState.ff72() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "ff73", cgbUndocumentedState.ff73() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "ff74", cgbUndocumentedState.ff74() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "ff75", (0x8F | (cgbUndocumentedState.ff75() & 0x70)), true, 4, 2);
        DebugJson.appendBoolean(builder, "compatibilityNintendoLicensed", cart.getHeader().isNintendoLicensedForCgbCompatibilityPalettes(), true, 4);
        DebugJson.appendHex(builder, "compatibilityOldLicensee", cart.getHeader().getOldLicenseeCode(), true, 4, 2);
        DebugJson.appendString(builder, "compatibilityNewLicensee", cart.getHeader().getNewLicenseeCode(), true, 4);
        DebugJson.appendHex(builder, "compatibilityTitleChecksum", cart.getHeader().getTitleChecksum(), true, 4, 2);
        DebugJson.appendHex(builder, "compatibilityTitleFourthByte", cart.getHeader().getTitleByte(3), true, 4, 2);
        DebugJson.appendNumber(builder, "compatibilityPaletteId", compatibilityPaletteSelection.paletteId(), true, 4);
        DebugJson.appendNumber(builder, "compatibilityPaletteGroup", compatibilityPaletteSelection.paletteGroup(), true, 4);
        DebugJson.appendNumber(builder, "compatibilityObj0PaletteWordOffset", compatibilityPaletteSelection.obj0PaletteWordOffset(), true, 4);
        DebugJson.appendNumber(builder, "compatibilityObj1PaletteWordOffset", compatibilityPaletteSelection.obj1PaletteWordOffset(), true, 4);
        DebugJson.appendNumber(builder, "compatibilityBgPaletteWordOffset", compatibilityPaletteSelection.bgPaletteWordOffset(), true, 4);
        DebugJson.appendBoolean(builder, "compatibilityLogoTilemapRequired", compatibilityPaletteSelection.logoTilemapRequired(), false, 4);
        builder.append("  },\n");
    }

    private void appendDisassemblyDebugJson(StringBuilder builder, int pc) {
        builder.append("  \"disassembly\": [\n");
        int cursor = pc & 0xFFFF;
        for (int index = 0; index < 32; index++) {
            Disassembler.Decoded decoded = Disassembler.decode(cursor, address -> bus.read(address) & 0xFF);
            builder.append("    {\n");
            DebugJson.appendHex(builder, "address", decoded.address(), true, 6, 4);
            DebugJson.appendBoolean(builder, "currentPc", index == 0, true, 6);
            DebugJson.appendString(builder, "bytes", decoded.bytes(), true, 6);
            DebugJson.appendString(builder, "instruction", decoded.instruction(), false, 6);
            builder.append("    }");
            if (index < 31) {
                builder.append(',');
            }
            builder.append('\n');
            cursor = (cursor + decoded.length()) & 0xFFFF;
        }
        builder.append("  ],\n");
    }

    private void appendCartDebugJson(StringBuilder builder, CartState cartState) {
        builder.append("  \"cart\": {\n");
        DebugJson.appendObject(builder, "properties", cart.debugProperties(), true, 4);
        builder.append("    \"externalRam\": {\n");
        DebugJson.appendNumber(builder, "size", cartState.externalRam().data().length, true, 6);
        DebugJson.appendNumber(builder, "currentBank", cartState.externalRam().currentBank(), false, 6);
        builder.append("    },\n");
        DebugJson.appendObject(builder, "mapperState", cartState.mapperState(), false, 4);
        builder.append("  },\n");
    }

    private void appendMemoryMapDebugJson(StringBuilder builder) {
        builder.append("  \"memoryMap\": [\n");
        java.util.List<Bus.MemoryMapEntry> entries = bus.memoryMap();
        for (int index = 0; index < entries.size(); index++) {
            Bus.MemoryMapEntry entry = entries.get(index);
            builder.append("    {\n");
            DebugJson.appendHex(builder, "start", entry.start(), true, 6, 4);
            DebugJson.appendHex(builder, "end", entry.end(), true, 6, 4);
            DebugJson.appendString(builder, "owner", entry.owner(), false, 6);
            builder.append("    }");
            if (index < entries.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
    }

    private void appendMemoryBanksDebugJson(StringBuilder builder) {
        builder.append("  \"memoryBanks\": [\n");
        java.util.List<MemoryBank> banks = bus.memoryBanks();
        for (int index = 0; index < banks.size(); index++) {
            MemoryBank bank = banks.get(index);
            builder.append("    {\n");
            DebugJson.appendString(builder, "name", bank.bankName(), true, 6);
            DebugJson.appendNumber(builder, "bankCount", bank.bankCount(), true, 6);
            DebugJson.appendNumber(builder, "bankSize", bank.bankSize(), true, 6);
            DebugJson.appendNumber(builder, "currentBank", bank.currentBank(), true, 6);
            DebugJson.appendString(builder, "currentBankSample", DebugJson.memoryBankSample(bank, 64), false, 6);
            builder.append("    }");
            if (index < banks.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
    }

    private void appendWatchpointsDebugJson(StringBuilder builder) {
        builder.append("  \"watchpoints\": [\n");
        java.util.List<DebugController.Watchpoint> watchpoints = debugController.watchpoints();
        for (int index = 0; index < watchpoints.size(); index++) {
            DebugController.Watchpoint watchpoint = watchpoints.get(index);
            builder.append("    {\n");
            DebugJson.appendString(builder, "type", watchpoint.accessType().name(), true, 6);
            DebugJson.appendHex(builder, "address", watchpoint.address(), true, 6, 4);
            if (watchpoint.value() == null) {
                DebugJson.appendString(builder, "value", "", false, 6);
            } else {
                DebugJson.appendHex(builder, "value", watchpoint.value(), false, 6, 2);
            }
            builder.append("    }");
            if (index < watchpoints.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
    }

    private void tickSystemCycle() {
        systemCycles++;
        if (hdma.isActive()) {
            if (hdma.isHBlankMode() && ppu.canRunHBlankDma()) {
                hdma.tick();
            } else if (hdma.isHBlankMode()) {
                hdma.leaveHBlank();
            }
            if (hdma.isGeneralPurposeMode()) {
                hdma.tick();
            }
        }
        if (dma.isActive()) {
            dma.tick();
        }

        linkCable.tick();
        timer.tick();
        serial.tick();
        ppu.tick();
        updateFastForwardAudioMode();
        apu.tick();
        if (ppu.consumeFrameReady()) {
            boolean transferFrame = superGameBoy.consumeTransferFrame();
            if (shouldRenderFrame() && !transferFrame && !superGameBoy.shouldSuppressFrame()) {
                if (window != null) {
                    window.renderFrame(ppu);
                }
                if (detachedDisplayWindow != null) {
                    if (detachedDisplayWindow.isOpen()) {
                        detachedDisplayWindow.renderFrame(ppu);
                    } else {
                        detachedDisplayWindow = null;
                    }
                }
            }
        }
        dots++;
        if (dots >= DOTS_PER_FRAME) {
            dots = 0;
            superGameBoy.tickFrame();
            frameNumber++;
            recordRewindSnapshot();
            updateRumbleOutput(cart.isRumbleActive());
            updatePerformanceStats();
            if (stopAfterFrames > 0 && frameNumber >= stopAfterFrames) {
                stop();
                return;
            }
            if (throttled && !externallyThrottled) {
                throttleFrame();
            }
        }
    }

    private boolean hdmaBlocksCpu() {
        if (!hdma.isActive()) {
            return false;
        }
        if (hdma.isGeneralPurposeMode()) {
            return true;
        }
        return hdma.isHBlankMode() && ppu.canRunHBlankDma() && !hdma.hblankBlockTransferred();
    }

    private void updatePerformanceStats() {
        performanceStatsFrames++;
        long now = System.nanoTime();
        long elapsed = now - performanceStatsStart;
        if (elapsed < 1_000_000_000L) {
            return;
        }
        double fps = performanceStatsFrames * 1_000_000_000.0 / elapsed;
        double speedPercent = fps * 100.0 / TARGET_FPS;
        performanceStatsFrames = 0;
        performanceStatsStart = now;
        if (window != null) {
            window.updatePerformanceStats(fps, speedPercent);
        }
    }

    private void throttleFrame() {
        long targetFrameNanos = targetFrameNanos();
        long now = System.nanoTime();
        if (now < nextFrameDeadline) {
            sleepUntil(nextFrameDeadline);
            now = System.nanoTime();
        }
        frameStart = now;
        nextFrameDeadline += targetFrameNanos;
        if (now - nextFrameDeadline > targetFrameNanos * 3) {
            nextFrameDeadline = now + targetFrameNanos;
        }
    }

    private boolean isFastForwardActive() {
        return !isLinkConnectionActive() && keyboardController != null && keyboardController.isTurboPressed();
    }

    private boolean shouldRenderFrame() {
        if (!isFastForwardActive()) {
            return true;
        }
        int renderInterval = Math.max(1, settings.turboMultiplier());
        return frameNumber % renderInterval == 0;
    }

    private void updateFastForwardAudioMode() {
        boolean active = isFastForwardActive();
        if (fastForwardAudioMuted == active) {
            return;
        }
        fastForwardAudioMuted = active;
        audioOutput.setSinkMuted(active);
    }

    private void sleepUntil(long deadline) {
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            if (remaining > 2_000_000L) {
                sleepNanos(remaining - 1_000_000L);
            } else if (remaining > 200_000L) {
                Thread.yield();
            }
        }
    }

    private void recordRewindSnapshot() {
        if (isLinkConnectionActive()) {
            return;
        }
        if (settings.rewindCapacity() == 0) {
            return;
        }
        if (frameNumber % settings.rewindCaptureIntervalFrames() != 0) {
            return;
        }
        if (throttled && System.nanoTime() - frameStart >= NANOS_PER_FRAME) {
            return;
        }
        rewindBuffer.add(createRewindSaveStateFile());
    }

    private void updateRumbleOutput(boolean active) {
        if (rumbleOutputActive == active) {
            return;
        }
        rumbleOutputActive = active;
        if (controller instanceof RumbleSink rumbleSink) {
            rumbleSink.setRumble(active);
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
        applyCgbBootHardwareDefaults();
        if (cartridgeCgbCompatible) {
            key0.write(0xFF4C, cart.getHeader().getCgbFlag());
        } else {
            key0.write(0xFF4C, (byte) 0x04);
            ppu.write(0xFF6C, (byte) 0x01);
        }
        cpu.setPc(0x100);
        cpu.setSp(0xfffe);
        if (superGameBoy.isEnabled()) {
            key0.write(0xFF4C, (byte) 0x04);
            ppu.write(0xFF6C, (byte) 0x01);
            cpu.setAf(0x0100);
            cpu.setBc(0x0014);
            cpu.setDe(0x0000);
            cpu.setHl(0x0000);
            if (bios != null) {
                cpu.getBus().write(0xFF50, (byte) 0x01);
            } else {
                key0.lock();
            }
            return;
        }
        if (bios != null) {
            cpu.getBus().write(0xFF50, (byte) 0x01);
        } else {
            key0.lock();
        }
        if (cartridgeCgbCompatible) {
            cpu.setAf(0x1180);
            cpu.setBc(0x0000);
            cpu.setDe(0xFF56);
            cpu.setHl(0x000D);
            return;
        }
        CgbCompatibilityPaletteSelection compatibilityPaletteSelection = CgbCompatibilityPaletteSelector.select(cart.getHeader());
        ppu.applyCgbCompatibilityPalettes(compatibilityPaletteSelection);
        int b = cart.getHeader().isNintendoLicensedForCgbCompatibilityPalettes()
                ? cart.getHeader().getTitleChecksum()
                : 0x00;
        cpu.setAf(0x1180);
        cpu.setB((byte) b);
        cpu.setC((byte) 0x00);
        cpu.setDe(0x0008);
        cpu.setHl(b == 0x43 || b == 0x58 ? 0x991A : 0x007C);

    }

    private void applyCgbBootHardwareDefaults() {
        bus.write(0xFF0F, (byte) 0xE1);
        bus.write(0xFFFF, (byte) 0x00);
        bus.write(0xFF02, (byte) 0x03);
        ppu.write(0xFF42, (byte) 0x00);
        ppu.write(0xFF43, (byte) 0x00);
        ppu.write(0xFF45, (byte) 0x00);
        ppu.write(0xFF47, (byte) 0xFC);
        ppu.write(0xFF4A, (byte) 0x00);
        ppu.write(0xFF4B, (byte) 0x00);
        ppu.write(0xFF40, (byte) 0x91);
    }

    public SaveStateFile createSaveStateFile() {
        ensureLinkDisconnected("Save state");
        synchronized (stateLock) {
            return new SaveStateFile(
                    SaveStateFile.CURRENT_FORMAT_VERSION,
                    createSaveStateMetadata(true),
                    createEmulatorState()
            );
        }
    }

    private SaveStateFile createRewindSaveStateFile() {
        synchronized (stateLock) {
            return new SaveStateFile(
                    SaveStateFile.CURRENT_FORMAT_VERSION,
                    createSaveStateMetadata(false),
                    createEmulatorState()
            );
        }
    }

    private SaveStateMetadata createSaveStateMetadata(boolean includePreview) {
        return new SaveStateMetadata(
                Instant.now(),
                cart.getHeader().getTitle(),
                romFile == null ? "" : romFile.getAbsolutePath(),
                cart.getHeader().getCartridgeType().name(),
                frameNumber,
                cpu.getPc(),
                includePreview ? ppu.copyFrameBufferArgb() : new int[0],
                includePreview ? 160 : 0,
                includePreview ? 144 : 0
        );
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
                    infraredPort.saveState(),
                    cgbUndocumentedRegisters.saveState(),
                    superGameBoy.saveState()
            );
        }
    }

    public void restoreSaveStateFile(SaveStateFile saveStateFile) {
        ensureLinkDisconnected("Load state");
        restoreEmulatorState(saveStateFile.state());
    }

    public boolean rewindOneSnapshot() {
        if (isLinkConnectionActive()) {
            return false;
        }
        var snapshot = rewindBuffer.popLatest();
        snapshot.ifPresent(this::restoreSaveStateFile);
        return snapshot.isPresent();
    }

    public boolean isLinkConnectionActive() {
        return linkCable != null && linkCable.isActive();
    }

    public void restoreEmulatorState(EmulatorState emulatorState) {
        ensureLinkDisconnected("Load state");
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
            cgbUndocumentedRegisters.loadState(emulatorState.cgbUndocumentedRegisters());
            superGameBoy.loadState(emulatorState.superGameBoy());
        }
        resume();
    }

    private void ensureLinkDisconnected(String operation) {
        if (isLinkConnectionActive()) {
            throw new IllegalStateException(operation + " is disabled while a link connection is active.");
        }
    }

    private InterruptManager interruptManager() {
        return bus.findMemorySpace(InterruptManager.class).orElseThrow();
    }

    private enum DebugStepMode {
        NONE,
        FRAME,
        SCANLINE,
        HBLANK,
        VBLANK
    }

}

