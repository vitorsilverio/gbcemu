package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.cpu.CpuState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmulatorSkipBiosTest {

    @TempDir
    Path tempDir;

    @Test
    void skipBiosUsesCgbHandOffRegistersForCgbCompatibleRom() throws IOException {
        Emulator emulator = new Emulator(null, romFile(0x80, 0x00, 0x00, "CGB GAME"), null, true);

        emulator.skipBios();
        CpuState cpu = emulator.createSaveStateFile().state().cpu();

        assertEquals(0x0100, cpu.pc());
        assertEquals(0xFFFE, cpu.sp());
        assertEquals(0x1180, cpu.af());
        assertEquals(0x0000, cpu.bc());
        assertEquals(0xFF56, cpu.de());
        assertEquals(0x000D, cpu.hl());
    }

    @Test
    void skipBiosUsesCgbDmgCompatibilityRegistersForNintendoDmgRom() throws IOException {
        Emulator emulator = new Emulator(null, romFile(0x00, 0x01, 0x00, "\u0001"), null, true);

        emulator.skipBios();
        CpuState cpu = emulator.createSaveStateFile().state().cpu();

        assertEquals(0x1180, cpu.af());
        assertEquals(0x0100, cpu.pc());
        assertEquals(0x0100, cpu.bc());
        assertEquals(0x0008, cpu.de());
        assertEquals(0x007C, cpu.hl());
    }

    @Test
    void skipBiosUsesLogoTilemapHandOffAddressForSpecialCgbDmgCompatibilityChecksum() throws IOException {
        Emulator emulator = new Emulator(null, romFile(0x00, 0x01, 0x00, "C"), null, true);

        emulator.skipBios();
        CpuState cpu = emulator.createSaveStateFile().state().cpu();

        assertEquals(0x4300, cpu.bc());
        assertEquals(0x991A, cpu.hl());
    }

    @Test
    void skipBiosAppliesDefaultCgbCompatibilityPalettesForNonNintendoDmgRom() throws IOException {
        Emulator emulator = new Emulator(null, romFile(0x00, 0x00, 0x00, "HOME GAME"), null, true);

        emulator.skipBios();
        var ppu = emulator.createSaveStateFile().state().ppu();

        assertArrayEquals(
                new byte[]{(byte) 0xFF, 0x7F, (byte) 0xEF, 0x1B, (byte) 0x80, 0x61, 0x00, 0x00},
                Arrays.copyOfRange(ppu.bgPalette(), 0, 8)
        );
        assertArrayEquals(
                new byte[]{(byte) 0xFF, 0x7F, 0x1F, 0x42, (byte) 0xF2, 0x1C, 0x00, 0x00},
                Arrays.copyOfRange(ppu.objPalette(), 0, 8)
        );
    }

    private File romFile(int cgbFlag, int oldLicensee, int romSize, String title) throws IOException {
        byte[] rom = new byte[32 * 1024];
        byte[] titleBytes = title.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(titleBytes, 0, rom, 0x0134, Math.min(titleBytes.length, 16));
        rom[0x0143] = (byte) cgbFlag;
        rom[0x0144] = '0';
        rom[0x0145] = '1';
        rom[0x0147] = 0x00;
        rom[0x0148] = (byte) romSize;
        rom[0x0149] = 0x00;
        rom[0x014B] = (byte) oldLicensee;
        File romFile = tempDir.resolve("test.gb").toFile();
        Files.write(romFile.toPath(), rom);
        return romFile;
    }
}
