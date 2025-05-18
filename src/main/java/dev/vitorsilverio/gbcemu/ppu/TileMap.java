package dev.vitorsilverio.gbcemu.ppu;

public class TileMap {
    private byte index;

    private boolean priority;
    private boolean flipY;
    private boolean flipX;
    private int notUsed;
    private int bank;
    private int paletteIndex;

    public void setIndex(byte index) {
        this.index = index;
    }

    public byte getIndex() {
        return index;
    }

    public void setAttributes(byte value) {
        this.priority = (value & 0x80) != 0;
        this.flipY = (value & 0x40) != 0;
        this.flipX = (value & 0x20) != 0;
        this.notUsed = (value & 0x10) >> 4;
        this.bank = (value & 0x08) >> 3;
        this.paletteIndex = value & 0x07;
    }

    public byte getAttributes() {
        byte value = 0;
        if (priority) {
            value |= (byte) 0x80;
        }
        if (flipY) {
            value |= (byte) 0x40;
        }
        if (flipX) {
            value |= (byte) 0x20;
        }
        value |= (byte) (notUsed << 4);
        value |= (byte) (bank << 3);
        value |= (byte) paletteIndex;
        return value;
    }

    public boolean isPriority() {
        return priority;
    }

    public boolean isFlipY() {
        return flipY;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public int getBank() {
        return bank;
    }

    public int getPaletteIndex() {
        return paletteIndex;
    }

    @Override
    public String toString() {
        return "TileMap{" +
                "index=" + index +
                ", priority=" + priority +
                ", flipY=" + flipY +
                ", flipX=" + flipX +
                ", notUsed=" + notUsed +
                ", bank=" + bank +
                ", paletteIndex=" + paletteIndex +
                '}';
    }
}
