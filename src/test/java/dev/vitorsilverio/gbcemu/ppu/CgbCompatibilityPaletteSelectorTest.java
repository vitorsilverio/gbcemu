package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.cartridge.CartHeader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CgbCompatibilityPaletteSelectorTest {

    @Test
    void usesDefaultPaletteForNonNintendoLicensedRom() {
        CartHeader header = header(0x00, "ALLEY WAY");

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(0, selection.paletteId());
        assertEquals(0, selection.paletteGroup());
        assertEquals(16, selection.obj0PaletteWordOffset());
        assertEquals(16, selection.obj1PaletteWordOffset());
        assertEquals(116, selection.bgPaletteWordOffset());
        assertFalse(selection.logoTilemapRequired());
    }

    @Test
    void selectsPaletteIdByNintendoTitleChecksum() {
        CartHeader header = header(0x01, bytes(0x88));

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(1, selection.paletteId());
        assertEquals(4, selection.paletteGroup());
        assertEquals(36, selection.obj0PaletteWordOffset());
        assertEquals(36, selection.obj1PaletteWordOffset());
        assertEquals(36, selection.bgPaletteWordOffset());
        assertFalse(selection.logoTilemapRequired());
    }

    @Test
    void usesDefaultPaletteWhenNintendoTitleChecksumIsUnknown() {
        CartHeader header = header(0x01, bytes(0x02));

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(0, selection.paletteId());
    }

    @Test
    void disambiguatesDuplicateChecksumByFourthTitleLetter() {
        CartHeader header = header(0x01, bytes(0x00, 0x00, 0x00, 'U', 0x5E));

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(79, selection.paletteId());
    }

    @Test
    void usesDefaultPaletteWhenDuplicateChecksumFourthTitleLetterDoesNotMatch() {
        CartHeader header = header(0x01, bytes(0x00, 0x00, 0x00, 'Z', 0x59));

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(0, selection.paletteId());
    }

    @Test
    void marksLogoTilemapRequirementForSpecialPaletteIds() {
        CartHeader header = header(0x01, bytes(0x00, 0x00, 0x00, 'L', 0x1A));

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(0x58, selection.paletteId());
        assertEquals(0, selection.paletteGroup());
        assertTrue(selection.logoTilemapRequired());
    }

    @Test
    void mapsHighestAutomaticPaletteGroup() {
        CartHeader header = header(0x01, bytes(0x00, 0x00, 0x00, '-', 0xC7));

        CgbCompatibilityPaletteSelection selection = CgbCompatibilityPaletteSelector.select(header);

        assertEquals(78, selection.paletteId());
        assertEquals(50, selection.paletteGroup());
        assertEquals(16, selection.obj0PaletteWordOffset());
        assertEquals(112, selection.obj1PaletteWordOffset());
        assertEquals(116, selection.bgPaletteWordOffset());
    }

    private CartHeader header(int oldLicensee, String title) {
        return header(oldLicensee, title.getBytes(StandardCharsets.ISO_8859_1));
    }

    private CartHeader header(int oldLicensee, byte[] title) {
        byte[] rom = new byte[0x150];
        System.arraycopy(title, 0, rom, 0x0134, Math.min(title.length, 16));
        rom[0x0144] = '0';
        rom[0x0145] = '1';
        rom[0x0147] = 0x00;
        rom[0x0148] = 0x00;
        rom[0x0149] = 0x00;
        rom[0x014B] = (byte) oldLicensee;
        return new CartHeader(rom);
    }

    private byte[] bytes(int... values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }
}
