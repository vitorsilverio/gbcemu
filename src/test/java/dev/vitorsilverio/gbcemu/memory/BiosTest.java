package dev.vitorsilverio.gbcemu.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BiosTest {

    @TempDir
    Path tempDir;

    @Test
    void ff50ReadsUnmapRegisterInsteadOfBootRomArray() throws IOException {
        File biosFile = tempDir.resolve("cgb_bios.bin").toFile();
        Files.write(biosFile.toPath(), new byte[0x900]);
        Bios bios = new Bios(biosFile);

        assertEquals(0, bios.read(0xFF50));

        bios.write(0xFF50, (byte) 0x01);

        assertEquals(1, bios.read(0xFF50));
        assertFalse(bios.contains(0x0000));
        assertTrue(bios.contains(0xFF50));
    }

    @Test
    void ff50NotifiesWhenBootRomIsUnmapped() throws IOException {
        File biosFile = tempDir.resolve("cgb_bios.bin").toFile();
        Files.write(biosFile.toPath(), new byte[0x900]);
        boolean[] disabled = {false};
        Bios bios = new Bios(biosFile, () -> disabled[0] = true);

        bios.write(0xFF50, (byte) 0x01);

        assertTrue(disabled[0]);
    }

    @Test
    void ff50IgnoresZeroAndOnlyUnmapsOnce() throws IOException {
        File biosFile = tempDir.resolve("cgb_bios.bin").toFile();
        Files.write(biosFile.toPath(), new byte[0x900]);
        int[] disabledCount = {0};
        Bios bios = new Bios(biosFile, () -> disabledCount[0]++);

        bios.write(0xFF50, (byte) 0x00);
        assertEquals(0, bios.read(0xFF50));
        assertEquals(0, disabledCount[0]);

        bios.write(0xFF50, (byte) 0x11);
        bios.write(0xFF50, (byte) 0x11);

        assertEquals(1, bios.read(0xFF50));
        assertEquals(1, disabledCount[0]);
    }
}
