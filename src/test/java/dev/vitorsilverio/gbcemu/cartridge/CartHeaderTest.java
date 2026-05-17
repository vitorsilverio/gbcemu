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

    @Test
    void decodesStandardRomSizeCodeAsUnsignedValue() {
        byte[] rom = baseRom();
        rom[0x0148] = 0x05;

        CartHeader header = new CartHeader(rom);

        assertEquals(1024 * 1024, header.getRomSizeBytes());
    }

    @Test
    void decodesUnofficialRomSizeCodes() {
        byte[] rom = baseRom();

        rom[0x0148] = 0x52;
        assertEquals(72 * 16 * 1024, new CartHeader(rom).getRomSizeBytes());

        rom[0x0148] = 0x53;
        assertEquals(80 * 16 * 1024, new CartHeader(rom).getRomSizeBytes());

        rom[0x0148] = 0x54;
        assertEquals(96 * 16 * 1024, new CartHeader(rom).getRomSizeBytes());
    }

    @Test
    void exposesRamSizeCodeAsUnsignedValue() {
        byte[] rom = baseRom();
        rom[0x0149] = (byte) 0xFF;

        CartHeader header = new CartHeader(rom);

        assertEquals(0xFF, header.getRamSizeCode());
        assertEquals(0, header.getRamSizeBytes());
    }

    @Test
    void readsEntryPointFromHeaderEntryPointBytesNotNintendoLogo() {
        byte[] rom = baseRom();
        rom[0x0100] = (byte) 0xC3;
        rom[0x0101] = 0x50;
        rom[0x0102] = 0x01;
        rom[0x0103] = 0x00;
        rom[0x0104] = (byte) 0xCE;
        rom[0x0105] = (byte) 0xED;

        CartHeader header = new CartHeader(rom);

        assertEquals(0xC3500100, header.getEntryPoint());
    }

    @Test
    void readsOnlyFortyEightNintendoLogoBytes() {
        byte[] rom = baseRom();
        for (int i = 0; i < 0x30; i++) {
            rom[0x0104 + i] = (byte) i;
        }
        rom[0x0134] = 0x7F;

        CartHeader header = new CartHeader(rom);

        assertEquals(0x30, header.getNintendoLogo().length);
        assertEquals(0x2F, header.getNintendoLogo()[0x2F] & 0xFF);
    }

    @Test
    void exposesAndValidatesHeaderChecksum() {
        byte[] rom = baseRom();
        byte[] title = "CHECK".getBytes();
        System.arraycopy(title, 0, rom, 0x0134, title.length);
        rom[0x014D] = (byte) computedHeaderChecksum(rom);

        CartHeader header = new CartHeader(rom);

        assertEquals(rom[0x014D] & 0xFF, header.getHeaderChecksum());
        assertEquals(rom[0x014D] & 0xFF, header.getComputedHeaderChecksum());
        assertTrue(header.isHeaderChecksumValid());
    }

    @Test
    void exposesGlobalChecksumAsUnsignedBigEndianValue() {
        byte[] rom = baseRom();
        rom[0x014E] = 0x12;
        rom[0x014F] = 0x34;

        CartHeader header = new CartHeader(rom);

        assertEquals(0x1234, header.getGlobalChecksum());
    }

    private byte[] baseRom() {
        byte[] rom = new byte[0x150];
        rom[0x0147] = 0x00;
        rom[0x0148] = 0x00;
        rom[0x0149] = 0x00;
        return rom;
    }

    private int computedHeaderChecksum(byte[] rom) {
        int checksum = 0;
        for (int address = 0x0134; address <= 0x014C; address++) {
            checksum = (checksum - ((rom[address] & 0xFF) + 1)) & 0xFF;
        }
        return checksum;
    }
}
