package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.cartridge.CartHeader;

public final class CgbCompatibilityPaletteSelector {

    private static final int DUPLICATE_CHECKSUM_START = 65;
    private static final int DUPLICATE_ROW_LENGTH = 14;

    private static final int[] TITLE_CHECKSUMS = {
            0x00, 0x88, 0x16, 0x36, 0xD1, 0xDB, 0xF2, 0x3C,
            0x8C, 0x92, 0x3D, 0x5C, 0x58, 0xC9, 0x3E, 0x70,
            0x1D, 0x59, 0x69, 0x19, 0x35, 0xA8, 0x14, 0xAA,
            0x75, 0x95, 0x99, 0x34, 0x6F, 0x15, 0xFF, 0x97,
            0x4B, 0x90, 0x17, 0x10, 0x39, 0xF7, 0xF6, 0xA2,
            0x49, 0x4E, 0x43, 0x68, 0xE0, 0x8B, 0xF0, 0xCE,
            0x0C, 0x29, 0xE8, 0xB7, 0x86, 0x9A, 0x52, 0x01,
            0x9D, 0x71, 0x9C, 0xBD, 0x5D, 0x6D, 0x67, 0x3F,
            0x6B, 0xB3, 0x46, 0x28, 0xA5, 0xC6, 0xD3, 0x27,
            0x61, 0x18, 0x66, 0x6A, 0xBF, 0x0D, 0xF4, 0xB3,
            0x46, 0x28, 0xA5, 0xC6, 0xD3, 0x27, 0x61, 0x18,
            0x66, 0x6A, 0xBF, 0x0D, 0xF4, 0xB3
    };

    private static final String DUPLICATE_FOURTH_LETTERS = "BEFAARBEKEK R-URAR INAILICE R";

    private static final int[] PALETTE_GROUP_BY_ID = {
            0, 4, 5, 35, 34, 3, 31, 15,
            10, 5, 19, 36, 7, 37, 30, 44,
            21, 32, 31, 20, 5, 33, 13, 14,
            5, 29, 5, 18, 9, 3, 2, 26,
            25, 25, 41, 42, 26, 45, 42, 45,
            36, 38, 26, 42, 30, 41, 34, 34,
            5, 42, 6, 5, 33, 25, 42, 42,
            40, 2, 16, 25, 42, 42, 5, 0,
            39, 36, 22, 25, 6, 32, 12, 36,
            11, 39, 18, 39, 24, 31, 50, 17,
            46, 6, 27, 0, 47, 41, 41, 0,
            0, 19, 34, 23, 18, 29
    };

    private static final int[][] PALETTE_WORD_OFFSETS = {
            {16, 16, 116}, {72, 72, 72}, {80, 80, 80}, {96, 96, 96},
            {36, 36, 36}, {0, 0, 0}, {108, 108, 108}, {20, 20, 20},
            {48, 48, 48}, {104, 104, 104}, {64, 32, 32}, {16, 112, 112},
            {16, 8, 8}, {12, 16, 16}, {16, 116, 116}, {112, 16, 112},
            {8, 68, 8}, {64, 64, 32}, {16, 16, 28}, {16, 16, 72},
            {16, 16, 80}, {76, 76, 36}, {15, 15, 44}, {68, 68, 8},
            {16, 16, 8}, {16, 16, 12}, {112, 112, 0}, {12, 12, 0},
            {0, 0, 4}, {72, 88, 72}, {80, 88, 80}, {96, 88, 96},
            {64, 88, 32}, {68, 16, 52}, {111, 0, 56}, {111, 16, 60},
            {76, 91, 36}, {64, 112, 40}, {16, 92, 112}, {68, 88, 8},
            {16, 0, 8}, {16, 112, 12}, {112, 12, 0}, {12, 112, 16},
            {84, 112, 16}, {12, 112, 0}, {100, 12, 112}, {0, 112, 32},
            {16, 12, 112}, {112, 12, 24}, {16, 112, 116}
    };

    private CgbCompatibilityPaletteSelector() {
    }

    public static CgbCompatibilityPaletteSelection select(CartHeader header) {
        int paletteId = selectPaletteId(header);
        int paletteGroup = PALETTE_GROUP_BY_ID[paletteId];
        int[] paletteCombination = PALETTE_WORD_OFFSETS[paletteGroup];
        return new CgbCompatibilityPaletteSelection(
                paletteId,
                paletteGroup,
                paletteCombination[0],
                paletteCombination[1],
                paletteCombination[2],
                paletteId == 0x43 || paletteId == 0x58
        );
    }

    private static int selectPaletteId(CartHeader header) {
        if (!header.isNintendoLicensedForCgbCompatibilityPalettes()) {
            return 0;
        }
        int checksum = header.getTitleChecksum();
        int checksumIndex = findChecksum(checksum);
        if (checksumIndex < 0) {
            return 0;
        }
        if (checksumIndex < DUPLICATE_CHECKSUM_START) {
            return checksumIndex;
        }
        int duplicateColumn = checksumIndex - DUPLICATE_CHECKSUM_START;
        int fourthLetter = header.getTitleByte(3);
        for (int index = duplicateColumn; index < DUPLICATE_FOURTH_LETTERS.length(); index += DUPLICATE_ROW_LENGTH) {
            if (DUPLICATE_FOURTH_LETTERS.charAt(index) == fourthLetter) {
                int row = (index - duplicateColumn) / DUPLICATE_ROW_LENGTH;
                return checksumIndex + row * DUPLICATE_ROW_LENGTH;
            }
        }
        return 0;
    }

    private static int findChecksum(int checksum) {
        for (int i = 0; i < TITLE_CHECKSUMS.length; i++) {
            if (TITLE_CHECKSUMS[i] == checksum) {
                return i;
            }
        }
        return -1;
    }
}
