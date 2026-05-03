package dev.vitorsilverio.gbcemu.ppu;

public class CgbPalette {

    private final byte[] paletteData = new byte[64];
    private boolean autoIncrement = false;
    private int currentAddress = 0;

    public CgbPalette() {
        for (int i = 0; i < paletteData.length; i += 2) {
            paletteData[i] = (byte) 0xFF;
            paletteData[i + 1] = 0x7F;
        }
    }

    public int getColor(int paletteIndex, int colorIndex) {
        if (paletteIndex < 0 || paletteIndex > 7) {
            throw new IllegalArgumentException("Palette index must be between 0 and 7");
        }
        if (colorIndex < 0 || colorIndex > 3) {
            throw new IllegalArgumentException("Index must be between 0 and 3");
        }
        var palette = (paletteIndex * 4 + colorIndex);
        int color = (paletteData[palette * 2] & 0xFF) | ((paletteData[palette * 2 + 1] & 0xFF) << 8);
        return toArgb(color);
    }

    private int toArgb(int color) {
        int red = color & 0x1F;
        int green = (color >> 5) & 0x1F;
        int blue = (color >> 10) & 0x1F;

        return 0xFF000000 |
                (expand5To8(red) << 16) |
                (expand5To8(green) << 8) |
                expand5To8(blue);
    }

    private int expand5To8(int value) {
        return (value << 3) | (value >> 2);
    }

    public void setPaletteIndex(byte value) {
        autoIncrement = (value & 0x80) != 0;
        currentAddress = value & 0x3F;
    }

    public byte getPaletteIndex() {
        return (byte) (currentAddress | (autoIncrement ? 0x80 : 0));
    }

    public void setPaletteData(byte value) {
        setPaletteData(value, true);
    }

    public void setPaletteData(byte value, boolean writable) {
        if (writable) {
            paletteData[currentAddress] = value;
        }
        if (autoIncrement) {
            currentAddress = (currentAddress + 1) & 0x3F;
        }
    }

    public byte getPaletteData() {
        return paletteData[currentAddress];
    }

}
