package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PpuTest {

    @Test
    void statIncludesModeWhenLcdIsEnabled() {
        Ppu ppu = new Ppu(new Bus());

        ppu.write(0xFF40, (byte) 0x80);

        assertEquals(PpuMode.OAM_READ.getValue(), ppu.read(0xFF41) & 0x03);
    }

    @Test
    void scrollRegistersAreUnsigned() {
        Ppu ppu = new Ppu(new Bus());

        ppu.write(0xFF42, (byte) 0xFE);
        ppu.write(0xFF43, (byte) 0x80);

        assertEquals(0xFE, ppu.read(0xFF42) & 0xFF);
        assertEquals(0x80, ppu.read(0xFF43) & 0xFF);
    }

    @Test
    void windowRendersOverBackgroundWhenEnabled() {
        Ppu ppu = new Ppu(new Bus());
        setBgPaletteColor(ppu, 1, 0x001F);
        setBgPaletteColor(ppu, 2, 0x7C00);
        setTilePixel(ppu, 0, 0, 1);
        setTilePixel(ppu, 1, 0, 2);
        ppu.write(0x9800, (byte) 0);
        ppu.write(0x9C00, (byte) 1);
        ppu.write(0xFF4A, (byte) 0);
        ppu.write(0xFF4B, (byte) 7);
        ppu.write(0xFF40, (byte) 0xF1);

        renderFirstPixel(ppu);

        assertEquals(0xFF0000FF, getRenderedPixel(ppu, 0, 0));
    }

    @Test
    void spriteRendersOverBackgroundAndTreatsColorZeroAsTransparent() {
        Ppu ppu = new Ppu(new Bus());
        setBgPaletteColor(ppu, 1, 0x001F);
        setObjPaletteColor(ppu, 1, 0x03E0);
        setTilePixel(ppu, 0, 0, 1);
        setTilePixel(ppu, 2, 0, 1);
        ppu.write(0x9800, (byte) 0);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 8);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0);
        ppu.write(0xFF40, (byte) 0x93);

        renderFirstPixel(ppu);

        assertEquals(0xFF00FF00, getRenderedPixel(ppu, 0, 0));
    }

    @Test
    void spritePriorityKeepsSpriteBehindNonZeroBackground() {
        Ppu ppu = new Ppu(new Bus());
        setBgPaletteColor(ppu, 1, 0x001F);
        setObjPaletteColor(ppu, 1, 0x03E0);
        setTilePixel(ppu, 0, 0, 1);
        setTilePixel(ppu, 2, 0, 1);
        ppu.write(0x9800, (byte) 0);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 8);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0x80);
        ppu.write(0xFF40, (byte) 0x93);

        renderFirstPixel(ppu);

        assertEquals(0xFFFF0000, getRenderedPixel(ppu, 0, 0));
    }

    private void setBgPaletteColor(Ppu ppu, int colorIndex, int rgb555) {
        ppu.write(0xFF68, (byte) (colorIndex * 2));
        ppu.write(0xFF69, (byte) (rgb555 & 0xFF));
        ppu.write(0xFF68, (byte) (colorIndex * 2 + 1));
        ppu.write(0xFF69, (byte) ((rgb555 >> 8) & 0xFF));
    }

    private void setObjPaletteColor(Ppu ppu, int colorIndex, int rgb555) {
        ppu.write(0xFF6A, (byte) (colorIndex * 2));
        ppu.write(0xFF6B, (byte) (rgb555 & 0xFF));
        ppu.write(0xFF6A, (byte) (colorIndex * 2 + 1));
        ppu.write(0xFF6B, (byte) ((rgb555 >> 8) & 0xFF));
    }

    private void setTilePixel(Ppu ppu, int tileIndex, int y, int colorIndex) {
        int address = 0x8000 + tileIndex * 16 + y * 2;
        ppu.write(address, (byte) ((colorIndex & 0x01) << 7));
        ppu.write(address + 1, (byte) (((colorIndex >> 1) & 0x01) << 7));
    }

    private void renderFirstPixel(Ppu ppu) {
        for (int i = 0; i < 95; i++) {
            ppu.tick();
        }
    }

    private int getRenderedPixel(Ppu ppu, int x, int y) {
        return ((BufferedImage) ppu.getFrameBuffer()).getRGB(x, y);
    }
}
