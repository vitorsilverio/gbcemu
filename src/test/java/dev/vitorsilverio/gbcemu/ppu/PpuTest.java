package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void disablingWindowResetsCgbWindowLineCounter() {
        Ppu ppu = new Ppu(new Bus());
        ppu.write(0x9C00, (byte) 1);
        ppu.write(0xFF4A, (byte) 0);
        ppu.write(0xFF4B, (byte) 7);
        ppu.write(0xFF40, (byte) 0xF1);

        renderPixel(ppu, 159, 0);
        tick(ppu, 204);
        assertEquals(1, ppu.saveState().windowLineCounter());

        ppu.write(0xFF40, (byte) 0x91);

        assertEquals(0, ppu.saveState().windowLineCounter());
    }

    @Test
    void windowYConditionStaysSetAfterWyChangesLaterInFrame() {
        Ppu ppu = new Ppu(new Bus());
        ppu.write(0xFF4A, (byte) 0);
        ppu.write(0xFF4B, (byte) 7);
        ppu.write(0xFF40, (byte) 0xF1);

        renderPixel(ppu, 159, 0);
        tick(ppu, 204);
        assertTrue(ppu.saveState().windowYCondition());

        ppu.write(0xFF4A, (byte) 16);

        assertTrue(ppu.saveState().windowYCondition());
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
    void nonCgbModeIgnoresCgbObjectPaletteAndVramBankAttributes() {
        Ppu ppu = new Ppu(new Bus(), false);
        ppu.write(0xFF48, (byte) 0xE4);
        setObjPaletteColor(ppu, 0, 1, 0x03E0);
        setObjPaletteColor(ppu, 0, 2, 0x001F);
        setObjPaletteColor(ppu, 7, 2, 0x7C00);
        setTilePixel(ppu, 2, 0, 1);
        ppu.write(0xFF4F, (byte) 1);
        setTilePixel(ppu, 2, 0, 2);
        ppu.write(0xFF4F, (byte) 0);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 8);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0x0F);
        ppu.write(0xFF40, (byte) 0x82);

        renderFirstPixel(ppu);

        assertEquals(0xFF00FF00, getRenderedPixel(ppu, 0, 0));
    }

    @Test
    void cgbModeCanSwitchToDmgCompatibilityAfterBootRom() {
        Ppu ppu = new Ppu(new Bus(), true);
        ppu.write(0xFF48, (byte) 0xE4);
        setObjPaletteColor(ppu, 0, 1, 0x03E0);
        setObjPaletteColor(ppu, 7, 1, 0x001F);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 8);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0x07);
        setTilePixel(ppu, 2, 0, 1);
        ppu.write(0xFF40, (byte) 0x82);

        renderFirstPixel(ppu);
        assertEquals(0xFFFF0000, getRenderedPixel(ppu, 0, 0));

        ppu = new Ppu(new Bus(), true);
        ppu.setCgbMode(false);
        ppu.write(0xFF48, (byte) 0xE4);
        setObjPaletteColor(ppu, 0, 1, 0x03E0);
        setObjPaletteColor(ppu, 7, 1, 0x001F);
        ppu.write(0xFE00, (byte) 16);
        ppu.write(0xFE01, (byte) 8);
        ppu.write(0xFE02, (byte) 2);
        ppu.write(0xFE03, (byte) 0x07);
        setTilePixel(ppu, 2, 0, 1);
        ppu.write(0xFF40, (byte) 0x82);

        renderFirstPixel(ppu);
        assertEquals(0xFF00FF00, getRenderedPixel(ppu, 0, 0));
    }

    @Test
    void debugTileMapUsesCgbPaletteWhenPpuIsInDmgCompatibilityMode() {
        Ppu ppu = new Ppu(new Bus(), true);
        ppu.setCgbMode(false);
        ppu.write(0xFF47, (byte) 0x04);
        setBgPaletteColor(ppu, 1, 0x001F);
        ppu.write(0x9800, (byte) 0);
        setTilePixel(ppu, 256, 0, 1);

        BufferedImage image = ppu.debugTileMapImage(TileMapArea.IN_9800);

        assertEquals(0xFFFF0000, image.getRGB(0, 0));
    }

    @Test
    void appliesCgbCompatibilityPalettesToBackgroundAndObjectPaletteSlots() {
        Ppu ppu = new Ppu(new Bus(), true);
        CgbCompatibilityPaletteSelection selection = new CgbCompatibilityPaletteSelection(
                0,
                0,
                16,
                24,
                116,
                false
        );

        ppu.applyCgbCompatibilityPalettes(selection);
        PpuState state = ppu.saveState();

        assertArrayEquals(
                CgbCompatibilityPaletteColors.littleEndianBytes(116),
                Arrays.copyOfRange(state.bgPalette(), 0, 8)
        );
        assertArrayEquals(
                CgbCompatibilityPaletteColors.littleEndianBytes(16),
                Arrays.copyOfRange(state.objPalette(), 0, 8)
        );
        assertArrayEquals(
                CgbCompatibilityPaletteColors.littleEndianBytes(24),
                Arrays.copyOfRange(state.objPalette(), 8, 16)
        );
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
    void disablingLcdResetsLyAndStopsPpuUntilEnabledAgain() {
        Ppu ppu = new Ppu(new Bus());
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 456 * 145);

        assertEquals(145, ppu.read(0xFF44) & 0xFF);

        ppu.write(0xFF40, (byte) 0x00);

        assertEquals(0, ppu.read(0xFF44) & 0xFF);
        assertEquals(0, ppu.read(0xFF41) & 0x03);

        tick(ppu, 456 * 4);

        assertEquals(0, ppu.read(0xFF44) & 0xFF);

        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 456);

        assertEquals(1, ppu.read(0xFF44) & 0xFF);
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

    @Test
    void cgbOamReadDuringMode2ReturnsFfWithoutChangingOam() {
        Ppu ppu = new Ppu(new Bus(), true);
        writeOamRow(ppu, 2, 0xAAAA, 0xBBBB, 0xCCCC, 0xDDDD);
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 9);

        assertEquals(0xFF, ppu.read(0xFE20) & 0xFF);

        assertOamRow(ppu, 2, 0xAAAA, 0xBBBB, 0xCCCC, 0xDDDD);
    }

    @Test
    void cgbOamWriteDuringMode2IsIgnoredWithoutChangingOam() {
        Ppu ppu = new Ppu(new Bus(), true);
        writeOamRow(ppu, 2, 0xAAAA, 0xBBBB, 0xCCCC, 0xDDDD);
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 9);

        ppu.write(0xFE20, (byte) 0x99);

        assertOamRow(ppu, 2, 0xAAAA, 0xBBBB, 0xCCCC, 0xDDDD);
    }

    @Test
    void cgbOamAccessDuringMode3IsBlockedWithoutChangingOam() {
        Ppu ppu = new Ppu(new Bus(), true);
        writeOamRow(ppu, 2, 0xAAAA, 0xBBBB, 0xCCCC, 0xDDDD);
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 80);

        assertEquals(0xFF, ppu.read(0xFE20) & 0xFF);
        ppu.write(0xFE20, (byte) 0x99);

        assertOamRow(ppu, 2, 0xAAAA, 0xBBBB, 0xCCCC, 0xDDDD);
    }

    @Test
    void cgbVramAccessDuringMode3IsBlockedWithoutChangingVram() {
        Ppu ppu = new Ppu(new Bus(), true);
        ppu.write(0x8000, (byte) 0x12);
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 80);

        assertEquals(0xFF, ppu.read(0x8000) & 0xFF);
        ppu.write(0x8000, (byte) 0x34);

        assertEquals(0x12, ppu.getVideoRam().readBank(0, 0) & 0xFF);
    }

    @Test
    void vbkReadsUnusedBitsAsOneAndSelectedBankInBitZero() {
        Ppu ppu = new Ppu(new Bus(), true);

        assertEquals(0xFE, ppu.read(0xFF4F) & 0xFF);

        ppu.write(0xFF4F, (byte) 0xFF);

        assertEquals(0xFF, ppu.read(0xFF4F) & 0xFF);
    }

    @Test
    void cgbPaletteDataAccessDuringMode3IsBlockedButStillAutoIncrements() {
        Ppu ppu = new Ppu(new Bus(), true);
        setBgPaletteColor(ppu, 0, 0x001F);
        setObjPaletteColor(ppu, 0, 0x03E0);
        ppu.write(0xFF68, (byte) 0x80);
        ppu.write(0xFF6A, (byte) 0x80);
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 80);

        assertEquals(0xFF, ppu.read(0xFF69) & 0xFF);
        assertEquals(0xFF, ppu.read(0xFF6B) & 0xFF);
        ppu.write(0xFF69, (byte) 0x00);
        ppu.write(0xFF6B, (byte) 0x00);

        PpuState state = ppu.saveState();
        assertEquals(0x81, ppu.read(0xFF68) & 0xFF);
        assertEquals(0x81, ppu.read(0xFF6A) & 0xFF);
        assertEquals(0x1F, state.bgPalette()[0] & 0xFF);
        assertEquals(0x00, state.bgPalette()[1] & 0xFF);
        assertEquals(0xE0, state.objPalette()[0] & 0xFF);
        assertEquals(0x03, state.objPalette()[1] & 0xFF);
    }

    @Test
    void hblankDmaRunsOnlyDuringVisibleHblankLines() {
        Ppu ppu = new Ppu(new Bus());
        ppu.write(0xFF40, (byte) 0x80);
        tick(ppu, 80 + 172);

        assertEquals(0, ppu.read(0xFF44) & 0xFF);
        assertEquals(PpuMode.HBLANK.getValue(), ppu.read(0xFF41) & 0x03);
        assertTrue(ppu.canRunHBlankDma());

        tick(ppu, 456 * 144 - 80 - 172);

        assertEquals(144, ppu.read(0xFF44) & 0xFF);
        assertEquals(PpuMode.VBLANK.getValue(), ppu.read(0xFF41) & 0x03);
        assertFalse(ppu.canRunHBlankDma());
    }

    private void setBgPaletteColor(Ppu ppu, int colorIndex, int rgb555) {
        ppu.write(0xFF68, (byte) (colorIndex * 2));
        ppu.write(0xFF69, (byte) (rgb555 & 0xFF));
        ppu.write(0xFF68, (byte) (colorIndex * 2 + 1));
        ppu.write(0xFF69, (byte) ((rgb555 >> 8) & 0xFF));
    }

    private void setObjPaletteColor(Ppu ppu, int colorIndex, int rgb555) {
        setObjPaletteColor(ppu, 0, colorIndex, rgb555);
    }

    private void setObjPaletteColor(Ppu ppu, int paletteIndex, int colorIndex, int rgb555) {
        int address = paletteIndex * 8 + colorIndex * 2;
        ppu.write(0xFF6A, (byte) address);
        ppu.write(0xFF6B, (byte) (rgb555 & 0xFF));
        ppu.write(0xFF6A, (byte) (address + 1));
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

    private void writeOamRow(Ppu ppu, int row, int word0, int word1, int word2, int word3) {
        writeOamWord(ppu, row, 0, word0);
        writeOamWord(ppu, row, 1, word1);
        writeOamWord(ppu, row, 2, word2);
        writeOamWord(ppu, row, 3, word3);
    }

    private void writeOamWord(Ppu ppu, int row, int word, int value) {
        int address = 0xFE00 + row * 8 + word * 2;
        ppu.write(address, (byte) (value & 0xFF));
        ppu.write(address + 1, (byte) ((value >> 8) & 0xFF));
    }

    private void assertOamRow(Ppu ppu, int row, int word0, int word1, int word2, int word3) {
        assertEquals(word0 & 0xFFFF, readOamWord(ppu, row, 0));
        assertEquals(word1 & 0xFFFF, readOamWord(ppu, row, 1));
        assertEquals(word2 & 0xFFFF, readOamWord(ppu, row, 2));
        assertEquals(word3 & 0xFFFF, readOamWord(ppu, row, 3));
    }

    private int readOamWord(Ppu ppu, int row, int word) {
        int address = 0xFE00 + row * 8 + word * 2;
        return (ppu.readOamRaw(address) & 0xFF) | ((ppu.readOamRaw(address + 1) & 0xFF) << 8);
    }

    private int getRenderedPixel(Ppu ppu, int x, int y) {
        return ((BufferedImage) ppu.getFrameBuffer()).getRGB(x, y);
    }
}
