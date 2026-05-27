package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.debug.CpuDebugWindow;
import dev.vitorsilverio.gbcemu.gui.AudioDebugWindow;
import dev.vitorsilverio.gbcemu.gui.CartDebugWindow;
import dev.vitorsilverio.gbcemu.gui.EmulatorWindow;
import dev.vitorsilverio.gbcemu.gui.MemoryDebugWindow;
import dev.vitorsilverio.gbcemu.gui.PpuDebugWindow;
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
    private final Object consolesLock = new Object();
    private volatile Console primaryConsole;
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
                headless ? null : DirectLinkCable.createStandalone(),
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
        this.consoles = new ArrayList<>(consoles);
        this.primaryConsole = this.consoles.getFirst();
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
        synchronized (consolesLock) {
            consoles.forEach(Console::resetExternalThrottleClock);
        }
        List<Long> consoleCycles = initialConsoleCycles();
        Console frameConsole = primaryConsole;
        long observedFrame = frameConsole.frameNumber();
        long nextFrameDeadline = System.nanoTime() + NANOS_PER_FRAME;
        while (!stopped) {
            pruneStoppedConsoles(consoleCycles);
            frameConsole = primaryConsole;
            if (frameConsole == null) {
                break;
            }
            if (allConsolesPaused()) {
                LockSupport.parkNanos(2_000_000L);
                nextFrameDeadline = System.nanoTime() + frameConsole.targetFrameNanos();
                observedFrame = frameConsole.frameNumber();
                continue;
            }

            TickTarget tickTarget = nextTickTarget(consoleCycles);
            if (tickTarget == null) {
                LockSupport.parkNanos(1_000_000L);
                continue;
            }
            int advancedCycles = tickTarget.console().tick();
            if (advancedCycles > 0) {
                updateConsoleCycles(consoleCycles, tickTarget.console(), advancedCycles);
            }

            frameConsole = primaryConsole;
            if (frameConsole == null) {
                break;
            }
            long frame = frameConsole.frameNumber();
            if (frame != observedFrame) {
                observedFrame = frame;
                long targetFrameNanos = frameConsole.targetFrameNanos();
                if (throttled) {
                    long now = System.nanoTime();
                    long remaining = nextFrameDeadline - now;
                    if (remaining > 0) {
                        LockSupport.parkNanos(remaining);
                    }
                    long frameClock = System.nanoTime();
                    synchronized (consolesLock) {
                        consoles.forEach(console -> console.markFrameClock(frameClock));
                    }
                    nextFrameDeadline += targetFrameNanos;
                    if (System.nanoTime() > nextFrameDeadline + targetFrameNanos * 3) {
                        nextFrameDeadline = System.nanoTime() + targetFrameNanos;
                    }
                }
            }
        }
        stop();
    }

    private List<Long> initialConsoleCycles() {
        synchronized (consolesLock) {
            List<Long> cycles = new ArrayList<>();
            for (Console console : consoles) {
                cycles.add(console.systemCycles());
            }
            return cycles;
        }
    }

    private TickTarget nextTickTarget(List<Long> consoleCycles) {
        synchronized (consolesLock) {
            syncCycleList(consoleCycles);
            int selected = -1;
            long lowestCycles = Long.MAX_VALUE;
            for (int i = 0; i < consoles.size(); i++) {
                Console console = consoles.get(i);
                if (console.isStopped() || console.isPaused()) {
                    continue;
                }
                long cycles = consoleCycles.get(i);
                if (cycles < lowestCycles) {
                    selected = i;
                    lowestCycles = cycles;
                }
            }
            return selected < 0 ? null : new TickTarget(consoles.get(selected));
        }
    }

    private void updateConsoleCycles(List<Long> consoleCycles, Console console, int advancedCycles) {
        synchronized (consolesLock) {
            int index = consoles.indexOf(console);
            if (index >= 0) {
                syncCycleList(consoleCycles);
                consoleCycles.set(index, consoleCycles.get(index) + advancedCycles);
            }
        }
    }

    private void syncCycleList(List<Long> consoleCycles) {
        while (consoleCycles.size() < consoles.size()) {
            consoleCycles.add(consoles.get(consoleCycles.size()).systemCycles());
        }
        while (consoleCycles.size() > consoles.size()) {
            consoleCycles.removeLast();
        }
    }

    private void pruneStoppedConsoles(List<Long> consoleCycles) {
        synchronized (consolesLock) {
            for (int i = consoles.size() - 1; i >= 0; i--) {
                if (consoles.get(i).isStopped()) {
                    consoles.get(i).detachDisplay();
                    consoles.remove(i);
                    if (i < consoleCycles.size()) {
                        consoleCycles.remove(i);
                    }
                }
            }
            refreshPrimaryConsole();
        }
    }

    private void refreshPrimaryConsole() {
        primaryConsole = consoles.isEmpty() ? null : consoles.getFirst();
    }

    private record TickTarget(Console console) {
    }

    public void pause() {
        if (isLinkConnectionActive()) {
            return;
        }
        Console console = primaryConsole;
        if (console != null) {
            console.pause();
        }
    }

    private boolean allConsolesPaused() {
        synchronized (consolesLock) {
            for (Console console : consoles) {
                if (!console.isPaused()) {
                    return false;
                }
            }
            return true;
        }
    }

    public void pauseSession() {
        synchronized (consolesLock) {
            consoles.forEach(Console::pauseLinkedSession);
        }
    }

    public void resume() {
        synchronized (consolesLock) {
            consoles.forEach(Console::resume);
        }
    }

    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        synchronized (consolesLock) {
            consoles.forEach(Console::stop);
            consoles.forEach(Console::detachDisplay);
            consoles.clear();
            refreshPrimaryConsole();
        }
    }

    public boolean isPaused() {
        Console console = primaryConsole;
        return console != null && console.isPaused();
    }

    public void stopAfterFrames(long frames) {
        Console console = primaryConsole;
        if (console != null) {
            console.stopAfterFrames(frames);
        }
    }

    public void skipBios() {
        Console console = primaryConsole;
        if (console != null) {
            console.skipBios();
        }
    }

    public void openCheats() {
        Console console = chooseConsole("Cheats");
        if (console != null) {
            console.openCheats();
        }
    }

    public void openDetachedDisplay(int index) {
        Console console = consoleAt(index);
        console.openDetachedDisplay("GBC EMU - Console " + (index + 1));
    }

    public void addConsole(PlayerConfig config, AppSettings settings) {
        synchronized (consolesLock) {
            if (consoles.size() >= 2) {
                throw new IllegalStateException("Only two local consoles are supported for now.");
            }
            Console host = primaryConsole;
            if (host == null) {
                throw new IllegalStateException("Start a ROM before adding another console.");
            }
            if (!(host.debugLinkCable() instanceof DirectLinkCable hostCable)) {
                throw new IllegalStateException("The current console was not started with a detachable local link cable.");
            }
            DirectLinkCable peerCable = hostCable.createPeer();
            Console console = createLinkedConsole(config, settings, peerCable);
            console.resetExternalThrottleClock();
            console.markFrameClock(System.nanoTime());
            consoles.add(console);
            refreshPrimaryConsole();
        }
    }

    public File stopConsole(int index) {
        synchronized (consolesLock) {
            if (index < 0 || index >= consoles.size()) {
                throw new IllegalArgumentException("Console " + (index + 1) + " is not active.");
            }
            Console console = consoles.remove(index);
            File romFile = console.romFile();
            console.stop();
            console.detachDisplay();
            refreshPrimaryConsole();
            return romFile;
        }
    }

    public void openAudioDebugger() {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }
        if (snapshot.size() == 1) {
            snapshot.getFirst().openAudioDebugger();
            return;
        }
        List<AudioDebugWindow.Target> targets = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Console console = snapshot.get(i);
            targets.add(new AudioDebugWindow.Target("Console " + (i + 1), console.debugApu()));
        }
        new AudioDebugWindow(targets);
    }

    public String serialTranscript() {
        Console console = primaryConsole;
        return console == null ? "" : console.serialTranscript();
    }

    public void applySettings(AppSettings settings) {
        consoleSnapshot().forEach(console -> console.applySettings(settings));
    }

    public void openMemoryDebugger() {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }
        if (snapshot.size() == 1) {
            snapshot.getFirst().openMemoryDebugger();
            return;
        }
        List<MemoryDebugWindow.Target> targets = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Console console = snapshot.get(i);
            targets.add(new MemoryDebugWindow.Target("Console " + (i + 1), console.debugBus(), console.debugPausedSupplier()));
        }
        new MemoryDebugWindow(targets);
    }

    public void openPpuDebugger() {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }
        if (snapshot.size() == 1) {
            snapshot.getFirst().openPpuDebugger();
            return;
        }
        List<PpuDebugWindow.Target> targets = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Console console = snapshot.get(i);
            targets.add(new PpuDebugWindow.Target("Console " + (i + 1), console.debugPpu(), console.debugSuperGameBoy()));
        }
        new PpuDebugWindow(targets);
    }

    public void openCpuDebugger() {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }
        if (snapshot.size() == 1) {
            snapshot.getFirst().openCpuDebugger();
            return;
        }
        List<CpuDebugWindow.Target> targets = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Console console = snapshot.get(i);
            targets.add(new CpuDebugWindow.Target(
                    "Console " + (i + 1),
                    console.debugCpu(),
                    console.debugBus(),
                    console.debugPpu(),
                    console.debugController(),
                    console.debugLinkCable()
            ));
        }
        new CpuDebugWindow(targets);
    }

    public void openCartDebugger() {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }
        if (snapshot.size() == 1) {
            snapshot.getFirst().openCartDebugger();
            return;
        }
        List<CartDebugWindow.Target> targets = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            Console console = snapshot.get(i);
            targets.add(new CartDebugWindow.Target("Console " + (i + 1), console.debugCart()));
        }
        new CartDebugWindow(targets);
    }

    public File dumpDebugBundle() {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            throw new IllegalStateException("No active console to dump.");
        }
        if (snapshot.size() == 1) {
            return snapshot.getFirst().dumpDebugBundle();
        }
        try {
            List<File> dumps = new ArrayList<>();
            for (int i = 0; i < snapshot.size(); i++) {
                dumps.add(snapshot.get(i).dumpDebugBundle("console-" + (i + 1)));
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
        ensureSingleConsole("Dump memory banks");
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
        Console console = primaryConsole;
        return console != null && console.rewindOneSnapshot();
    }

    public boolean isLinkConnectionActive() {
        synchronized (consolesLock) {
            return consoles.size() > 1 || primaryConsole != null && primaryConsole.isLinkConnectionActive();
        }
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
        return consoleAt(index);
    }

    public int consoleCount() {
        synchronized (consolesLock) {
            return consoles.size();
        }
    }

    private Console consoleAt(int index) {
        synchronized (consolesLock) {
            if (index < 0 || index >= consoles.size()) {
                throw new IllegalArgumentException("Console " + (index + 1) + " is not active.");
            }
            return consoles.get(index);
        }
    }

    private List<Console> consoleSnapshot() {
        synchronized (consolesLock) {
            return new ArrayList<>(consoles);
        }
    }

    private void ensureSingleConsole(String operation) {
        synchronized (consolesLock) {
            if (consoles.size() != 1 || primaryConsole == null) {
                throw new IllegalStateException(operation + " is disabled while multiple consoles are active.");
            }
        }
    }

    private Console chooseConsole(String title) {
        List<Console> snapshot = consoleSnapshot();
        if (snapshot.isEmpty()) {
            return null;
        }
        if (snapshot.size() == 1) {
            return snapshot.getFirst();
        }
        String[] options = new String[snapshot.size()];
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
                return snapshot.get(i);
            }
        }
        return snapshot.getFirst();
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
