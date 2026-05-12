package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.cartridge.CartFactory;
import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.controller.IdleController;
import dev.vitorsilverio.gbcemu.controller.KeyboardController;
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
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import dev.vitorsilverio.gbcemu.peripherals.*;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.ppu.PpuMode;
import dev.vitorsilverio.gbcemu.snapshot.EmulatorState;
import dev.vitorsilverio.gbcemu.snapshot.RewindBuffer;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateFile;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateMetadata;
import dev.vitorsilverio.gbcemu.util.DebugJson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Emulator {

    private static final int DOTS_PER_FRAME = 70224;
    private static final long NANOS_PER_FRAME = 16_742_706L;
    private static final double TARGET_FPS = 1_000_000_000.0 / NANOS_PER_FRAME;
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
    private final KeyboardController keyboardController;
    private final GameSharkDevice gameSharkDevice;
    private final Cart cart;
    private final File romFile;
    private final File saveFile;
    private final boolean throttled;
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
    private long frameStart = System.nanoTime();
    private long nextFrameDeadline = frameStart + NANOS_PER_FRAME;
    private long performanceStatsStart = System.nanoTime();
    private int performanceStatsFrames;
    private DebugStepMode debugStepMode = DebugStepMode.NONE;
    private long debugStepTargetFrame;
    private int debugStepStartLine;
    private long stopAfterFrames = -1;
    private boolean fastForwardAudioMuted;
    private static final DateTimeFormatter DEBUG_DUMP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private final Multiplayer multiplayer;


    public Emulator(File biosFile, File romFile, File saveFile) {
        this(biosFile, romFile, saveFile, false, null);
    }

    public Emulator(File biosFile, File romFile, File saveFile, boolean headless) {
        this(biosFile, romFile, saveFile, headless, null);
    }

    public Emulator(File biosFile, File romFile, File saveFile, boolean headless, EmulatorWindow window) {
        this(biosFile, romFile, saveFile, headless, window, AppSettings.defaults());
    }

    public Emulator(File biosFile, File romFile, File saveFile, boolean headless, EmulatorWindow window, AppSettings settings) {
        this.settings = settings.normalized();
        this.rewindBuffer = new RewindBuffer(this.settings.rewindCapacity());
        this.bus = new Bus();
        this.debugController.setWatchpointsChangedListener(this::updateMemoryAccessListener);
        this.gameSharkDevice = new GameSharkDevice();
        this.bus.addMemorySpace(gameSharkDevice);
        if (biosFile != null) {
            bios = new Bios(biosFile);
            bus.addMemorySpace(bios);
        } else {
            bios = null;
        }
        this.romFile = romFile;
        this.saveFile = saveFile;
        this.cart = CartFactory.fromFile(romFile, saveFile);
        this.cartridgeCgbCompatible = cart.getHeader().isCgbCompatible();
        this.cpu = new Cpu(bus);
        this.timer = new Timer(bus);
        this.ppu = new Ppu(bus, biosFile != null || cartridgeCgbCompatible);
        this.apu = headless ? Apu.muted() : new Apu();
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
        keyboardController = headless ? null : new KeyboardController(this.settings);
        Controller controller = headless ? new IdleController() : keyboardController;
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
        this.multiplayer = new Multiplayer();
        this.serial = new Serial(bus, multiplayer);
        bus.addMemorySpace(serial);
        cpu.setCycleCallback(this::tickSystemCycle);
    }


    public void start() {
        while (!stopped) {
            boolean stepInstruction = debugController.consumeInstructionStep();
            startPendingDebugStep();
            if (paused && !stepInstruction) {
                sleepNanos(2_000_000);
                continue;
            }
            if (!stepInstruction && debugController.shouldBreakAtPc(cpu.getPc())) {
                paused = true;
                debugStepMode = DebugStepMode.NONE;
                continue;
            }
            if (!stepInstruction && debugController.shouldBreakOnMemoryAccess()) {
                paused = true;
                debugStepMode = DebugStepMode.NONE;
                continue;
            }
            synchronized (stateLock) {
                if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                    cpu.tick();
                } else {
                    tickSystemCycle();
                }
            }
            if (stepInstruction) {
                paused = true;
            }
            completeDebugStepIfNeeded();
        }
        if (window != null) {
            window.detach(ppu);
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

    public void stop() {
        stopped = true;
        paused = false;
        apu.close();
        if(multiplayer != null && multiplayer.isConnected()) {
            multiplayer.disconnect();
        }
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
        rewindBuffer = new RewindBuffer(this.settings.rewindCapacity());
        if (keyboardController != null) {
            keyboardController.applySettings(this.settings);
        }
    }

    public void openMemoryDebugger() {
        new MemoryDebugWindow(bus, this::isPaused);
    }

    public void openPpuDebugger() {
        new PpuDebugWindow(ppu);
    }

    public void openCpuDebugger() {
        new CpuDebugWindow(cpu, bus, ppu, debugController);
    }

    public void openCartDebugger() {
        new CartDebugWindow(cart);
    }

    private void updateMemoryAccessListener() {
        bus.setMemoryAccessListener(debugController.hasWatchpoints() ? debugController.memoryAccessListener() : null);
    }

    public File dumpDebugBundle() {
        synchronized (stateLock) {
            String baseName = "debug-bundle-" + DEBUG_DUMP_TIMESTAMP.format(Instant.now());
            String frameFilename = baseName + "-frame.png";
            writeDebugFramePng(frameFilename);
            return DebugJson.writeTargetFile(baseName + ".json", debugBundleJson(frameFilename), "Failed to dump debug bundle");
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

    private String debugBundleJson(String frameFilename) {
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
        InfraredState infraredState = infraredPort.saveState();
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"metadata\": {\n");
        DebugJson.appendString(builder, "generatedAt", Instant.now().toString(), true, 4);
        DebugJson.appendString(builder, "rom", romFile == null ? "" : romFile.getAbsolutePath(), true, 4);
        DebugJson.appendString(builder, "saveFile", saveFile == null ? "" : saveFile.getAbsolutePath(), true, 4);
        DebugJson.appendString(builder, "title", cart.getHeader().getTitle(), true, 4);
        DebugJson.appendLong(builder, "frame", frameNumber, true, 4);
        DebugJson.appendBoolean(builder, "biosLoaded", bios != null, true, 4);
        DebugJson.appendBoolean(builder, "cartridgeCgbCompatible", cartridgeCgbCompatible, true, 4);
        DebugJson.appendBoolean(builder, "throttled", throttled, true, 4);
        DebugJson.appendBoolean(builder, "paused", paused, true, 4);
        DebugJson.appendBoolean(builder, "stopped", stopped, true, 4);
        DebugJson.appendString(builder, "framePng", frameFilename, false, 4);
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
        DebugJson.appendBoolean(builder, "transferActive", serialState.transferCyclesRemaining() > 0, true, 4);
        DebugJson.appendNumber(builder, "transferCyclesRemaining", serialState.transferCyclesRemaining(), true, 4);
        DebugJson.appendHex(builder, "outgoingByte", serialState.outgoingByte(), true, 4, 2);
        DebugJson.appendString(builder, "pendingText", serialState.pendingText(), true, 4);
        DebugJson.appendString(builder, "transcript", serial.transcript(), false, 4);
        builder.append("  },\n");
        appendDmaDebugJson(builder, dmaState, hdmaState);
        appendCgbRegistersDebugJson(builder, key0State, key1State, infraredState);
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
        DebugJson.appendBoolean(builder, "completed", hdmaState.completed(), false, 4);
        builder.append("  },\n");
    }

    private void appendCgbRegistersDebugJson(
            StringBuilder builder,
            Key0State key0State,
            Key1State key1State,
            InfraredState infraredState
    ) {
        builder.append("  \"cgbRegisters\": {\n");
        DebugJson.appendHex(builder, "key0", key0State.key0() & 0xFF, true, 4, 2);
        DebugJson.appendBoolean(builder, "prepareSpeedSwitch", key1State.prepareSpeedSwitch(), true, 4);
        DebugJson.appendBoolean(builder, "doubleSpeed", key1State.doubleSpeed(), true, 4);
        DebugJson.appendHex(builder, "infrared", infraredState.data() & 0xFF, false, 4, 2);
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

        multiplayer.tick();
        timer.tick();
        serial.tick();
        ppu.tick();
        updateFastForwardAudioMode();
        apu.tick();
        if (window != null && ppu.consumeFrameReady()) {
            window.renderFrame(ppu);
        }
        dots++;
        if (dots >= DOTS_PER_FRAME) {
            dots = 0;
            frameNumber++;
            recordRewindSnapshot();
            updatePerformanceStats();
            if (stopAfterFrames > 0 && frameNumber >= stopAfterFrames) {
                stop();
                return;
            }
            if (throttled) {
                throttleFrame();
            }
        }
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

    private long targetFrameNanos() {
        if (!isFastForwardActive()) {
            return NANOS_PER_FRAME;
        }
        return Math.max(1L, NANOS_PER_FRAME / Math.max(1, settings.turboMultiplier()));
    }

    private boolean isFastForwardActive() {
        return keyboardController != null && keyboardController.isTurboPressed();
    }

    private void updateFastForwardAudioMode() {
        boolean active = isFastForwardActive();
        if (fastForwardAudioMuted == active) {
            return;
        }
        fastForwardAudioMuted = active;
        apu.setFastForwardAudioMuted(active);
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

    public void openMultiplayerDialog() {
        new MultiplayerDialog(multiplayer);
    }

    private enum DebugStepMode {
        NONE,
        FRAME,
        SCANLINE,
        HBLANK,
        VBLANK
    }

}
