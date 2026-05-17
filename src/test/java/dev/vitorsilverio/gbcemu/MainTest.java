package dev.vitorsilverio.gbcemu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @BeforeEach
    void clearConfiguredDefaultBios() {
        Preferences.userNodeForPackage(Main.class).remove("defaultBios");
    }

    @Test
    void saveFileDefaultsToRomNameWithSavExtension() {
        Main.Options options = Main.Options.parse(new String[]{"--rom", "test-roms/pokemon.gbc"});

        assertEquals(new File("test-roms/pokemon.sav"), options.saveFile());
    }

    @Test
    void noSaveDisablesSaveFile() {
        Main.Options options = Main.Options.parse(new String[]{"--rom", "test-roms/pokemon.gbc", "--no-save"});

        assertNull(options.saveFile());
    }

    @Test
    void parsesHeadlessSkipBiosBiosAndExplicitSaveFile() {
        Main.Options options = Main.Options.parse(new String[]{
                "--headless",
                "--skip-bios",
                "--rom", "test-roms/instr_timing.gb",
                "--bios", "dmg_bios.bin",
                "--save-file", "debug.sav",
                "--max-frames", "120",
                "--dump-debug-on-exit",
                "--expect-serial", "Passed",
                "--fail-serial", "Failed"
        });

        assertTrue(options.headless());
        assertTrue(options.skipBios());
        assertTrue(options.dumpDebugOnExit());
        assertEquals(new File("test-roms/instr_timing.gb"), options.romFile());
        assertEquals(new File("dmg_bios.bin"), options.biosFile());
        assertEquals(new File("debug.sav"), options.saveFile());
        assertEquals(120, options.maxFrames());
        assertEquals("Passed", options.expectSerial());
        assertEquals("Failed", options.failSerial());
    }

    @Test
    void noBiosImpliesSkipBiosAndClearsBiosFile() {
        Main.Options options = Main.Options.parse(new String[]{
                "--headless",
                "--no-bios",
                "--rom", "test-roms/instr_timing.gb"
        });

        assertNull(options.biosFile());
        assertTrue(options.skipBios());
    }

    @Test
    void missingConfiguredBiosDefaultsToSkipBios() {
        Main.Options options = Main.Options.parse(new String[]{"--rom", "test-roms/pokemon.gbc"});

        assertNull(options.biosFile());
        assertTrue(options.skipBios());
    }

    @Test
    void maxFramesRequiresPositiveInteger() {
        assertThrows(IllegalArgumentException.class, () ->
                Main.Options.parse(new String[]{"--rom", "test-roms/instr_timing.gb", "--max-frames", "0"}));
        assertThrows(IllegalArgumentException.class, () ->
                Main.Options.parse(new String[]{"--rom", "test-roms/instr_timing.gb", "--max-frames", "abc"}));
    }

    @Test
    void romIsOptionalUntilRuntimeSelection() {
        Main.Options options = Main.Options.parse(new String[]{});

        assertNull(options.romFile());
        assertNull(options.biosFile());
        assertTrue(options.skipBios());
        assertNull(options.saveFile());
    }
}
