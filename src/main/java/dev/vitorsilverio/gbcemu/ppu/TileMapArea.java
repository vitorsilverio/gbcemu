package dev.vitorsilverio.gbcemu.ppu;

public enum TileMapArea {
    IN_9800(0, 0x9800),
    IN_9C00(0x1, 0x9C00);

    private final int value;
    private final int address;

    TileMapArea(int value, int address) {
        this.value = value;
        this.address = address;
    }

    public int getValue() {
        return value;
    }

    public int getAddress() {
        return address;
    }

    public static TileMapArea fromValue(int value) {
        for (TileMapArea area : TileMapArea.values()) {
            if (area.value == value) {
                return area;
            }
        }
        throw new IllegalArgumentException("Invalid Window Tile Area value: " + value);
    }
}
