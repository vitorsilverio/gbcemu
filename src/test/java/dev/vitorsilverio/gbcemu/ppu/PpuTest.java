package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
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

    @Test
    void cgbObjectPriorityDefaultsToOamOrder() {
        Ppu ppu = new Ppu(new Bus());
        setObjPaletteColor(ppu, 1, 0x001F);
        setObjPaletteColor(ppu, 2, 0x03E0);
        setTilePixel(ppu, 2, 0, 1);
        setTilePixel(ppu, 3, 1, 0, 2);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 16);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0);
        ppu.write(0xFE04, (byte) 16);
        ppu.write(0xFE05, (byte) 15);
        ppu.write(0xFE06, (byte) 3);
        ppu.write(0xFE07, (byte) 0);
        ppu.write(0xFF40, (byte) 0x82);

        renderPixel(ppu, 8, 0);

        assertEquals(0xFFFF0000, getRenderedPixel(ppu, 8, 0));
    }

    @Test
    void dmgObjectPriorityCanPreferLowerXCoordinate() {
        Ppu ppu = new Ppu(new Bus());
        setObjPaletteColor(ppu, 1, 0x001F);
        setObjPaletteColor(ppu, 2, 0x03E0);
        setTilePixel(ppu, 2, 0, 1);
        setTilePixel(ppu, 3, 1, 0, 2);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 16);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0);
        ppu.write(0xFE04, (byte) 16);
        ppu.write(0xFE05, (byte) 15);
        ppu.write(0xFE06, (byte) 3);
        ppu.write(0xFE07, (byte) 0);
        ppu.write(0xFF6C, (byte) 1);
        ppu.write(0xFF40, (byte) 0x82);

        renderPixel(ppu, 8, 0);

        assertEquals(0xFF00FF00, getRenderedPixel(ppu, 8, 0));
    }

    @Test
    void scanlineTakesExactly456Dots() {
        Ppu ppu = new Ppu(new Bus());
        ppu.write(0xFF40, (byte) 0x80);

        tick(ppu, 455);

        assertEquals(0, ppu.read(0xFF44) & 0xFF);

        ppu.tick();

        assertEquals(1, ppu.read(0xFF44) & 0xFF);
    }

    @Test
    void frameTakesExactly70224Dots() {
        Ppu ppu = new Ppu(new Bus());
        ppu.write(0xFF40, (byte) 0x80);

        tick(ppu, 70223);

        assertEquals(153, ppu.read(0xFF44) & 0xFF);

        ppu.tick();

        assertEquals(0, ppu.read(0xFF44) & 0xFF);
    }

    @Test
    void vblankInterruptIsRequestedOnlyWhenEnteringVblank() {
        Bus bus = new Bus();
        Ppu ppu = new Ppu(bus);
        ppu.write(0xFF40, (byte) 0x80);

        tick(ppu, 456 * 144);

        assertEquals(Interrupt.VBLANK.getMask(), bus.read(0xFF0F) & Interrupt.VBLANK.getMask());
        bus.clearInterrupt(Interrupt.VBLANK);

        tick(ppu, 456 * 9);

        assertEquals(0, bus.read(0xFF0F) & Interrupt.VBLANK.getMask());
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
        setTilePixel(ppu, tileIndex, 0, y, colorIndex);
    }

    private void setTilePixel(Ppu ppu, int tileIndex, int x, int y, int colorIndex) {
        int address = 0x8000 + tileIndex * 16 + y * 2;
        int bit = 7 - x;
        ppu.write(address, (byte) ((colorIndex & 0x01) << bit));
        ppu.write(address + 1, (byte) (((colorIndex >> 1) & 0x01) << bit));
    }

    private void renderFirstPixel(Ppu ppu) {
        for (int i = 0; i < 95; i++) {
            ppu.tick();
        }
    }

    private void renderPixel(Ppu ppu, int x, int y) {
        tick(ppu, 80 + 12 + y * 456 + x + 1);
    }

    private void tick(Ppu ppu, int ticks) {
        for (int i = 0; i < ticks; i++) {
            ppu.tick();
        }
    }

    private int getRenderedPixel(Ppu ppu, int x, int y) {
        return ((BufferedImage) ppu.getFrameBuffer()).getRGB(x, y);
    }
}
