package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OamRAMTest {

    @Test
    void writesEachObjectAttributeFieldByAddressOffset() {
        OamRAM oam = new OamRAM();

        oam.write(0xFE00, (byte) 16);
        oam.write(0xFE01, (byte) 8);
        oam.write(0xFE02, (byte) 3);
        oam.write(0xFE03, (byte) 0xA0);

        assertEquals(16, oam.read(0xFE00) & 0xFF);
        assertEquals(8, oam.read(0xFE01) & 0xFF);
        assertEquals(3, oam.read(0xFE02) & 0xFF);
        assertEquals(0xA0, oam.read(0xFE03) & 0xFF);
    }
}
