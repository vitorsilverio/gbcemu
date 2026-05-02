package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PpuControlTest {

    @Test
    void windowEnableUsesBitFive() {
        PpuControl control = new PpuControl();

        control.setData((byte) 0x20);

        assertTrue(control.isWindowEnabled());
        assertFalse(TileMapArea.IN_9C00.equals(control.getWindowTileArea()));
    }

    @Test
    void windowTileMapUsesBitSix() {
        PpuControl control = new PpuControl();

        control.setData((byte) 0x40);

        assertFalse(control.isWindowEnabled());
        assertTrue(TileMapArea.IN_9C00.equals(control.getWindowTileArea()));
    }
}
