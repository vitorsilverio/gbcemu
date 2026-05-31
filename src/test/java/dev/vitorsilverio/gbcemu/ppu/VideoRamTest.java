package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VideoRamTest {

    @Test
    void tileMapStateContainsOnlyHardwareVisibleTileMaps() {
        VideoRam videoRam = new VideoRam();

        VideoRamState state = videoRam.saveState();

        assertEquals(0x800, state.tileMapIndexes().length);
        assertEquals(0x800, state.tileMapAttributes().length);
    }

    @Test
    void tileMapIndexAndAttributeBanksShareTheSameVisibleMapEntries() {
        VideoRam videoRam = new VideoRam();

        videoRam.write(0xFF4F, (byte) 0);
        videoRam.write(0x9800, (byte) 0x12);
        videoRam.write(0x9C00, (byte) 0x34);
        videoRam.write(0xFF4F, (byte) 1);
        videoRam.write(0x9800, (byte) 0x56);
        videoRam.write(0x9C00, (byte) 0x78);

        assertEquals(0x12, videoRam.readBank(0, 0x1800) & 0xFF);
        assertEquals(0x34, videoRam.readBank(0, 0x1C00) & 0xFF);
        assertEquals(0x56, videoRam.readBank(1, 0x1800) & 0xFF);
        assertEquals(0x78, videoRam.readBank(1, 0x1C00) & 0xFF);
    }
}
