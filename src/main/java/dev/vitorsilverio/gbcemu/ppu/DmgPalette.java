package dev.vitorsilverio.gbcemu.ppu;

public class DmgPalette {

    private final byte[] colors = new byte[4];

    public void setData(byte value) {
        for (int i = 0; i < 4; i++) {
            colors[i] = (byte) ((value >> (i * 2)) & 0x03);
        }
    }
    public byte getColor(int index) {
        if (index < 0 || index >= colors.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return colors[index];
    }

    public byte getData() {
        byte value = 0;
        for (int i = 0; i < 4; i++) {
            value |= (byte) ((colors[i] & 0x03) << (i * 2));
        }
        return value;
    }

}
