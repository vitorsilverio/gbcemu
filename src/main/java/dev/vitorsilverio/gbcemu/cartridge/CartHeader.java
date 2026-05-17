package dev.vitorsilverio.gbcemu.cartridge;

import java.nio.charset.StandardCharsets;

public class CartHeader {

    private final Integer entryPoint;
    private final byte[] nintendoLogo;
    private final byte[] titleBytes;
    private final String title;
    private final String manufacturerCode;
    private final byte cgbFlag;
    private final String newLicenseeCode;
    private final String sgbFlag;
    private final CartridgeType cartridgeType;
    private final int romSize;
    private final int ramSize;
    private final String destinationCode;
    private final String oldLicenseeCode;
    private final int oldLicenseeCodeValue;
    private final String versionNumber;
    private final String headerChecksum;
    private final String globalChecksum;


    public CartHeader(byte[] rom) {
        entryPoint = ((rom[0x0100] & 0xFF) << 24)
                | ((rom[0x0101] & 0xFF) << 16)
                | ((rom[0x0102] & 0xFF) << 8)
                | (rom[0x0103] & 0xFF);
        nintendoLogo = new byte[0x30];
        System.arraycopy(rom, 0x0104, nintendoLogo, 0, nintendoLogo.length);
        titleBytes = new byte[16];
        System.arraycopy(rom, 0x0134, titleBytes, 0, titleBytes.length);
        cgbFlag = rom[0x0143];
        title = new String(rom, 0x0134, titleLength(), StandardCharsets.ISO_8859_1);
        manufacturerCode = new String(rom, 0x013F, 4, StandardCharsets.ISO_8859_1);
        newLicenseeCode = new String(rom, 0x0144, 2, StandardCharsets.ISO_8859_1);
        sgbFlag = new String(rom, 0x0146, 1, StandardCharsets.ISO_8859_1);
        cartridgeType = CartridgeType.fromCode(rom[0x0147]);
        romSize = decodeRomSize(rom[0x0148] & 0xFF);
        ramSize = rom[0x0149] & 0xFF;
        destinationCode = new String(rom, 0x014A, 1, StandardCharsets.ISO_8859_1);
        oldLicenseeCodeValue = rom[0x014B] & 0xFF;
        oldLicenseeCode = new String(rom, 0x014B, 1, StandardCharsets.ISO_8859_1);
        versionNumber = new String(rom, 0x014C, 1, StandardCharsets.ISO_8859_1);
        headerChecksum = new String(rom, 0x014D, 1, StandardCharsets.ISO_8859_1);
        globalChecksum = new String(rom, 0x014E, 2, StandardCharsets.ISO_8859_1);
    }

    private int titleLength() {
        return isCgbCompatible() ? 15 : 16;
    }

    public CartridgeType getCartridgeType() {
        return cartridgeType;
    }

    public int getEntryPoint() {
        return entryPoint;
    }

    public byte[] getNintendoLogo() {
        return nintendoLogo.clone();
    }

    public String getTitle() {
        int end = title.length();
        while (end > 0 && (title.charAt(end - 1) == '\0' || Character.isWhitespace(title.charAt(end - 1)))) {
            end--;
        }
        return title.substring(0, end);
    }

    public boolean isCgbCompatible() {
        return cgbFlag == (byte) 0x80 || cgbFlag == (byte) 0xC0;
    }

    public byte getCgbFlag() {
        return cgbFlag;
    }

    public int getOldLicenseeCode() {
        return oldLicenseeCodeValue;
    }

    public String getNewLicenseeCode() {
        return newLicenseeCode;
    }

    public int getTitleChecksum() {
        int checksum = 0;
        for (byte titleByte : titleBytes) {
            checksum = (checksum + (titleByte & 0xFF)) & 0xFF;
        }
        return checksum;
    }

    public int getTitleByte(int index) {
        if (index < 0 || index >= titleBytes.length) {
            throw new IllegalArgumentException("Title byte index must be between 0 and 15");
        }
        return titleBytes[index] & 0xFF;
    }

    public boolean isNintendoLicensedForCgbCompatibilityPalettes() {
        if (oldLicenseeCodeValue == 0x33) {
            return "01".equals(newLicenseeCode);
        }
        return oldLicenseeCodeValue == 0x01;
    }

    public int getRamSizeBytes() {
        return switch (ramSize) {
            case 0x00 -> 0;
            case 0x01 -> 2 * 1024;
            case 0x02 -> 8 * 1024;
            case 0x03 -> 32 * 1024;
            case 0x04 -> 128 * 1024;
            case 0x05 -> 64 * 1024;
            default -> 0;
        };
    }

    public int getRomSizeBytes() {
        return romSize;
    }

    public int getRamSizeCode() {
        return ramSize;
    }

    private int decodeRomSize(int code) {
        return switch (code) {
            case 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 -> (32 * 1024) << code;
            case 0x52 -> 72 * 16 * 1024;
            case 0x53 -> 80 * 16 * 1024;
            case 0x54 -> 96 * 16 * 1024;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return String.format("""
                entryPoint: %08X
                nintendoLogo: %s
                title: %s
                manufacturerCode: %s
                cgbFlag: %02X
                newLicenseeCode: %s
                sgbFlag: %s
                cartridgeType: %s
                romSize: %d
                ramSize: %d
                destinationCode: %s
                oldLicenseeCode: %s
                versionNumber: %s
                headerChecksum: %s
                globalChecksum: %s
                """,
                entryPoint,
                new String(nintendoLogo),
                title,
                manufacturerCode,
                cgbFlag,
                newLicenseeCode,
                sgbFlag,
                cartridgeType.name(),
                romSize,
                ramSize,
                destinationCode,
                oldLicenseeCode,
                versionNumber,
                headerChecksum,
                globalChecksum);
    }
}
