
package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.core.Emulator;
import dev.vitorsilverio.gbcemu.gui.EmulatorMenuActions;
import dev.vitorsilverio.gbcemu.gui.EmulatorWindow;
import dev.vitorsilverio.gbcemu.gui.SaveStateDialog;
import dev.vitorsilverio.gbcemu.gui.SettingsDialog;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateStore;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class Main {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Main.class);
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
                Main::openSettings,
                Main::pauseEmulator,
                Main::resumeEmulator,
                Main::stopEmulator,
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
                Main::dumpMemoryBanks,
                Main::configureMultiplayer
        );
    }

    private static void configureMultiplayer() {
        if (activeEmulator != null) {
            activeEmulator.openMultiplayerDialog(Main::applySettings);
        }
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
        SAVE_STATE_STORE.load(activeOptions.romFile(), 0)
                .ifPresentOrElse(
                        slot -> activeEmulator.restoreSaveStateFile(slot.saveStateFile()),
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
        try {
            SAVE_STATE_STORE.save(activeOptions.romFile(), 0, activeEmulator.createSaveStateFile());
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

    private static void pauseEmulator() {
        if (activeEmulator != null) {
            activeEmulator.pause();
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
            showOverlay(EmulatorWindow.OverlayIcon.STOP);
        }
    }

    private static void showOverlay(EmulatorWindow.OverlayIcon icon) {
        if (window != null) {
            window.showOverlay(icon);
        }
    }

    private static File chooseRomFile() {
        JFileChooser chooser = new JFileChooser(currentDirectory());
        chooser.setDialogTitle("Open ROM");
        chooser.setFileFilter(new FileNameExtensionFilter("Games", "gb", "gbc"));
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    private static synchronized void startEmulator(Options options) {
        if (activeEmulator != null) {
            activeEmulator.stop();
        }
        activeOptions = options;
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
