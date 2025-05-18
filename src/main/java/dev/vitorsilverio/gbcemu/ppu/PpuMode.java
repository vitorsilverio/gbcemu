package dev.vitorsilverio.gbcemu.ppu;

public enum PpuMode {
    HBLANK(0),
    VBLANK(1),
    OAM_READ(2),
    VRAM_READ(3);

    private final int value;

    PpuMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PpuMode fromValue(int value) {
        for (PpuMode mode : PpuMode.values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid PPU mode value: " + value);
    }
}
