package dev.vitorsilverio.gbcemu.ppu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Map;

public class Tile extends BufferedImage {

    private static final Logger logger = LoggerFactory.getLogger(Tile.class);

    private final byte[] data;

    private static final int WHITE = 0xffffff;
    private static final int BLACK = 0x000000;
    private static final int LIGHT_GRAY = 0xc0c0c0;
    private static final int DARK_GRAY = 0x808080;


    public Tile() {
        super(8, 8, BufferedImage.TYPE_INT_RGB);
        this.data = new byte[16];
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= 8 || y < 0 || y >= 8) {
            logger.error("Coordinates out of bounds: x={}, y={}", x, y);
            throw new IllegalArgumentException("Coordinates out of bounds");
        }
        int byteIndex = y * 2;
        int bitIndex = 7 - x;
        int color1 = (data[byteIndex] >> bitIndex) & 1;
        int color2 = (data[byteIndex + 1] >> bitIndex) & 1;
        return (color1 << 1) | color2;
    }

    public void setData(int index, byte value) {
        if (index < 0 || index >= data.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        data[index] = value;
        for(int x = 0; x < 8; x++) {
            int y = index / 2;
            this.setRGB(x, y, switch (getPixel(x, y)) {
                case 0 -> WHITE;
                case 1 -> LIGHT_GRAY;
                case 2 -> DARK_GRAY;
                case 3 -> BLACK;
                default -> throw new IllegalArgumentException("Invalid color value");
            });
        }
    }

    public byte getData(int index) {
        if (index < 0 || index >= data.length) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return data[index];
    }

    @Override
    public String toString() {
        var map = Map.of(
                0, "█",
                1, "▓",
                2, "▒",
                3, "░"
        );
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int color = getPixel(x, y);
                sb.append(map.get(color));
                sb.append(map.get(color));
            }
            sb.append("\n");
        }
        return sb.toString();
    }


}
