package dev.vitorsilverio.gbcemu.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
