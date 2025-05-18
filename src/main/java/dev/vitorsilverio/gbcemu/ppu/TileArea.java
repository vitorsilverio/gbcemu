package dev.vitorsilverio.gbcemu.ppu;

public enum TileArea {
    METHOD_8000(1, 0x8000, false),
    METHOD_8800(0, 0x9000, true);

    private final int value;
    private final int baseAddress;
    private final boolean isSigned;

    TileArea(int value, int baseAddress, boolean isSigned) {
        this.value = value;
        this.baseAddress = baseAddress;
        this.isSigned = isSigned;
    }

    public int getValue() {
        return value;
    }

    public int getBaseAddress() {
        return baseAddress;
    }

    public boolean isSigned() {
        return isSigned;
    }

    public static TileArea fromValue(int value) {
        for (TileArea mode : TileArea.values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid Addressing Mode value: " + value);
    }
}
