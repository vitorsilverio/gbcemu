package dev.vitorsilverio.gbcemu.ppu;

public class CgbPalette {

    private final byte[] paletteData = new byte[64];
    private boolean autoIncrement = false;
    private int currentAddress = 0;

    public CgbPalette() {
        // Initialize the palette data with default values
        for (int i = 0; i < paletteData.length / 8; i += 8) {
            paletteData[i] = (byte) 0b1111_1111;
            paletteData[i+1] = (byte) 0b1111_1111;
            paletteData[i+2] = (byte) 0b1110_1110;
            paletteData[i+3] = (byte) 0b0111_1011;
            paletteData[i+4] = (byte) 0b1110_0011;
            paletteData[i+5] = (byte) 0b0001_1100;
            paletteData[i+6] = 0;
            paletteData[i+7] = 0;
        }
    }

    public int getColor(int paletteIndex, int colorIndex) {
        if (paletteIndex < 0 || paletteIndex > 8) {
            throw new IllegalArgumentException("Palette index must be between 0 and 7");
        }
        if (colorIndex < 0 || colorIndex > 3) {
            throw new IllegalArgumentException("Index must be between 0 and 3");
        }
        return paletteData[paletteIndex * 4 + colorIndex] | ( paletteData[paletteIndex * 4 + colorIndex + 1] << 8);
    }

    public void setPaletteIndex(byte value) {
        autoIncrement = (value & 0x80) != 0;
        currentAddress = value & 0x3F;
    }

    public byte getPaletteIndex() {
        return (byte) (currentAddress | (autoIncrement ? 0x80 : 0));
    }

    public void setPaletteData(byte value) {
        paletteData[currentAddress] = value;
        if (autoIncrement) {
            currentAddress = (currentAddress + 1) & 0x3F;
        }
    }

    public byte getPaletteData() {
        return paletteData[currentAddress];
    }

}
