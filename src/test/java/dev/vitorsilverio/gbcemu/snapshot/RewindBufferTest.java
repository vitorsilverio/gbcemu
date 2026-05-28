package dev.vitorsilverio.gbcemu.snapshot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RewindBufferTest {

    @Test
    void keepsOnlyLatestStatesUpToCapacity() {
        RewindBuffer buffer = new RewindBuffer(2);

        buffer.add(saveState(1));
        buffer.add(saveState(2));
        buffer.add(saveState(3));

        assertEquals(2, buffer.size());
        assertEquals(3, buffer.popLatest().orElseThrow().metadata().frameNumber());
        assertEquals(2, buffer.popLatest().orElseThrow().metadata().frameNumber());
        assertFalse(buffer.popLatest().isPresent());
    }

    @Test
    void ignoresNullStates() {
        RewindBuffer buffer = new RewindBuffer(1);

        buffer.add(null);

        assertEquals(0, buffer.size());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RewindBuffer(-1));
    }

    @Test
    void zeroCapacityDisablesCapture() {
        RewindBuffer buffer = new RewindBuffer(0);

        buffer.add(saveState(1));

        assertEquals(0, buffer.size());
        assertFalse(buffer.popLatest().isPresent());
    }

    @Test
    void clearRemovesCapturedStates() {
        RewindBuffer buffer = new RewindBuffer(2);

        buffer.add(saveState(1));
        buffer.clear();

        assertTrue(buffer.popLatest().isEmpty());
    }

    private static SaveStateFile saveState(long frameNumber) {
        return new SaveStateFile(
                SaveStateFile.CURRENT_FORMAT_VERSION,
                new SaveStateMetadata(
                        Instant.EPOCH,
                        "TEST",
                        "test.gb",
                        "ROM_ONLY",
                        frameNumber,
                        0x100,
                        new int[0],
                        0,
                        0
                ),
                null
        );
    }
}
