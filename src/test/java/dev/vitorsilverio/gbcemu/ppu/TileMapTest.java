package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileMapTest {

    @Test
    void tileIndexIsUnsigned() {
        TileMap tileMap = new TileMap();

        tileMap.setIndex((byte) 0xFE);

        assertEquals(0xFE, tileMap.getIndex());
    }
}
