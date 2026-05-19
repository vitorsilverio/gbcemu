package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.gui.EmulatorWindow;
import dev.vitorsilverio.gbcemu.link.DirectLinkCable;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.snapshot.EmulatorState;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateFile;
import dev.vitorsilverio.gbcemu.util.DebugJson;

import javax.swing.JOptionPane;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * Runtime session for one or more Game Boy consoles.
 *
 * <p>The hardware lives in {@link Console}. This class owns the execution loop, throttling, and
 * multi-console coordination. A regular game is an emulator session with one console; local link
 * multiplayer is the same session with two consoles connected by a direct cable.</p>
 */
public class Emulator {

    private static final long NANOS_PER_FRAME = 16_742_706L;
    private static final DateTimeFormatter DEBUG_DUMP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final List<Console> consoles;
    private final Console primaryConsole;
    private final boolean throttled;
    private volatile boolean stopped;

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
        this(new Console(
                biosFile,
                romFile,
                saveFile,
                headless,
                window,
                settings,
                null,
                null,
                null,
                false,
                true
        ), !headless);
    }

    private Emulator(Console console, boolean throttled) {
        this(List.of(console), throttled);
    }

    private Emulator(List<Console> consoles, boolean throttled) {
        if (consoles.isEmpty()) {
            throw new IllegalArgumentException("At least one console is required");
        }
        this.consoles = List.copyOf(consoles);
        this.primaryConsole = consoles.getFirst();
        this.throttled = throttled;
    }

    public static Emulator linked(PlayerConfig player1, PlayerConfig player2, AppSettings settings) {
        DirectLinkCable.Pair cablePair = DirectLinkCable.createPair();
        List<Console> consoles = new ArrayList<>();
        consoles.add(createLinkedConsole(player1, settings, cablePair.player1()));
        consoles.add(createLinkedConsole(player2, settings, cablePair.player2()));
        return new Emulator(consoles, true);
    }

    private static Console createLinkedConsole(PlayerConfig config, AppSettings settings, LinkCable linkCable) {
        Console console = new Console(
                config.biosFile(),
                config.romFile(),
                config.saveFile(),
                false,
                config.window(),
                settings,
                config.controller(),
                config.keyListener(),
                linkCable,
                config.secondaryDisplay(),
                true
        );
        if (config.skipBios()) {
            console.skipBios();
        }
        return console;
    }

    public void start() {
        consoles.forEach(Console::resetExternalThrottleClock);
        long[] consoleCycles = initialConsoleCycles();
        long observedFrame = primaryConsole.frameNumber();
        long nextFrameDeadline = System.nanoTime() + NANOS_PER_FRAME;
        while (!stopped && !anyConsoleStopped()) {
            if (allConsolesPaused()) {
                LockSupport.parkNanos(2_000_000L);
                nextFrameDeadline = System.nanoTime() + primaryConsole.targetFrameNanos();
                observedFrame = primaryConsole.frameNumber();
                continue;
            }

            int consoleIndex = nextConsoleIndex(consoleCycles);
            if (consoleIndex < 0) {
                LockSupport.parkNanos(1_000_000L);
                continue;
            }
            int advancedCycles = consoles.get(consoleIndex).tick();
            if (advancedCycles > 0) {
                consoleCycles[consoleIndex] += advancedCycles;
            }

            long frame = primaryConsole.frameNumber();
            if (frame != observedFrame) {
                observedFrame = frame;
                long targetFrameNanos = primaryConsole.targetFrameNanos();
                if (throttled) {
                    long now = System.nanoTime();
                    long remaining = nextFrameDeadline - now;
                    if (remaining > 0) {
                        LockSupport.parkNanos(remaining);
                    }
                    long frameClock = System.nanoTime();
                    consoles.forEach(console -> console.markFrameClock(frameClock));
                    nextFrameDeadline += targetFrameNanos;
                    if (System.nanoTime() > nextFrameDeadline + targetFrameNanos * 3) {
                        nextFrameDeadline = System.nanoTime() + targetFrameNanos;
                    }
                }
            }
        }
        stop();
    }

    private long[] initialConsoleCycles() {
        long[] cycles = new long[consoles.size()];
        for (int i = 0; i < consoles.size(); i++) {
            cycles[i] = consoles.get(i).systemCycles();
        }
        return cycles;
    }

    private int nextConsoleIndex(long[] consoleCycles) {
        int selected = -1;
        long lowestCycles = Long.MAX_VALUE;
        for (int i = 0; i < consoles.size(); i++) {
            Console console = consoles.get(i);
            if (console.isStopped() || console.isPaused()) {
                continue;
            }
            if (consoleCycles[i] < lowestCycles) {
                selected = i;
                lowestCycles = consoleCycles[i];
            }
        }
        return selected;
    }

    public void pause() {
        if (isLinkConnectionActive()) {
            return;
        }
        primaryConsole.pause();
    }

    private boolean anyConsoleStopped() {
        for (Console console : consoles) {
            if (console.isStopped()) {
                return true;
            }
        }
        return false;
    }

    private boolean allConsolesPaused() {
        for (Console console : consoles) {
            if (!console.isPaused()) {
                return false;
            }
        }
        return true;
    }

    public void pauseSession() {
        consoles.forEach(Console::pauseLinkedSession);
    }

    public void resume() {
        consoles.forEach(Console::resume);
    }

    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        consoles.forEach(Console::stop);
        consoles.forEach(Console::detachDisplay);
    }

    public boolean isPaused() {
        return primaryConsole.isPaused();
    }

    public void stopAfterFrames(long frames) {
        primaryConsole.stopAfterFrames(frames);
    }

    public void skipBios() {
        primaryConsole.skipBios();
    }

    public void openCheats() {
        Console console = chooseConsole("Cheats");
        if (console != null) {
            console.openCheats();
        }
    }

    public void openAudioDebugger() {
        Console console = chooseConsole("Audio debug");
        if (console != null) {
            console.openAudioDebugger();
        }
    }

    public String serialTranscript() {
        return primaryConsole.serialTranscript();
    }

    public void applySettings(AppSettings settings) {
        consoles.forEach(console -> console.applySettings(settings));
    }

    public void openMemoryDebugger() {
        Console console = chooseConsole("Memory debug");
        if (console != null) {
            console.openMemoryDebugger();
        }
    }

    public void openPpuDebugger() {
        Console console = chooseConsole("PPU debug");
        if (console != null) {
            console.openPpuDebugger();
        }
    }

    public void openCpuDebugger() {
        Console console = chooseConsole("CPU debug");
        if (console != null) {
            console.openCpuDebugger();
        }
    }

    public void openCartDebugger() {
        Console console = chooseConsole("Cart debug");
        if (console != null) {
            console.openCartDebugger();
        }
    }

    public File dumpDebugBundle() {
        if (consoles.size() == 1) {
            return primaryConsole.dumpDebugBundle();
        }
        try {
            List<File> dumps = new ArrayList<>();
            for (int i = 0; i < consoles.size(); i++) {
                dumps.add(consoles.get(i).dumpDebugBundle("console-" + (i + 1)));
            }
            String baseName = "debug-bundle-" + DEBUG_DUMP_TIMESTAMP.format(Instant.now()) + "-session.json";
            Path target = Path.of("target", baseName);
            Files.createDirectories(target.getParent());
            Files.writeString(target, sessionDebugIndexJson(dumps));
            return target.toFile();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump emulator session debug bundle", e);
        }
    }

    private String sessionDebugIndexJson(List<File> dumps) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        DebugJson.appendString(builder, "generatedAt", Instant.now().toString(), true, 2);
        DebugJson.appendString(builder, "kind", "emulator-session", true, 2);
        builder.append("  \"consoles\": [\n");
        for (int i = 0; i < dumps.size(); i++) {
            builder.append("    {\n");
            DebugJson.appendNumber(builder, "index", i + 1, true, 6);
            DebugJson.appendString(builder, "dump", dumps.get(i).getName(), false, 6);
            builder.append("    }");
            if (i < dumps.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    public File dumpMemoryBanks() {
        return primaryConsole.dumpMemoryBanks();
    }

    public SaveStateFile createSaveStateFile() {
        ensureSingleConsole("Save state");
        return primaryConsole.createSaveStateFile();
    }

    public void restoreSaveStateFile(SaveStateFile saveStateFile) {
        ensureSingleConsole("Load state");
        primaryConsole.restoreSaveStateFile(saveStateFile);
    }

    public boolean rewindOneSnapshot() {
        if (isLinkConnectionActive()) {
            return false;
        }
        return primaryConsole.rewindOneSnapshot();
    }

    public boolean isLinkConnectionActive() {
        return consoles.size() > 1 || primaryConsole.isLinkConnectionActive();
    }

    public EmulatorState createEmulatorState() {
        ensureSingleConsole("Create emulator state");
        return primaryConsole.createEmulatorState();
    }

    public void restoreEmulatorState(EmulatorState emulatorState) {
        ensureSingleConsole("Restore emulator state");
        primaryConsole.restoreEmulatorState(emulatorState);
    }

    public Console console(int index) {
        return consoles.get(index);
    }

    public int consoleCount() {
        return consoles.size();
    }

    private void ensureSingleConsole(String operation) {
        if (consoles.size() != 1) {
            throw new IllegalStateException(operation + " is disabled while multiple consoles are active.");
        }
    }

    private Console chooseConsole(String title) {
        if (consoles.size() == 1) {
            return primaryConsole;
        }
        String[] options = new String[consoles.size()];
        for (int i = 0; i < options.length; i++) {
            options[i] = "Console " + (i + 1);
        }
        Object selected = JOptionPane.showInputDialog(
                null,
                "Choose console:",
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        if (selected == null) {
            return null;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return consoles.get(i);
            }
        }
        return primaryConsole;
    }

    public record PlayerConfig(
            File biosFile,
            File romFile,
            File saveFile,
            EmulatorWindow window,
            Controller controller,
            KeyListener keyListener,
            boolean skipBios,
            boolean secondaryDisplay
    ) {
    }
}
