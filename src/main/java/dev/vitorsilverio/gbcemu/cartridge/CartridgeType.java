package dev.vitorsilverio.gbcemu.cartridge;

public enum CartridgeType {
    ROM_ONLY(0x00),
    MBC1(0x01),
    MBC1_RAM(0x02),
    MBC1_RAM_BATTERY(0x03),
    MBC2(0x05),
    MBC2_BATTERY(0x06),
    ROM_RAM(0x08),
    ROM_RAM_BATTERY(0x09),
    MMM01(0x0B),
    MMM01_RAM(0x0C),
    MMM01_RAM_BATTERY(0x0D),
    MBC3_TIMER_BATTERY(0x0F),
    MBC3(0x10),
    MBC3_RAM(0x11),
    MBC3_RAM_BATTERY(0x12),
    MBC5(0x19),
    MBC5_RAM(0x1A),
    MBC5_RAM_BATTERY(0x1B),
    MBC5_RUMBLE(0x1C),
    MBC5_RUMBLE_RAM(0x1D),
    MBC5_RUMBLE_RAM_BATTERY(0x1E),
    MBC6(0x20),
    MBC7_SENSOR_RUMBLE_RAM_BATTERY(0x22),
    POCKET_CAMERA(0xFC),
    BANDAI_TAMA5(0xFD),
    HuC3(0xFE),
    HuC1_RAM_BATTERY(0xFF);

    private final int code;

    CartridgeType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CartridgeType fromCode(int code) {
        for (CartridgeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown cartridge type: " + code);
    }
}
