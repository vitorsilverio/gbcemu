package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.snapshot.SaveStateStore;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class Main {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Main.class);
    private static final String DEFAULT_BIOS_KEY = "defaultBios";
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
                  --bios <path>       BIOS file to load. Defaults to cgb_bios.bin.
                  --no-bios           Do not load a BIOS file. Requires --skip-bios.
                  --save-file <path>  Save file path. Defaults to ROM name with .sav extension.
                  --no-save           Disable save file persistence.
                  --skip-bios         Start directly at 0x0100 using default DMG registers.
                  --headless          Run without window or audio output, unthrottled.
                  --help              Show this help.
                """);
    }

    private static EmulatorMenuActions menuActions() {
        return new EmulatorMenuActions(
                Main::openRomFromMenu,
                Main::configureDefaultBios,
                Main::openSettings,
                Main::pauseEmulator,
                Main::resumeEmulator,
                Main::stopEmulator,
                Main::saveSnapshot,
                Main::restoreSnapshot,
                Main::rewindSnapshot,
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
        if (activeEmulator == null) {
            return;
        }
        if (!activeEmulator.rewindOneSnapshot()) {
            JOptionPane.showMessageDialog(null,
                    "No rewind snapshot is available yet.",
                    "Rewind",
                    JOptionPane.INFORMATION_MESSAGE);
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

    private static void configureDefaultBios() {
        JFileChooser chooser = new JFileChooser(currentDirectory());
        chooser.setDialogTitle("Set default BIOS");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File biosFile = chooser.getSelectedFile();
        PREFERENCES.put(DEFAULT_BIOS_KEY, biosFile.getAbsolutePath());
        JOptionPane.showMessageDialog(null,
                "Default BIOS updated. It will be used the next time a ROM is opened.",
                "GBC EMU",
                JOptionPane.INFORMATION_MESSAGE);
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
        activeEmulator = emulator;
        if (options.headless()) {
            emulator.start();
            return;
        }
        Thread thread = new Thread(emulator::start, "gbcemu-runtime");
        thread.setDaemon(false);
        thread.start();
    }

    private static File defaultBiosFile() {
        String configured = PREFERENCES.get(DEFAULT_BIOS_KEY, null);
        return configured == null || configured.isBlank() ? new File("cgb_bios.bin") : new File(configured);
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
            boolean noBios
    ) {
        static Options empty() {
            return new Options(null, defaultBiosFile(), null, false, false, false, false, false);
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
                    case "--help", "-h" -> help = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i] + ". Args: " + Arrays.toString(args));
                }
            }

            if (help) {
                return new Options(romFile, biosFile, saveFile, headless, skipBios, true, noSave, noBios);
            }
            if (noBios && !skipBios) {
                throw new IllegalArgumentException("--no-bios requires --skip-bios");
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
            return new Options(romFile, biosFile, noSave ? null : saveFile, headless, skipBios, false, noSave, noBios);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
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
            return new Options(romFile, biosFile, resolvedSaveFile, headless, skipBios, help, noSave, noBios);
        }

    }
}
