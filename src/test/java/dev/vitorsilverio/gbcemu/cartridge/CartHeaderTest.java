package dev.vitorsilverio.gbcemu.cartridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartHeaderTest {

    @Test
    void exposesLicenseeCodesUsedByCgbCompatibilityPaletteSelection() {
        byte[] rom = baseRom();
        rom[0x0144] = '0';
        rom[0x0145] = '1';
        rom[0x014B] = 0x33;

        CartHeader header = new CartHeader(rom);

        assertEquals(0x33, header.getOldLicenseeCode());
        assertEquals("01", header.getNewLicenseeCode());
        assertTrue(header.isNintendoLicensedForCgbCompatibilityPalettes());
    }

    @Test
    void usesOldLicenseeWhenHeaderDoesNotSelectNewLicenseeCode() {
        byte[] rom = baseRom();
        rom[0x0144] = '0';
        rom[0x0145] = '0';
        rom[0x014B] = 0x01;

        CartHeader header = new CartHeader(rom);

        assertTrue(header.isNintendoLicensedForCgbCompatibilityPalettes());
    }

    @Test
    void rejectsNonNintendoLicenseeForCgbCompatibilityPaletteSelection() {
        byte[] rom = baseRom();
        rom[0x0144] = '0';
        rom[0x0145] = '2';
        rom[0x014B] = 0x33;

        CartHeader header = new CartHeader(rom);

        assertFalse(header.isNintendoLicensedForCgbCompatibilityPalettes());
    }

    @Test
    void computesCgbCompatibilityPaletteTitleChecksumFromSixteenTitleBytes() {
        byte[] rom = baseRom();
        for (int i = 0; i < 16; i++) {
            rom[0x0134 + i] = (byte) (i + 1);
        }

        CartHeader header = new CartHeader(rom);

        assertEquals(0x88, header.getTitleChecksum());
    }

    @Test
    void cgbFlagIsNotIncludedInDisplayedTitle() {
        byte[] rom = baseRom();
        byte[] title = "CGB GAME".getBytes();
        System.arraycopy(title, 0, rom, 0x0134, title.length);
        rom[0x0143] = (byte) 0x80;

        CartHeader header = new CartHeader(rom);

        assertEquals("CGB GAME", header.getTitle());
    }

    private byte[] baseRom() {
        byte[] rom = new byte[0x150];
        rom[0x0147] = 0x00;
        rom[0x0148] = 0x00;
        rom[0x0149] = 0x00;
        return rom;
    }
}
