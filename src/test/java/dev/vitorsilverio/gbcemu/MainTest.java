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
                "--save-file", "debug.sav"
        });

        assertTrue(options.headless());
        assertTrue(options.skipBios());
        assertEquals(new File("test-roms/instr_timing.gb"), options.romFile());
        assertEquals(new File("dmg_bios.bin"), options.biosFile());
        assertEquals(new File("debug.sav"), options.saveFile());
    }

    @Test
    void noBiosRequiresSkipBiosAndClearsBiosFile() {
        Main.Options options = Main.Options.parse(new String[]{
                "--headless",
                "--skip-bios",
                "--no-bios",
                "--rom", "test-roms/instr_timing.gb"
        });

        assertNull(options.biosFile());
        assertThrows(IllegalArgumentException.class, () ->
                Main.Options.parse(new String[]{"--no-bios", "--rom", "test-roms/instr_timing.gb"}));
    }

    @Test
    void romIsOptionalUntilRuntimeSelection() {
        Main.Options options = Main.Options.parse(new String[]{});

        assertNull(options.romFile());
        assertEquals(new File("cgb_bios.bin"), options.biosFile());
        assertNull(options.saveFile());
    }
}
