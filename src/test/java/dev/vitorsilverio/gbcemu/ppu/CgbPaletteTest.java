package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void setsFullPaletteFromLittleEndianBytes() {
        CgbPalette palette = new CgbPalette();

        palette.setPaletteBytes(2, new byte[]{
                0x1F, 0x00,
                (byte) 0xE0, 0x03,
                0x00, 0x7C,
                0x00, 0x00
        });

        assertEquals(0xFFFF0000, palette.getColor(2, 0));
        assertEquals(0xFF00FF00, palette.getColor(2, 1));
        assertEquals(0xFF0000FF, palette.getColor(2, 2));
        assertEquals(0xFF000000, palette.getColor(2, 3));
    }

    @Test
    void rejectsInvalidPaletteByteLength() {
        CgbPalette palette = new CgbPalette();

        assertThrows(IllegalArgumentException.class, () -> palette.setPaletteBytes(0, new byte[2]));
    }
}
