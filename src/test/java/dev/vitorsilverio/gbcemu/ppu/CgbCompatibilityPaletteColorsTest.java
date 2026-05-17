package dev.vitorsilverio.gbcemu.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CgbCompatibilityPaletteColorsTest {

    @Test
    void returnsFourRgb555ColorsStartingAtPaletteWordOffset() {
        assertArrayEquals(
                new int[]{0x7FFF, 0x421F, 0x1CF2, 0x0000},
                CgbCompatibilityPaletteColors.colors(16)
        );
    }

    @Test
    void supportsRawOffsetsThatStartInsideAPalette() {
        assertArrayEquals(
                new int[]{0x7FFF, 0x7FFF, 0x7E8C, 0x7C00},
                CgbCompatibilityPaletteColors.colors(111)
        );
    }

    @Test
    void exportsColorsAsLittleEndianPaletteBytes() {
        assertArrayEquals(
                new byte[]{(byte) 0xFF, 0x7F, 0x1F, 0x42, (byte) 0xF2, 0x1C, 0x00, 0x00},
                CgbCompatibilityPaletteColors.littleEndianBytes(16)
        );
    }

    @Test
    void rejectsPaletteWordOffsetThatCannotFitFourColors() {
        assertThrows(IllegalArgumentException.class, () -> CgbCompatibilityPaletteColors.colors(125));
    }
}
