package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CgbPaletteTest {

    @Test
    void convertsCgbRgb555PaletteColorToArgb() {
        CgbPalette palette = new CgbPalette();
        palette.setPaletteIndex((byte) 0);
        palette.setPaletteData((byte) 0x1F);
        palette.setPaletteIndex((byte) 1);
        palette.setPaletteData((byte) 0x00);

        assertEquals(0xFFFF0000, palette.getColor(0, 0));
    }

    @Test
    void autoIncrementWrapsPaletteAddress() {
        CgbPalette palette = new CgbPalette();

        palette.setPaletteIndex((byte) 0xBF);
        palette.setPaletteData((byte) 0x00);

        assertEquals(0x80, palette.getPaletteIndex() & 0xFF);
    }

    @Test
    void blockedPaletteWriteStillAutoIncrementsAddress() {
        CgbPalette palette = new CgbPalette();

        palette.setPaletteIndex((byte) 0x80);
        palette.setPaletteData((byte) 0x00, false);

        assertEquals(0x81, palette.getPaletteIndex() & 0xFF);
        assertEquals(0xFFFF, palette.getColor(0, 0) & 0xFFFF);
    }
}
