
package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.CompositeController;
import dev.vitorsilverio.gbcemu.controller.GamepadController;
import dev.vitorsilverio.gbcemu.controller.KeyboardController;
import dev.vitorsilverio.gbcemu.core.Emulator;
import dev.vitorsilverio.gbcemu.gui.EmulatorMenuActions;
import dev.vitorsilverio.gbcemu.gui.EmulatorWindow;
import dev.vitorsilverio.gbcemu.gui.SaveStateDialog;
import dev.vitorsilverio.gbcemu.gui.SettingsDialog;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateStore;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class Main {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Main.class);
    private static final String LAST_ROM_DIRECTORY = "lastRomDirectory";
    private static final String RECENT_ROM_PREFIX = "recentRom";
    private static final int MAX_RECENT_ROMS = 10;
    private static AppSettings settings = AppSettings.load(PREFERENCES);
    private static Emulator activeEmulator;
    private static Options activeOptions;
    private static EmulatorWindow window;
    private static final SaveStateStore SAVE_STATE_STORE = new SaveStateStore();

    public static void main(String[] args) {
        var javaHome = System.getProperty("java.home", ".");
        System.setProperty("java.home", javaHome);
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage();
            return;
        }
        if (options.help()) {
            printUsage();
            return;
        }
        if (options.romFile() == null) {
            if (options.headless()) {
                System.err.println("--rom is required in headless mode");
                printUsage();
                return;
            }
            activeOptions = options;
            window = new EmulatorWindow(menuActions(), settings);
            window.show();
            return;
        }

        activeOptions = options;
        if (!options.headless()) {
            window = new EmulatorWindow(menuActions(), settings);
            window.show();
        }
        startEmulator(options);
    }

    private static void printUsage() {
        System.out.println("""
                Usage: gbcemu --rom <path> [options]

                Options:
                  --rom <path>        ROM file to load. Required.
                  --bios <path>       BIOS file to load. Defaults to the configured BIOS.
                  --no-bios           Do not load a BIOS file; implies --skip-bios.
                  --save-file <path>  Save file path. Defaults to ROM name with .sav extension.
                  --no-save           Disable save file persistence.
                  --skip-bios         Start directly at 0x0100 using default boot registers.
                  --headless          Run without window or audio output, unthrottled.
                  --max-frames <n>    Stop automatically after n rendered frames.
                  --dump-debug-on-exit
                                      Write a debug bundle when the emulator stops.
                  --expect-serial <text>
                                      Exit with code 2 if serial transcript does not contain text.
                  --fail-serial <text>
                                      Exit with code 3 if serial transcript contains text.
                  --help              Show this help.
                """);
    }

    private static EmulatorMenuActions menuActions() {
        return new EmulatorMenuActions(
                Main::openRomFromMenu,
                Main::recentRoms,
                Main::openRecentRom,
                Main::clearRecentRoms,
                Main::openLinkedSessionFromMenu,
                Main::openSettings,
                Main::pauseEmulator,
                Main::resumeEmulator,
                Main::stopEmulator,
                Main::restartEmulator,
                Main::saveSnapshot,
                Main::restoreSnapshot,
                Main::rewindSnapshot,
                Main::rewindSnapshotSilent,
                Main::openSaveStateDialog,
                Main::configureCheats,
                Main::openAudioDebugger,
                Main::openMemoryDebugger,
                Main::openPpuDebugger,
                Main::openCpuDebugger,
                Main::openCartDebugger,
                Main::dumpDebugBundle,
                Main::dumpMemoryBanks
        );
    }

    private static void dumpMemoryBanks() {
        if (activeEmulator == null) {
            JOptionPane.showMessageDialog(null,
                    "Load a ROM before dumping memory banks.",
                    "Memory dump",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            File dumpDirectory = activeEmulator.dumpMemoryBanks();
            JOptionPane.showMessageDialog(null,
                    "Memory banks written to:\n" + dumpDirectory.getAbsolutePath(),
                    "Memory dump",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to dump memory banks: " + e.getMessage(),
                    "Memory dump",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void dumpDebugBundle() {
        if (activeEmulator == null) {
            JOptionPane.showMessageDialog(null,
                    "Load a ROM before dumping debug data.",
                    "Debug dump",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            File dumpFile = activeEmulator.dumpDebugBundle();
            JOptionPane.showMessageDialog(null,
                    "Debug dump written to:\n" + dumpFile.getAbsolutePath(),
                    "Debug dump",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to dump debug data: " + e.getMessage(),
                    "Debug dump",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void configureCheats() {
        if (activeEmulator != null) {
            activeEmulator.openCheats();
        }
    }

    private static void openSettings() {
        SettingsDialog dialog = new SettingsDialog(window == null ? null : window.owner(), settings, Main::applySettings);
        dialog.setVisible(true);
    }

    private static void applySettings(AppSettings newSettings) {
        settings = newSettings.normalized();
        settings.save(PREFERENCES);
        if (window != null) {
            window.applySettings(settings);
        }
        if (activeEmulator != null) {
            activeEmulator.applySettings(settings);
        }
    }

    private static void openAudioDebugger() {
        if (activeEmulator != null) {
            activeEmulator.openAudioDebugger();
        }
    }

    private static void openMemoryDebugger() {
        if (activeEmulator != null) {
            activeEmulator.openMemoryDebugger();
        }
    }

    private static void openPpuDebugger() {
        if (activeEmulator != null) {
            activeEmulator.openPpuDebugger();
        }
    }

    private static void openCpuDebugger() {
        if (activeEmulator != null) {
            activeEmulator.openCpuDebugger();
        }
    }

    private static void openCartDebugger() {
        if (activeEmulator != null) {
            activeEmulator.openCartDebugger();
        }
    }

    private static void restoreSnapshot() {
        if (!hasActiveRomForSaveStates()) {
            return;
        }
        if (isLinkOperationBlocked("Load state")) {
            return;
        }
        SAVE_STATE_STORE.load(activeOptions.romFile(), 0)
                .ifPresentOrElse(
                        slot -> {
                            activeEmulator.restoreSaveStateFile(slot.saveStateFile());
                            showOverlay(EmulatorWindow.OverlayIcon.LOAD);
                        },
                        () -> JOptionPane.showMessageDialog(null,
                                "Slot 0 is empty for this ROM.",
                                "Save states",
                                JOptionPane.INFORMATION_MESSAGE)
                );
    }

    private static void rewindSnapshot() {
        rewindSnapshot(true);
    }

    private static void rewindSnapshotSilent() {
        rewindSnapshot(false);
    }

    private static void rewindSnapshot(boolean notifyWhenUnavailable) {
        if (activeEmulator == null) {
            return;
        }
        if (activeEmulator.isLinkConnectionActive()) {
            if (notifyWhenUnavailable) {
                showLinkBlockedMessage("Rewind");
            }
            return;
        }
        if (!activeEmulator.rewindOneSnapshot()) {
            if (notifyWhenUnavailable) {
                JOptionPane.showMessageDialog(null,
                        "No rewind snapshot is available yet.",
                        "Rewind",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        showOverlay(EmulatorWindow.OverlayIcon.REWIND);
    }

    private static void saveSnapshot() {
        if (!hasActiveRomForSaveStates()) {
            return;
        }
        if (isLinkOperationBlocked("Save state")) {
            return;
        }
        try {
            SAVE_STATE_STORE.save(activeOptions.romFile(), 0, activeEmulator.createSaveStateFile());
            showOverlay(EmulatorWindow.OverlayIcon.SAVE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to save slot 0: " + e.getMessage(),
                    "Save states",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean hasActiveRomForSaveStates() {
        if (activeEmulator == null || activeOptions == null || activeOptions.romFile() == null) {
            JOptionPane.showMessageDialog(null,
                    "Load a ROM before using save states.",
                    "Save states",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    private static void openSaveStateDialog() {
        if (!hasActiveRomForSaveStates()) {
            return;
        }
        if (isLinkOperationBlocked("Save states")) {
            return;
        }
        SaveStateDialog dialog = new SaveStateDialog(
                activeOptions.romFile(),
                SAVE_STATE_STORE,
                activeEmulator::createSaveStateFile,
                activeEmulator::restoreSaveStateFile
        );
        dialog.setVisible(true);
    }

    private static void openRomFromMenu() {
        File romFile = chooseRomFile();
        if (romFile == null) {
            return;
        }
        Options baseOptions = activeOptions == null ? Options.empty() : activeOptions;
        startEmulator(baseOptions.withRomFile(romFile));
    }

    private static void openRecentRom(File romFile) {
        if (romFile == null) {
            return;
        }
        if (!romFile.isFile()) {
            removeRecentRom(romFile);
            JOptionPane.showMessageDialog(null,
                    "Recent ROM no longer exists:\n" + romFile.getAbsolutePath(),
                    "Recent ROMs",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Options baseOptions = activeOptions == null ? Options.empty() : activeOptions;
        startEmulator(baseOptions.withRomFile(romFile));
    }

    private static void openLinkedSessionFromMenu() {
        File player1Rom = chooseRomFile("Open Player 1 ROM");
        if (player1Rom == null) {
            return;
        }
        File player2Rom = chooseRomFile("Open Player 2 ROM");
        if (player2Rom == null) {
            return;
        }

        Options baseOptions = activeOptions == null ? Options.empty() : activeOptions;
        Options player1Options = baseOptions.withRomFile(player1Rom);
        Options player2Options = baseOptions.withRomFile(player2Rom);
        addRecentRom(player1Rom);
        addRecentRom(player2Rom);
        stopCurrentRuntime();
        activeOptions = null;

        if (window == null) {
            window = new EmulatorWindow(menuActions(), settings);
            window.show();
        }
        KeyboardController player1Controller = new KeyboardController(settings);
        KeyboardController player2Controller = new KeyboardController(player2KeyCodes(), 0, false);
        CompositeController player1Input = new CompositeController(player1Controller, new GamepadController(settings.gamepadConfig(0)));
        CompositeController player2Input = new CompositeController(player2Controller, new GamepadController(settings.gamepadConfig(1)));
        activeEmulator = Emulator.linked(
                new Emulator.PlayerConfig(
                        player1Options.biosFile(),
                        player1Options.romFile(),
                        player1Options.saveFile(),
                        window,
                        player1Input,
                        player1Controller,
                        player1Options.skipBios(),
                        false
                ),
                new Emulator.PlayerConfig(
                        player2Options.biosFile(),
                        player2Options.romFile(),
                        player2Options.saveFile(),
                        window,
                        player2Input,
                        player2Controller,
                        player2Options.skipBios(),
                        true
                ),
                settings
        );
        Thread thread = new Thread(activeEmulator::start, "gbcemu-runtime");
        thread.setDaemon(false);
        thread.start();
        showOverlay(EmulatorWindow.OverlayIcon.PLAY);
    }

    private static int[] player2KeyCodes() {
        int[] keyCodes = new int[AppSettings.CONTROLLER_BUTTON_NAMES.length];
        for (int i = 0; i < keyCodes.length; i++) {
            keyCodes[i] = settings.player2ControllerKeyCode(i);
        }
        return keyCodes;
    }

    private static void pauseEmulator() {
        if (activeEmulator != null) {
            if (activeEmulator.isLinkConnectionActive()) {
                activeEmulator.pauseSession();
            } else {
                activeEmulator.pause();
            }
            showOverlay(EmulatorWindow.OverlayIcon.PAUSE);
        }
    }

    private static void resumeEmulator() {
        if (activeEmulator != null) {
            activeEmulator.resume();
            showOverlay(EmulatorWindow.OverlayIcon.PLAY);
        }
    }

    private static void stopEmulator() {
        if (activeEmulator != null) {
            activeEmulator.stop();
            activeEmulator = null;
            if (window != null) {
                window.resetTitle();
            }
            showOverlay(EmulatorWindow.OverlayIcon.STOP);
        }
    }

    private static void restartEmulator() {
        if (activeOptions == null || activeOptions.romFile() == null) {
            JOptionPane.showMessageDialog(null,
                    "Load a ROM before restarting.",
                    "Restart",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        startEmulator(activeOptions);
        showOverlay(EmulatorWindow.OverlayIcon.PLAY);
    }

    private static void showOverlay(EmulatorWindow.OverlayIcon icon) {
        if (window != null) {
            window.showOverlay(icon);
        }
    }

    private static boolean isLinkOperationBlocked(String operation) {
        if (activeEmulator == null || !activeEmulator.isLinkConnectionActive()) {
            return false;
        }
        showLinkBlockedMessage(operation);
        return true;
    }

    private static void showLinkBlockedMessage(String operation) {
        JOptionPane.showMessageDialog(null,
                operation + " is disabled while a link connection is active.",
                "Link cable",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static File chooseRomFile() {
        return chooseRomFile("Open ROM");
    }

    private static File chooseRomFile(String title) {
        JFileChooser chooser = new JFileChooser(lastRomDirectory());
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Games", "gb", "gbc"));
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File selected = chooser.getSelectedFile();
        File parent = selected.getParentFile();
        if (parent != null) {
            PREFERENCES.put(LAST_ROM_DIRECTORY, parent.getAbsolutePath());
        }
        return selected;
    }

    private static synchronized void startEmulator(Options options) {
        stopCurrentRuntime();
        activeOptions = options;
        if (!options.headless() && options.romFile() != null) {
            addRecentRom(options.romFile());
        }
        Emulator emulator = new Emulator(
                options.biosFile(),
                options.romFile(),
                options.saveFile(),
                options.headless(),
                options.headless() ? null : window,
                settings
        );
        if (options.skipBios()) {
            emulator.skipBios();
        }
        emulator.stopAfterFrames(options.maxFrames());
        activeEmulator = emulator;
        if (options.headless()) {
            emulator.start();
            dumpDebugOnExit(options, emulator);
            exitFromSerialExpectations(options, emulator);
            return;
        }
        Thread thread = new Thread(emulator::start, "gbcemu-runtime");
        thread.setDaemon(false);
        thread.start();
    }

    private static void stopCurrentRuntime() {
        if (activeEmulator != null) {
            activeEmulator.stop();
            activeEmulator = null;
        }
    }

    private static void dumpDebugOnExit(Options options, Emulator emulator) {
        if (!options.dumpDebugOnExit()) {
            return;
        }
        File dumpFile = emulator.dumpDebugBundle();
        System.out.println("Debug dump written to: " + dumpFile.getAbsolutePath());
    }

    private static void exitFromSerialExpectations(Options options, Emulator emulator) {
        int exitCode = serialExpectationExitCode(options, emulator.serialTranscript());
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static int serialExpectationExitCode(Options options, String transcript) {
        if (!options.failSerial().isBlank() && transcript.contains(options.failSerial())) {
            System.err.println("Serial transcript contains fail marker: " + options.failSerial());
            return 3;
        }
        if (!options.expectSerial().isBlank() && !transcript.contains(options.expectSerial())) {
            System.err.println("Serial transcript did not contain expected marker: " + options.expectSerial());
            return 2;
        }
        return 0;
    }

    private static File defaultBiosFile() {
        String configured = settings.defaultBiosPath();
        return configured == null || configured.isBlank() ? null : new File(configured);
    }

    private static File currentDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    private static File lastRomDirectory() {
        String configured = PREFERENCES.get(LAST_ROM_DIRECTORY, "");
        if (configured.isBlank()) {
            return currentDirectory();
        }
        File directory = new File(configured);
        return directory.isDirectory() ? directory : currentDirectory();
    }

    private static List<File> recentRoms() {
        List<File> files = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < MAX_RECENT_ROMS; i++) {
            String path = PREFERENCES.get(RECENT_ROM_PREFIX + i, "");
            if (path.isBlank()) {
                continue;
            }
            File file = new File(path);
            if (!file.isFile()) {
                changed = true;
                continue;
            }
            if (!containsSameFile(files, file)) {
                files.add(file);
            } else {
                changed = true;
            }
        }
        if (changed) {
            saveRecentRoms(files);
        }
        return files;
    }

    private static void addRecentRom(File romFile) {
        if (romFile == null) {
            return;
        }
        File normalized = normalizeFile(romFile);
        List<File> files = new ArrayList<>();
        files.add(normalized);
        for (File recent : recentRoms()) {
            if (!sameFile(normalized, recent) && files.size() < MAX_RECENT_ROMS) {
                files.add(recent);
            }
        }
        saveRecentRoms(files);
    }

    private static void removeRecentRom(File romFile) {
        if (romFile == null) {
            return;
        }
        File normalized = normalizeFile(romFile);
        List<File> files = new ArrayList<>();
        for (File recent : recentRoms()) {
            if (!sameFile(normalized, recent)) {
                files.add(recent);
            }
        }
        saveRecentRoms(files);
    }

    private static void clearRecentRoms() {
        saveRecentRoms(List.of());
    }

    private static void saveRecentRoms(List<File> files) {
        for (int i = 0; i < MAX_RECENT_ROMS; i++) {
            if (i < files.size()) {
                PREFERENCES.put(RECENT_ROM_PREFIX + i, normalizeFile(files.get(i)).getAbsolutePath());
            } else {
                PREFERENCES.remove(RECENT_ROM_PREFIX + i);
            }
        }
    }

    private static boolean containsSameFile(List<File> files, File file) {
        for (File candidate : files) {
            if (sameFile(candidate, file)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameFile(File first, File second) {
        return normalizeFile(first).equals(normalizeFile(second));
    }

    private static File normalizeFile(File file) {
        return file == null ? new File("") : file.getAbsoluteFile();
    }

    record Options(
            File romFile,
            File biosFile,
            File saveFile,
            boolean headless,
            boolean skipBios,
            boolean help,
            boolean noSave,
            boolean noBios,
            long maxFrames,
            boolean dumpDebugOnExit,
            String expectSerial,
            String failSerial
    ) {
        static Options empty() {
            File biosFile = defaultBiosFile();
            return new Options(null, biosFile, null, false, biosFile == null, false, false, false, -1, false, "", "");
        }

        static Options parse(String[] args) {
            File romFile = null;
            File biosFile = defaultBiosFile();
            File saveFile = null;
            boolean explicitSaveFile = false;
            boolean noSave = false;
            boolean headless = false;
            boolean skipBios = false;
            boolean noBios = false;
            boolean help = false;
            long maxFrames = -1;
            boolean dumpDebugOnExit = false;
            String expectSerial = "";
            String failSerial = "";

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--rom" -> romFile = new File(requireValue(args, ++i, "--rom"));
                    case "--bios" -> biosFile = new File(requireValue(args, ++i, "--bios"));
                    case "--no-bios" -> noBios = true;
                    case "--save-file" -> {
                        saveFile = new File(requireValue(args, ++i, "--save-file"));
                        explicitSaveFile = true;
                    }
                    case "--no-save" -> noSave = true;
                    case "--headless" -> headless = true;
                    case "--skip-bios" -> skipBios = true;
                    case "--max-frames" -> maxFrames = parsePositiveLong(requireValue(args, ++i, "--max-frames"), "--max-frames");
                    case "--dump-debug-on-exit" -> dumpDebugOnExit = true;
                    case "--expect-serial" -> expectSerial = requireValue(args, ++i, "--expect-serial");
                    case "--fail-serial" -> failSerial = requireValue(args, ++i, "--fail-serial");
                    case "--help", "-h" -> help = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i] + ". Args: " + Arrays.toString(args));
                }
            }

            if (help) {
                return new Options(romFile, biosFile, saveFile, headless, skipBios, true, noSave, noBios, maxFrames, dumpDebugOnExit, expectSerial, failSerial);
            }
            if (noSave && explicitSaveFile) {
                throw new IllegalArgumentException("--no-save and --save-file cannot be used together");
            }
            if (!noSave && saveFile == null) {
                saveFile = defaultSaveFile(romFile);
            }
            if (noBios) {
                biosFile = null;
            }
            if (biosFile == null) {
                skipBios = true;
            }
            return new Options(romFile, biosFile, noSave ? null : saveFile, headless, skipBios, false, noSave, noBios, maxFrames, dumpDebugOnExit, expectSerial, failSerial);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
        }

        private static long parsePositiveLong(String value, String option) {
            try {
                long parsed = Long.parseLong(value);
                if (parsed <= 0) {
                    throw new NumberFormatException();
                }
                return parsed;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(option + " requires a positive integer");
            }
        }

        private static File defaultSaveFile(File romFile) {
            if (romFile == null) {
                return null;
            }
            String name = romFile.getName();
            int dot = name.lastIndexOf('.');
            String saveName = (dot >= 0 ? name.substring(0, dot) : name) + ".sav";
            File parent = romFile.getParentFile();
            return parent == null ? new File(saveName) : new File(parent, saveName);
        }

        private Options withRomFile(File romFile) {
            File resolvedSaveFile = noSave ? null : defaultSaveFile(romFile);
            File resolvedBiosFile = noBios ? null : defaultBiosFile();
            boolean skipBiosWasImpliedByMissingBios = skipBios && biosFile == null && !noBios;
            boolean resolvedSkipBios = skipBiosWasImpliedByMissingBios
                    ? resolvedBiosFile == null
                    : skipBios || resolvedBiosFile == null;
            return new Options(romFile, resolvedBiosFile, resolvedSaveFile, headless, resolvedSkipBios, help, noSave, noBios, maxFrames, dumpDebugOnExit, expectSerial, failSerial);
        }

    }
}
