package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Ppu implements MemorySpace, MachineCycle {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Ppu.class);

    private static final int OAM_SCANLINE_CYCLES = 80;
    private static final int SCANLINE_CYCLES = 456;
    private static final int MIN_VRAM_READ_CYCLES = 172;
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
    private final int[][] bgColorIndexes; // 160x144 pixels
    private final boolean[][] bgPriorities; // 160x144 pixels
    private boolean cgbMode;

    private int cycles;
    private PpuMode mode = PpuMode.OAM_READ;
    private int currentLine;
    private int currentColumn;
    private int scrollX;
    private int scrollY;
    private int penaltyDelay = 0;
    private int hBlankCycles = SCANLINE_CYCLES - OAM_SCANLINE_CYCLES - MIN_VRAM_READ_CYCLES;
    private ObjectPriorityMode objectPriorityMode = ObjectPriorityMode.CGB;
    private byte lineCompare = 0;
    private int windowX;
    private int windowY;

    private boolean previousStatSignal;
    private volatile boolean frameReady;




    public Ppu(Bus bus) {
        this(bus, true);
    }

    public Ppu(Bus bus, boolean cgbMode) {
        this.bus = bus;
        this.cgbMode = cgbMode;
        this.frameBuffer = new int[160][144];
        this.bgColorIndexes = new int[160][144];
        this.bgPriorities = new boolean[160][144];
        if (!cgbMode) {
            objectPriorityMode = ObjectPriorityMode.DMG;
        }
    }

    public void setCgbMode(boolean cgbMode) {
        this.cgbMode = cgbMode;
        objectPriorityMode = cgbMode ? ObjectPriorityMode.CGB : ObjectPriorityMode.DMG;
    }


    @Override
    public void tick() {
        if (!control.isEnabled()) {
            return;
        }

        logger.trace("Ppu tick: mode={}, line={}, column={}, cycles={}", mode, currentLine, currentColumn, cycles);

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
        statInterrupt();
        cycles++;
    }

    private void statInterrupt() {
        boolean signal = (stat.isLycInterrupt() && (currentLine & 0xff) == (lineCompare & 0xff)) ||
                (stat.isOamInterrupt() && mode == PpuMode.OAM_READ) ||
                (stat.isVBlankInterrupt() && mode == PpuMode.VBLANK) ||
                (stat.isHBlankInterrupt() && mode == PpuMode.HBLANK);
        if (signal && !previousStatSignal) {
            bus.requestInterrupt(Interrupt.LCD_STAT);
        }
        previousStatSignal = signal;
    }

    private void execVRAMRead() {
        // wait penalty
        if (penaltyDelay > 0) {
            penaltyDelay--;
            return;
        }

        // Draw current line
        if (cycles < 12) {
            // The 12 extra dots of penalty come from two tile fetches at the beginning of Mode 3. One is the first tile in the scanline (the one that gets shifted by SCX % 8 pixels), the other is simply discarded.
            return;
        }
        Pixel bgPixel = getBackgroundOrWindowPixel(currentColumn, currentLine);
        bgColorIndexes[currentColumn][currentLine] = bgPixel.colorIndex();
        bgPriorities[currentColumn][currentLine] = bgPixel.priority();
        Pixel pixel = getSpritePixel(currentColumn, currentLine, bgPixel);
        frameBuffer[currentColumn][currentLine] = pixel.color();

        currentColumn++;
        // when line finished then go to HBLANK
        if (currentColumn == 160) {
            // End of the scanline
            hBlankCycles = SCANLINE_CYCLES - OAM_SCANLINE_CYCLES - (cycles + 1);
            mode = PpuMode.HBLANK;
            cycles = -1;
        }

    }

    private Pixel getBackgroundOrWindowPixel(int x, int y) {
        TileMapArea tileMapArea = control.getBgTileArea();
        int pixelX = (x + scrollX) & 0xFF;
        int pixelY = (y + scrollY) & 0xFF;

        if (isWindowVisibleAt(x, y)) {
            tileMapArea = control.getWindowTileArea();
            pixelX = x - (windowX - 7);
            pixelY = y - windowY;
        }

        int indexY = ((pixelY / 8) % 32);
        int indexX = ((pixelX / 8) % 32);
        int index = ((indexY * 32) + indexX) & 0x3ff;
        TileMap map = videoRam.getTileMap(tileMapArea, index);
        int bank = cgbMode ? map.getBank() : 0;
        Tile tile = videoRam.getTile(control.getTileArea(), bank, map.getIndex());

        int tileX = pixelX & 0x7;
        int tileY = pixelY & 0x7;
        if (cgbMode && map.isFlipX()) {
            tileX = 7 - tileX;
        }
        if (cgbMode && map.isFlipY()) {
            tileY = 7 - tileY;
        }

        int colorIndex = tile.getPixel(tileX, tileY);
        int paletteIndex = cgbMode ? map.getPaletteIndex() : 0;
        int mappedColorIndex = cgbMode ? colorIndex : bgPaletteDmg.getColor(colorIndex);
        int color = cgbMode ? bgPalette.getColor(paletteIndex, mappedColorIndex) : grayColor(mappedColorIndex);
        return new Pixel(colorIndex, color, cgbMode && map.isPriority());
    }

    private boolean isWindowVisibleAt(int x, int y) {
        return control.isWindowEnabled() &&
                y >= windowY &&
                x >= windowX - 7 &&
                windowX <= 166 &&
                windowY <= 143;
    }

    private Pixel getSpritePixel(int x, int y, Pixel bgPixel) {
        if (!control.isSpriteEnabled()) {
            return bgPixel;
        }

        SpritePixel spritePixel = findSpritePixel(x, y);
        if (spritePixel == null) {
            return bgPixel;
        }

        if (isBackgroundAboveSprite(bgPixel, spritePixel.attribute())) {
            return bgPixel;
        }

        int paletteIndex = cgbMode ? spritePixel.attribute().getCgbPalette() : spritePixel.attribute().getDmgPalette();
        int colorIndex = spritePixel.colorIndex();
        int mappedColorIndex = cgbMode ? colorIndex : getDmgObjectPalette(spritePixel.attribute()).getColor(colorIndex);
        int color = cgbMode ? objPalette.getColor(paletteIndex, mappedColorIndex) : grayColor(mappedColorIndex);
        return new Pixel(colorIndex, color, false);
    }

    private SpritePixel findSpritePixel(int x, int y) {
        int spriteHeight = control.getSpriteSize() == 0 ? 8 : 16;
        List<SpriteCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            ObjectAtribute object = oam.getObjectAtribute(i);
            int spriteY = object.getScreenY();
            if (y >= spriteY && y < spriteY + spriteHeight) {
                candidates.add(new SpriteCandidate(i, object));
                if (candidates.size() == 10) {
                    break;
                }
            }
        }

        if (objectPriorityMode == ObjectPriorityMode.DMG) {
            candidates.sort(Comparator
                    .comparingInt((SpriteCandidate sprite) -> sprite.attribute().getScreenX())
                    .thenComparingInt(SpriteCandidate::index));
        }

        for (SpriteCandidate candidate : candidates) {
            ObjectAtribute object = candidate.attribute();
            int spriteX = object.getScreenX();
            if (x < spriteX || x >= spriteX + 8) {
                continue;
            }

            int tileX = x - spriteX;
            int tileY = y - object.getScreenY();
            if (object.isFlipX()) {
                tileX = 7 - tileX;
            }
            if (object.isFlipY()) {
                tileY = spriteHeight - 1 - tileY;
            }

            int tileIndex = object.getTileIndexUnsigned();
            if (spriteHeight == 16) {
                tileIndex &= 0xFE;
                if (tileY >= 8) {
                    tileIndex++;
                    tileY -= 8;
                }
            }

            int bank = cgbMode ? object.getBank() : 0;
            Tile tile = videoRam.getTile(TileArea.METHOD_8000, bank, tileIndex);
            int colorIndex = tile.getPixel(tileX, tileY);
            if (colorIndex != 0) {
                return new SpritePixel(colorIndex, object);
            }
        }
        return null;
    }

    private boolean isBackgroundAboveSprite(Pixel bgPixel, ObjectAtribute object) {
        if (bgPixel.colorIndex() == 0) {
            return false;
        }
        if (!control.isBgOrWindowPriority()) {
            return false;
        }
        return bgPixel.priority() || object.isPriority();
    }

    private DmgPalette getDmgObjectPalette(ObjectAtribute object) {
        return object.getDmgPalette() == 0 ? obj0PaletteDmg : obj1PaletteDmg;
    }

    private void execOAMRead() {
        // Read from OAM

        if (cycles == OAM_SCANLINE_CYCLES - 1) {
            mode = PpuMode.VRAM_READ;
            cycles = -1;
            currentColumn = 0;
            penaltyDelay = scrollX % 8;
        }
    }

    private void execHBlank() {
        // Execute HBlank
        if (cycles == hBlankCycles - 1) {
            currentLine++;
            cycles = -1;
            if (currentLine == 144) {
                mode = PpuMode.VBLANK;
                frameReady = true;
                bus.requestInterrupt(Interrupt.VBLANK);
            } else {
                mode = PpuMode.OAM_READ;
            }
        }
    }

    private void execVBlank() {
        // Execute VBlank
        if (cycles != VBLANK_CYCLES - 1) {
            return;
        }
        currentLine++;
        cycles = -1;
        if (currentLine > 153) {
            currentLine = 0;
            mode = PpuMode.OAM_READ;
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
                return (byte) (scrollY & 0xFF);
            case SCX:
                return (byte) (scrollX & 0xFF);
            case LY:
                return (byte) (currentLine & 0xFF);
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
                return (byte) (windowX & 0xFF);
            case WY:
                return (byte) (windowY & 0xFF);
            case STAT:
                return (byte) (stat.getData() |
                        (control.isEnabled() ? mode.getValue() & 0x3 : 0 ) |
                        (currentLine == lineCompare ? 0x04 : 0));
        }

        if (oam.contains(address)) {
            if (control.isEnabled() && (PpuMode.OAM_READ.equals(mode) || PpuMode.VRAM_READ.equals(mode))) {
                return (byte) 0xff;
            }
            return oam.read(address);
        }

        if (0xfea0 <= address && address <= 0xfeff) { // Prohibited area CGB rev 5+
            if (control.isEnabled() && PpuMode.OAM_READ.equals(mode)) {
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
            if (control.isEnabled() && (PpuMode.OAM_READ.equals(mode) || PpuMode.VRAM_READ.equals(mode))) {
                return;
            }
            oam.write(address, value);
            return;
        }

        if (0xfea0 <= address && address <= 0xfeff) {
            return;
        }

        switch (address) {
            case LCDC:
                control.setData(value);
                return;
            case SCY:
                scrollY = value & 0xFF;
                return;
            case SCX:
                scrollX = value & 0xFF;
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
                bgPalette.setPaletteData(value, !PpuMode.VRAM_READ.equals(mode));
                return;
            case OBPI:
                objPalette.setPaletteIndex(value);
                return;
            case OBPD:
                objPalette.setPaletteData(value, !PpuMode.VRAM_READ.equals(mode));
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
                windowX = value & 0xFF;
                return;
            case WY:
                windowY = value & 0xFF;
                return;
        }
    }

    public BufferedImage getFrameBuffer() {
        var image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                int color = frameBuffer[x][y];
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    public boolean consumeFrameReady() {
        if (!frameReady) {
            return false;
        }
        frameReady = false;
        return true;
    }

    public boolean isHBlank() {
        return mode == PpuMode.HBLANK;
    }

    public DebugSnapshot debugSnapshot() {
        return new DebugSnapshot(
                cgbMode,
                control.getData() & 0xFF,
                stat.getData() & 0xFF,
                mode,
                currentLine,
                currentColumn,
                cycles,
                scrollX,
                scrollY,
                windowX,
                windowY,
                lineCompare & 0xFF
        );
    }

    public FrameDebugStats frameDebugStats() {
        int blackPixels = 0;
        int whitePixels = 0;
        int otherPixels = 0;
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                int rgb = frameBuffer[x][y] & 0x00FFFFFF;
                if (rgb == 0) {
                    blackPixels++;
                } else if (rgb == 0x00FFFFFF) {
                    whitePixels++;
                } else {
                    otherPixels++;
                }
            }
        }

        int nonEmptyTiles = 0;
        for (int bank = 0; bank < 2; bank++) {
            for (int tileIndex = 0; tileIndex < 384; tileIndex++) {
                Tile tile = videoRam.getTile(TileArea.METHOD_8000, bank, tileIndex);
                if (!isEmptyTile(tile)) {
                    nonEmptyTiles++;
                }
            }
        }

        return new FrameDebugStats(
                cgbMode,
                control.getData() & 0xFF,
                stat.getData() & 0xFF,
                mode,
                currentLine,
                currentColumn,
                blackPixels,
                whitePixels,
                otherPixels,
                nonEmptyTiles,
                bgPalette.getColor(0, 0),
                bgPalette.getColor(0, 1),
                bgPalette.getColor(0, 2),
                bgPalette.getColor(0, 3),
                bgPaletteDmg.getData() & 0xFF
        );
    }

    private boolean isEmptyTile(Tile tile) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (tile.getPixel(x, y) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public BufferedImage debugTileImage(int bank) {
        BufferedImage image = new BufferedImage(16 * 8, 24 * 8, BufferedImage.TYPE_INT_RGB);
        for (int tileIndex = 0; tileIndex < 384; tileIndex++) {
            Tile tile = videoRam.getTile(TileArea.METHOD_8000, bank & 1, tileIndex);
            int baseX = (tileIndex % 16) * 8;
            int baseY = (tileIndex / 16) * 8;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    image.setRGB(baseX + x, baseY + y, grayColor(tile.getPixel(x, y)));
                }
            }
        }
        return image;
    }

    public int[] copyFrameBufferArgb() {
        int[] pixels = new int[160 * 144];
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                pixels[y * 160 + x] = frameBuffer[x][y];
            }
        }
        return pixels;
    }

    public int[] copyBgColorIndexesArgb() {
        int[] pixels = new int[160 * 144];
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                pixels[y * 160 + x] = switch (bgColorIndexes[x][y] & 0x03) {
                    case 0 -> 0xFFFFFFFF;
                    case 1 -> 0xFFFF0000;
                    case 2 -> 0xFF00FF00;
                    case 3 -> 0xFF0000FF;
                    default -> 0xFFFF00FF;
                };
            }
        }
        return pixels;
    }

    public BufferedImage debugTileMapImage(TileMapArea area) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int mapIndex = 0; mapIndex < 1024; mapIndex++) {
            TileMap map = videoRam.getTileMap(area, mapIndex);
            int bank = cgbMode ? map.getBank() : 0;
            Tile tile = videoRam.getTile(control.getTileArea(), bank, map.getIndex());
            int baseX = (mapIndex % 32) * 8;
            int baseY = (mapIndex / 32) * 8;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int tileX = cgbMode && map.isFlipX() ? 7 - x : x;
                    int tileY = cgbMode && map.isFlipY() ? 7 - y : y;
                    int colorIndex = tile.getPixel(tileX, tileY);
                    int color = cgbMode
                            ? bgPalette.getColor(map.getPaletteIndex(), colorIndex)
                            : grayColor(bgPaletteDmg.getColor(colorIndex));
                    image.setRGB(baseX + x, baseY + y, color);
                }
            }
        }
        return image;
    }

    public BufferedImage debugPaletteImage(boolean objects) {
        int[] colors = objects ? objPalette.colors() : bgPalette.colors();
        BufferedImage image = new BufferedImage(128, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            for (int palette = 0; palette < 8; palette++) {
                for (int color = 0; color < 4; color++) {
                    graphics.setColor(new Color(colors[palette * 4 + color], true));
                    graphics.fillRect(color * 32, palette * 4, 32, 4);
                }
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private int grayColor(int colorIndex) {
        return switch (colorIndex & 0x03) {
            case 0 -> 0xFFFFFFFF;
            case 1 -> 0xFFC0C0C0;
            case 2 -> 0xFF808080;
            case 3 -> 0xFF000000;
            default -> 0xFFFFFFFF;
        };
    }

    byte readOamRaw(int address) {
        return oam.read(address);
    }

    public record DebugSnapshot(
            boolean cgbMode,
            int lcdc,
            int stat,
            PpuMode mode,
            int line,
            int column,
            int cycles,
            int scrollX,
            int scrollY,
            int windowX,
            int windowY,
            int lineCompare
    ) {
    }

    public record FrameDebugStats(
            boolean cgbMode,
            int lcdc,
            int stat,
            PpuMode mode,
            int line,
            int column,
            int blackPixels,
            int whitePixels,
            int otherPixels,
            int nonEmptyTiles,
            int bgColor0,
            int bgColor1,
            int bgColor2,
            int bgColor3,
            int dmgBgPalette
    ) {
    }

    private record Pixel(int colorIndex, int color, boolean priority) {
    }

    private record SpritePixel(int colorIndex, ObjectAtribute attribute) {
    }

    private record SpriteCandidate(int index, ObjectAtribute attribute) {
    }
}
