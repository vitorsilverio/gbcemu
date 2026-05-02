package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileTest {

    @Test
    void decodesLowAndHighBitplanesInGameBoyOrder() {
        Tile tile = new Tile();

        tile.setData(0, (byte) 0b1000_0000);
        tile.setData(1, (byte) 0b0100_0000);

        assertEquals(1, tile.getPixel(0, 0));
        assertEquals(2, tile.getPixel(1, 0));
        assertEquals(0, tile.getPixel(2, 0));
    }
}
