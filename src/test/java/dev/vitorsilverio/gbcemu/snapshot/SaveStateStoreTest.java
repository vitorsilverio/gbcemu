package dev.vitorsilverio.gbcemu.snapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveStateStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void storesMultipleSlotsBesideRomUsingSaExtensions() throws Exception {
        SaveStateStore store = new SaveStateStore();
        File romFile = tempDir.resolve("Pokemon Silver.gbc").toFile();

        store.save(romFile, 0, saveState("slot0", 12));
        store.save(romFile, 1, saveState("slot1", 34));

        var slots = store.list(romFile);

        assertEquals(2, slots.size());
        assertEquals(0, slots.get(0).index());
        assertEquals("Pokemon Silver.sa0", slots.get(0).file().getName());
        assertEquals(1, slots.get(1).index());
        assertEquals("Pokemon Silver.sa1", slots.get(1).file().getName());
        assertEquals(2, store.nextSlotIndex(romFile));
        assertTrue(slots.get(0).saveStateFile().metadata().romTitle().contains("slot0"));
        assertEquals("slot0", store.load(romFile, 0).orElseThrow().saveStateFile().metadata().romTitle());
    }

    private SaveStateFile saveState(String title, long frame) {
        return new SaveStateFile(
                SaveStateFile.CURRENT_FORMAT_VERSION,
                new SaveStateMetadata(
                        Instant.EPOCH,
                        title,
                        "rom.gbc",
                        "MBC3",
                        frame,
                        0x1234,
                        new int[]{0xFF000000},
                        1,
                        1
                ),
                null
        );
    }
}
