package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Ppu implements MemorySpace, MachineCycle {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Ppu.class);

    private static final int OAM_SCANLINE_CYCLES = 80;
    private static final int SCANLINE_CYCLES = 376;
    private static final int VBLANK_CYCLES = 456;

    private final int LCDC = 0xFF40;
    private final int STAT = 0xFF41;
    private final int SCY = 0xFF42;
    private final int SCX = 0xFF43;
    private final int LY = 0xFF44;
    private final int LYC = 0xFF45;

    private final int BGP = 0xFF47;
    private final int OBP0 = 0xFF48;
    private final int OBP1 = 0xFF49;
    private final int WY = 0xFF4A;
    private final int WX = 0xFF4B;
    private final int VBK = 0xFF4F;
    private final int BGPI = 0xFF68;
    private final int BGPD = 0xFF69;
    private final int OBPI = 0xFF6A;
    private final int OBPD = 0xFF6B;
    private final int OPRI = 0xFF6C;

    private final List<Integer> REGISTERS = List.of(
            LCDC, STAT, SCY, SCX, LY, LYC,
            BGP, OBP0, OBP1, WY, WX, VBK,
            BGPI, BGPD, OBPI, OBPD, OPRI
    );

    private final PpuControl control = new PpuControl();
    private final VideoRam videoRam = new VideoRam();
    private final OamRAM oam = new OamRAM();
    private final Bus bus;
    private final CgbPalette bgPalette = new CgbPalette();
    private final CgbPalette objPalette = new CgbPalette();
    private final DmgPalette bgPaletteDmg = new DmgPalette();
    private final DmgPalette obj0PaletteDmg = new DmgPalette();
    private final DmgPalette obj1PaletteDmg = new DmgPalette();
    private final Stat stat = new Stat();
    private final int[][] frameBuffer; // 160x144 pixels

    private int cycles;
    private PpuMode mode = PpuMode.OAM_READ;
    private int currentLine;
    private int currentColumn;
    private int scrollX;
    private int scrollY;
    private int penaltyDelay = 0;
    private ObjectPriorityMode objectPriorityMode = ObjectPriorityMode.DMG;
    private byte lineCompare = 0;
    private int windowX;
    private int windowY;




    public Ppu(Bus bus) {
        this.bus = bus;
        this.frameBuffer = new int[160][144];
    }


    @Override
    public void tick() {
        if (!control.isEnabled()) {
            return;
        }

        logger.trace("Ppu tick: mode={}, line={}, column={}, cycles={}", mode, currentLine, currentColumn, cycles);

       statInterrupt();

        // Execute logic based on the current mode
        switch (mode) {
            case OAM_READ:
                execOAMRead();
                break;
            case VRAM_READ:
                execVRAMRead();
                break;
            case HBLANK:
                execHBlank();
                break;
            case VBLANK:
                execVBlank();
                break;
        }
        cycles++;
    }

    private void statInterrupt() {
        if (stat.isLycInterrupt() && currentLine == lineCompare) {
            bus.requestInterrupt(Interrupt.LCD_STAT);
        }
        if (stat.isOamInterrupt() && mode == PpuMode.OAM_READ) {
            bus.requestInterrupt(Interrupt.LCD_STAT);
        }
        if (stat.isVBlankInterrupt() && mode == PpuMode.VBLANK) {
            bus.requestInterrupt(Interrupt.LCD_STAT);
        }
        if (stat.isHBlankInterrupt() && mode == PpuMode.HBLANK) {
            bus.requestInterrupt(Interrupt.LCD_STAT);
        }
    }

    private void execVRAMRead() {
        // wait penalty
        if (penaltyDelay > 0) {
            penaltyDelay--;
            return;
        }

        // Draw current line
        if ( cycles < 12 ){
            currentColumn = 0;
            // The 12 extra dots of penalty come from two tile fetches at the beginning of Mode 3. One is the first tile in the scanline (the one that gets shifted by SCX % 8 pixels), the other is simply discarded.
            return;
        }
        // Include penalty delay for scrollX
        if ( cycles == 12) {
            penaltyDelay = scrollX % 8;
            return;
        }
        // Fetch the tile data
        int indexY = (((currentLine + scrollY) / 8 ) % 32);
        int indexX = (((currentColumn + scrollX) / 8 ) % 32);
        int index = ((indexY * 32) + indexX) & 0x3ff;
        TileMap map = videoRam.getTileMap(control.getBgTileArea(), index);
        Tile tile = videoRam.getTile(control.getTileArea(), map.getBank(), map.getIndex());

        var tileX = ((currentColumn + scrollX) % 8) & 0x7;
        var tileY = ((currentLine + scrollY) % 8) & 0x7;
        if (map.isFlipX()) {
            tileX = 7 - tileX;
        }
        if (map.isFlipY()) {
            tileY = 7 - tileY;
        }
        // Draw the current tile (background)
        frameBuffer[currentColumn][currentLine] = bgPalette.getColor(map.getPaletteIndex(), tile.getPixel(tileX, tileY));

        // TODO: Draw window
        // TODO: Draw sprites

        currentColumn++;
        // when line finished then go to HBLANK
        if (currentColumn == 159) {
            // End of the scanline
            mode = PpuMode.HBLANK;
        }

    }

    private void execOAMRead() {
        // Read from OAM

        if (cycles == OAM_SCANLINE_CYCLES -1) {
            mode = PpuMode.VRAM_READ;
            cycles = 0;
        }
    }

    private void execHBlank() {
        // Execute HBlank
        if (cycles == SCANLINE_CYCLES -1) {
            currentLine++;
            cycles = 0;
            if (currentLine == 144) {
                mode = PpuMode.VBLANK;
                bus.requestInterrupt(Interrupt.VBLANK);
            } else {
                mode = PpuMode.OAM_READ;
            }
        }
    }

    private void execVBlank() {
        // Execute VBlank
        int line = currentLine;
        currentLine = 144 + ((cycles / VBLANK_CYCLES));
        if (line != currentLine) {
            bus.requestInterrupt(Interrupt.VBLANK);
        }
        if (cycles == (VBLANK_CYCLES * 10) - 1) {
            currentLine = 0;
            mode = PpuMode.OAM_READ;
            cycles = 0;
        }
    }

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address) ||
                videoRam.contains(address) ||
                oam.contains(address) ||
                (address >= 0xfea0 && address <= 0xfeff); // OAM + Prohibited area
    }

    @Override
    public byte read(int address) {
        if (videoRam.contains(address)) {
            return videoRam.read(address);
        }

        switch (address) {
            case LCDC:
                return control.getData();
            case SCY:
                return (byte) scrollY;
            case SCX:
                return (byte) scrollX;
            case LY:
                return (byte) currentLine;
            case LYC:
                return lineCompare;
            case BGPI:
                return bgPalette.getPaletteIndex();
            case BGPD:
                if (PpuMode.VRAM_READ.equals(mode)) {
                    // None can be read in this mode
                    return (byte)0xff;
                }
                return bgPalette.getPaletteData();
            case OBPI:
                return objPalette.getPaletteIndex();
            case OBPD:
                if (PpuMode.VRAM_READ.equals(mode)) {
                    // None can be read in this mode
                    return (byte)0xff;
                }
                return objPalette.getPaletteData();
            case OPRI:
                return objectPriorityMode.getValue();
            case BGP:
                return bgPaletteDmg.getData();
            case OBP0:
                return obj0PaletteDmg.getData();
            case OBP1:
                return obj1PaletteDmg.getData();
            case WX:
                return (byte) windowX;
            case WY:
                return (byte) windowY;
            case STAT:
                return (byte) (stat.getData() |
                        (control.isEnabled() ? 0: mode.getValue() & 0x3 ) |
                        (currentLine == lineCompare ? 0x04 : 0));
        }

        if (0xfea0 <= address && address <= 0xfeff) { // Prohibited area CGB rev 5+
            if (PpuMode.OAM_READ.equals(mode)) {
                // None can be read in this mode
                return (byte)0xff;
            }
            var value = address & 0xf0;
            return (byte)(value | (value >> 4));
        }

        // VRAM, OAM, CGB palettes only can be read in certain modes
        if (PpuMode.VRAM_READ.equals(mode)){
            // None can be read in this mode
            return (byte)0xff;
        }
        if (!PpuMode.OAM_READ.equals(mode) && oam.contains(address)) {
            // OAM cannot be read in this mode
            return oam.read(address);
        }
        if(videoRam.contains(address)) {
            return videoRam.read(address);
        }
        return (byte) 0xff;
    }

    @Override
    public void write(int address, byte value) {
        if (videoRam.contains(address)) {
            videoRam.write(address, value);
            return;
        }

        if (oam.contains(address)) {
            oam.write(address, value);
            return;
        }

        switch (address) {
            case LCDC:
                control.setData(value);
                return;
            case SCY:
                scrollY = value;
                return;
            case SCX:
                scrollX = value;
                return;
            case LYC:
                lineCompare = value;
                return;
            case STAT:
                stat.setData(value);
                return;
            case BGPI:
                bgPalette.setPaletteIndex(value);
                return;
            case BGPD:
                if (PpuMode.VRAM_READ.equals(mode)) {
                    // None can be read in this mode
                    return;
                }
                bgPalette.setPaletteData(value);
                return;
            case OBPI:
                objPalette.setPaletteIndex(value);
                return;
            case OBPD:
                if (PpuMode.VRAM_READ.equals(mode)) {
                    // None can be read in this mode
                    return;
                }
                objPalette.setPaletteData(value);
                return;
            case OPRI:
                objectPriorityMode = ObjectPriorityMode.fromValue((byte) (value & 0x01));
                return;
            case BGP:
                bgPaletteDmg.setData(value);
                return;
            case OBP0:
                obj0PaletteDmg.setData(value);
                return;
            case OBP1:
                obj1PaletteDmg.setData(value);
                return;
            case WX:
                windowX = value;
                return;
            case WY:
                windowY = value;
                return;
        }
    }

    public Image getFrameBuffer() {
        var image = new BufferedImage(160, 144, BufferedImage.TYPE_USHORT_555_RGB);
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                int color = frameBuffer[x][y];
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

}
