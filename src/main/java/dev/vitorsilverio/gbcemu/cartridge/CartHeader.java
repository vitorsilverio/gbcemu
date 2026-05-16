package dev.vitorsilverio.gbcemu.cartridge;

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
        entryPoint = ((rom[0x0104] & 0xFF) << 8) | (rom[0x0105] & 0xFF);
        nintendoLogo = new byte[0x3f];
        System.arraycopy(rom, 262, nintendoLogo, 0, nintendoLogo.length);
        titleBytes = new byte[16];
        System.arraycopy(rom, 0x0134, titleBytes, 0, titleBytes.length);
        title = new String(rom, 0x0134, 16);
        manufacturerCode = new String(rom, 0x013F, 4);
        cgbFlag = rom[0x0143];
        newLicenseeCode = new String(rom, 0x0144, 2);
        sgbFlag = new String(rom, 0x0146, 1);
        cartridgeType = CartridgeType.fromCode(rom[0x0147]);
        romSize = (1 << rom[0x0148]) * 32 * 1024;
        ramSize = rom[0x0149];
        destinationCode = new String(rom, 0x014A, 1);
        oldLicenseeCodeValue = rom[0x014B] & 0xFF;
        oldLicenseeCode = new String(rom, 0x014B, 1);
        versionNumber = new String(rom, 0x014C, 1);
        headerChecksum = new String(rom, 0x014D, 1);
        globalChecksum = new String(rom, 0x014E, 2);
    }

    public CartridgeType getCartridgeType() {
        return cartridgeType;
    }

    public String getTitle() {
        return title.strip();
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

    public boolean isNintendoLicensedForCgbCompatibilityPalettes() {
        if (oldLicenseeCodeValue == 0x33) {
            return "01".equals(newLicenseeCode);
        }
        return oldLicenseeCodeValue == 0x01;
    }

    public int getRamSizeBytes() {
        return switch (ramSize & 0xFF) {
            case 0x00 -> 0;
            case 0x01 -> 2 * 1024;
            case 0x02 -> 8 * 1024;
            case 0x03 -> 32 * 1024;
            case 0x04 -> 128 * 1024;
            case 0x05 -> 64 * 1024;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return String.format("""
                entryPoint: %04X
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
