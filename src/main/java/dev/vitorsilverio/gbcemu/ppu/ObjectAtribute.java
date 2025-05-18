package dev.vitorsilverio.gbcemu.ppu;

public class ObjectAtribute {

    private byte y;
    private byte x;
    private byte tileIndex;
    private boolean priority;
    private boolean flipY;
    private boolean flipX;
    private byte dmgPalette;
    private byte bank;
    private byte cgbPalette;

    public byte getY() {
        return y;
    }

    public byte getX() {
        return x;
    }

    public byte getTileIndex() {
        return tileIndex;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setY(byte y) {
        this.y = y;
    }

    public boolean isFlipY() {
        return flipY;
    }

    public void setX(byte x) {
        this.x = x;
    }

    public void setTileIndex(byte tileIndex) {
        this.tileIndex = tileIndex;
    }

    public boolean isFlipX() {
        return flipX;
    }

    public byte getDmgPalette() {
        return dmgPalette;
    }

    public byte getBank() {
        return bank;
    }

    public int getCgbPalette() {
        return cgbPalette;
    }

    public byte getAttributes() {
        return (byte) (((priority ? 1 : 0) << 7) | ((flipY ? 1 : 0) << 6) | ((flipX ? 1 : 0) << 5) | ((dmgPalette & 1) << 4) | ((bank & 1) << 3) | (cgbPalette));
    }

    public void setAttributes(byte value) {
        priority = (value & 0x80) != 0;
        flipY = (value & 0x40) != 0;
        flipX = (value & 0x20) != 0;
        dmgPalette = (byte) ((value & 0x10) != 0 ? 1 : 0);
        bank = (byte) ((value & 0x08) != 0 ? 1 : 0);
        cgbPalette = (byte) (value & 0x07);
    }

    @Override
    public String toString() {
        return "ObjectAtribute{" +
                "y=" + y +
                ", x=" + x +
                ", tileIndex=" + tileIndex +
                ", priority=" + priority +
                ", flipY=" + flipY +
                ", flipX=" + flipX +
                ", dmgPalette=" + dmgPalette +
                ", bank=" + bank +
                ", cgbPalette=" + cgbPalette +
                '}';
    }
}
