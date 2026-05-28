package dev.vitorsilverio.gbcemu.ppu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
public class Tile implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Tile.class);

    private final byte[] data;
    private final byte[] pixels;

    public Tile() {
        this.data = new byte[16];
        this.pixels = new byte[64];
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= 8 || y < 0 || y >= 8) {
            logger.error("Coordinates out of bounds: x={}, y={}", x, y);
            throw new IllegalArgumentException("Coordinates out of bounds");
        }
        return getPixelUnchecked(x, y);
    }

    public int getPixelUnchecked(int x, int y) {
        return pixels[(y << 3) | x] & 0x03;
    }

    public void setData(int index, byte value) {
        if (index < 0 || index >= data.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        data[index] = value;
        refreshPixelRow(index >> 1);
    }

    public byte getData(int index) {
        if (index < 0 || index >= data.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return data[index];
    }

    private void refreshPixelRow(int y) {
        int byteIndex = y << 1;
        int low = data[byteIndex] & 0xFF;
        int high = data[byteIndex + 1] & 0xFF;
        int pixelOffset = y << 3;
        for (int x = 0; x < 8; x++) {
            int bitIndex = 7 - x;
            int lowBit = (low >> bitIndex) & 1;
            int highBit = (high >> bitIndex) & 1;
            pixels[pixelOffset + x] = (byte) (lowBit | (highBit << 1));
        }
    }

    @Override
    public String toString() {
        String[] map = {"█", "▓", "▒", "░"};
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int color = getPixel(x, y);
                sb.append(map[color]);
                sb.append(map[color]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }


}
