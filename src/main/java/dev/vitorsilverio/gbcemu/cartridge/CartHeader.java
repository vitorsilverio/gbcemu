package dev.vitorsilverio.gbcemu.cartridge;

public class CartHeader {

    private final Integer entryPoint;
    private final byte[] nintendoLogo;
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
    private final String versionNumber;
    private final String headerChecksum;
    private final String globalChecksum;


    public CartHeader(byte[] rom) {
        entryPoint = ((rom[0x0104] & 0xFF) << 8) | (rom[0x0105] & 0xFF);
        nintendoLogo = new byte[0x3f];
        System.arraycopy(rom, 262, nintendoLogo, 0, nintendoLogo.length);
        title = new String(rom, 0x0134, 16);
        manufacturerCode = new String(rom, 0x013F, 4);
        cgbFlag = rom[0x0143];
        newLicenseeCode = new String(rom, 0x0144, 2);
        sgbFlag = new String(rom, 0x0146, 1);
        cartridgeType = CartridgeType.fromCode(rom[0x0147]);
        romSize = (1 << rom[0x0148]) * 32 * 1024;
        ramSize = rom[0x0149];
        destinationCode = new String(rom, 0x014A, 1);
        oldLicenseeCode = new String(rom, 0x014B, 2);
        versionNumber = new String(rom, 0x014C, 1);
        headerChecksum = new String(rom, 0x014D, 1);
        globalChecksum = new String(rom, 0x014E, 2);
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
