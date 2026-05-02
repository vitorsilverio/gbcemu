package dev.vitorsilverio.gbcemu;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class Main {

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Main.class);
    private static final String DEFAULT_BIOS_KEY = "defaultBios";

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
            File selectedRom = chooseRomFile();
            if (selectedRom == null) {
                return;
            }
            options = options.withRomFile(selectedRom);
        }

        var emulator = new Emulator(
                options.biosFile(),
                options.romFile(),
                options.saveFile(),
                options.headless(),
                options.headless() ? null : menuActions(options)
        );
        if (options.skipBios()) {
            emulator.skipBios();
        }
        emulator.start();
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

    private static EmulatorMenuActions menuActions(Options options) {
        return new EmulatorMenuActions(
                () -> openRomFromMenu(options),
                Main::configureDefaultBios
        );
    }

    private static void openRomFromMenu(Options currentOptions) {
        File romFile = chooseRomFile();
        if (romFile == null) {
            return;
        }
        try {
            relaunch(currentOptions.withRomFile(romFile));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to relaunch emulator: " + e.getMessage(),
                    "GBC EMU", JOptionPane.ERROR_MESSAGE);
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

    private static void relaunch(Options options) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        command.addAll(options.toArgs());
        new ProcessBuilder(command).inheritIO().start();
        System.exit(0);
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

        private List<String> toArgs() {
            List<String> args = new ArrayList<>();
            if (romFile != null) {
                args.add("--rom");
                args.add(romFile.getAbsolutePath());
            }
            if (noBios) {
                args.add("--no-bios");
            } else if (biosFile != null) {
                args.add("--bios");
                args.add(biosFile.getAbsolutePath());
            }
            if (noSave) {
                args.add("--no-save");
            } else if (saveFile != null) {
                args.add("--save-file");
                args.add(saveFile.getAbsolutePath());
            }
            if (skipBios) {
                args.add("--skip-bios");
            }
            if (headless) {
                args.add("--headless");
            }
            return args;
        }

    }
}
