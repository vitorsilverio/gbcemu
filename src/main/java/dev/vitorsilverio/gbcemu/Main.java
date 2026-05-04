package dev.vitorsilverio.gbcemu;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class Main {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Main.class);
    private static final String DEFAULT_BIOS_KEY = "defaultBios";
    private static Emulator activeEmulator;
    private static Options activeOptions;
    private static EmulatorWindow window;

    public static void main(String[] args) {
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
            window = new EmulatorWindow(menuActions());
            window.show();
            return;
        }

        activeOptions = options;
        if (!options.headless()) {
            window = new EmulatorWindow(menuActions());
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
                Main::openDebugger,
                Main::pauseEmulator,
                Main::resumeEmulator,
                Main::stopEmulator
        );
    }

    private static void openRomFromMenu() {
        File romFile = chooseRomFile();
        if (romFile == null) {
            return;
        }
        Options baseOptions = activeOptions == null ? Options.empty() : activeOptions;
        startEmulator(baseOptions.withRomFile(romFile));
    }

    private static void openDebugger() {
        if (activeEmulator != null) {
            activeEmulator.openDebugger();
        }
    }

    private static void pauseEmulator() {
        if (activeEmulator != null) {
            activeEmulator.pause();
        }
    }

    private static void resumeEmulator() {
        if (activeEmulator != null) {
            activeEmulator.resume();
        }
    }

    private static void stopEmulator() {
        if (activeEmulator != null) {
            activeEmulator.stop();
            activeEmulator = null;
        }
    }

    private static File chooseRomFile() {
        JFileChooser chooser = new JFileChooser(currentDirectory());
        chooser.setDialogTitle("Open ROM");
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
                options.headless() ? null : window
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
