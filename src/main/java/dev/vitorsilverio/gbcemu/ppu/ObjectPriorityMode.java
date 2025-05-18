package dev.vitorsilverio.gbcemu.ppu;

public enum ObjectPriorityMode {
    CGB((byte) 0),
    DMG((byte) 1);

    private final byte value;

    ObjectPriorityMode(byte value) {
        this.value = value;
    }

    public static ObjectPriorityMode fromValue(byte i) {
        for (ObjectPriorityMode mode : ObjectPriorityMode.values()) {
            if (mode.value == i) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid Object Priority Mode value: " + i);
    }

    public byte getValue() {
        return value;
    }
}
