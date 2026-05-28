package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.memory.MemoryBankProvider;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import dev.vitorsilverio.gbcemu.util.RawImage;

import java.util.List;

public class Ppu implements MemorySpace, MemoryBankProvider, MachineCycle, Stateful<PpuState> {

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
    private final int[][] resolvedColorIndexes; // 160x144 pixels
    private final boolean[][] bgPriorities; // 160x144 pixels
    private final int[] frameImagePixels;
    private boolean cgbMode;

    private int cycles;
    private PpuMode mode = PpuMode.OAM_READ;
    private int currentLine;
    private int currentColumn;
    private int scrollX;
    private int scrollY;
    private long frameNumber;
    private int penaltyDelay = 0;
    private int hBlankCycles = SCANLINE_CYCLES - OAM_SCANLINE_CYCLES - MIN_VRAM_READ_CYCLES;
    private ObjectPriorityMode objectPriorityMode = ObjectPriorityMode.CGB;
    private byte lineCompare = 0;
    private int windowX;
    private int windowY;

    private boolean previousStatSignal;
    private volatile boolean frameReady;
    private final ObjectAtribute[] spriteCandidates = new ObjectAtribute[10];
    private final int[] spriteCandidateIndexes = new int[10];
    private int spriteCandidateCount;
    private int currentSpriteHeight = 8;
    private ObjectAtribute foundSpriteAttribute;
    private int foundSpriteColorIndex;
    private int bgPixelColorIndex;
    private int bgMappedColorIndex;
    private int bgPixelColor;
    private boolean bgPixelPriority;
    private int resolvedPixelColorIndex;
    private int resolvedPixelColor;
    private boolean windowYCondition;
    private int windowLineCounter;
    private boolean windowStartedOnLine;




    public Ppu(Bus bus) {
        this(bus, true);
    }

    public Ppu(Bus bus, boolean cgbMode) {
        this.bus = bus;
        this.cgbMode = cgbMode;
        this.frameBuffer = new int[160][144];
        this.bgColorIndexes = new int[160][144];
        this.resolvedColorIndexes = new int[160][144];
        this.bgPriorities = new boolean[160][144];
        this.frameImagePixels = new int[160 * 144];
        if (!cgbMode) {
            objectPriorityMode = ObjectPriorityMode.DMG;
            initializeDmgGrayCgbPalettes();
        }
    }

    public void setCgbMode(boolean cgbMode) {
        this.cgbMode = cgbMode;
        objectPriorityMode = cgbMode ? ObjectPriorityMode.CGB : ObjectPriorityMode.DMG;
        if (!cgbMode) {
            initializeDmgGrayCgbPalettes();
        }
    }


    @Override
    public void tick() {
        if (!control.isEnabled()) {
            return;
        }

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

    private void updateStatSignal() {
        boolean signal = (stat.isLycInterrupt() && (currentLine & 0xff) == (lineCompare & 0xff)) ||
                (stat.isOamInterrupt() && mode == PpuMode.OAM_READ) ||
                (stat.isVBlankInterrupt() && mode == PpuMode.VBLANK) ||
                (stat.isHBlankInterrupt() && mode == PpuMode.HBLANK);
        if (signal && !previousStatSignal) {
            bus.requestInterrupt(Interrupt.LCD_STAT);
        }
        previousStatSignal = signal;
    }

    private void setMode(PpuMode mode) {
        this.mode = mode;
        updateStatSignal();
    }

    private void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
        updateStatSignal();
    }

    private void execVRAMRead() {
        if (currentColumn >= 160) {
            currentColumn = 160;
            setMode(PpuMode.HBLANK);
            cycles = -1;
            hBlankCycles = Math.max(1, hBlankCycles);
            return;
        }

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
        loadBackgroundOrWindowPixel(currentColumn, currentLine);
        bgColorIndexes[currentColumn][currentLine] = bgPixelColorIndex;
        bgPriorities[currentColumn][currentLine] = bgPixelPriority;
        resolveSpritePixel(currentColumn, currentLine);
        resolvedColorIndexes[currentColumn][currentLine] = resolvedPixelColorIndex;
        frameBuffer[currentColumn][currentLine] = resolvedPixelColor;
        frameImagePixels[currentLine * 160 + currentColumn] = resolvedPixelColor;

        currentColumn++;
        // when line finished then go to HBLANK
        if (currentColumn == 160) {
            // End of the scanline
            hBlankCycles = SCANLINE_CYCLES - OAM_SCANLINE_CYCLES - (cycles + 1);
            setMode(PpuMode.HBLANK);
            cycles = -1;
        }

    }

    private void loadBackgroundOrWindowPixel(int x, int y) {
        TileMapArea tileMapArea = control.getBgTileArea();
        int pixelX = (x + scrollX) & 0xFF;
        int pixelY = (y + scrollY) & 0xFF;

        if (isWindowVisibleAt(x)) {
            windowStartedOnLine = true;
            tileMapArea = control.getWindowTileArea();
            pixelX = x - (windowX - 7);
            pixelY = windowLineCounter;
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

        bgPixelColorIndex = tile.getPixelUnchecked(tileX, tileY);
        int paletteIndex = cgbMode ? map.getPaletteIndex() : 0;
        int mappedColorIndex = cgbMode ? bgPixelColorIndex : bgPaletteDmg.getColor(bgPixelColorIndex);
        bgMappedColorIndex = mappedColorIndex;
        bgPixelColor = bgPalette.getColor(paletteIndex, mappedColorIndex);
        bgPixelPriority = cgbMode && map.isPriority();
    }

    private boolean isWindowVisibleAt(int x) {
        return control.isWindowEnabled() &&
                windowYCondition &&
                x >= windowX - 7 &&
                windowX >= 0 &&
                windowX <= 166 &&
                windowY <= 143;
    }

    private void resolveSpritePixel(int x, int y) {
        resolvedPixelColorIndex = bgMappedColorIndex;
        resolvedPixelColor = bgPixelColor;
        if (!control.isSpriteEnabled()) {
            return;
        }

        if (!findSpritePixel(x, y)) {
            return;
        }

        if (isBackgroundAboveSprite(foundSpriteAttribute)) {
            return;
        }

        int paletteIndex = cgbMode ? foundSpriteAttribute.getCgbPalette() : foundSpriteAttribute.getDmgPalette();
        int colorIndex = foundSpriteColorIndex;
        int mappedColorIndex = cgbMode ? colorIndex : getDmgObjectPalette(foundSpriteAttribute).getColor(colorIndex);
        resolvedPixelColorIndex = mappedColorIndex;
        resolvedPixelColor = objPalette.getColor(paletteIndex, mappedColorIndex);
    }

    private boolean findSpritePixel(int x, int y) {
        for (int i = 0; i < spriteCandidateCount; i++) {
            ObjectAtribute object = spriteCandidates[i];
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
                tileY = currentSpriteHeight - 1 - tileY;
            }

            int tileIndex = object.getTileIndexUnsigned();
            if (currentSpriteHeight == 16) {
                tileIndex &= 0xFE;
                if (tileY >= 8) {
                    tileIndex++;
                    tileY -= 8;
                }
            }

            int bank = cgbMode ? object.getBank() : 0;
            Tile tile = videoRam.getTile(TileArea.METHOD_8000, bank, tileIndex);
            int colorIndex = tile.getPixelUnchecked(tileX, tileY);
            if (colorIndex != 0) {
                foundSpriteAttribute = object;
                foundSpriteColorIndex = colorIndex;
                return true;
            }
        }
        foundSpriteAttribute = null;
        foundSpriteColorIndex = 0;
        return false;
    }

    private void prepareSpriteCandidatesForLine() {
        spriteCandidateCount = 0;
        if (!cgbMode && !control.isSpriteEnabled()) {
            return;
        }
        currentSpriteHeight = control.getSpriteSize() == 0 ? 8 : 16;
        for (int i = 0; i < 40; i++) {
            ObjectAtribute object = oam.getObjectAtribute(i);
            int spriteY = object.getScreenY();
            if (currentLine >= spriteY && currentLine < spriteY + currentSpriteHeight) {
                spriteCandidates[spriteCandidateCount] = object;
                spriteCandidateIndexes[spriteCandidateCount] = i;
                spriteCandidateCount++;
                if (spriteCandidateCount == 10) {
                    break;
                }
            }
        }
        if (objectPriorityMode == ObjectPriorityMode.DMG) {
            sortSpriteCandidatesForDmg(spriteCandidateCount);
        }
    }

    private void sortSpriteCandidatesForDmg(int candidateCount) {
        for (int i = 1; i < candidateCount; i++) {
            ObjectAtribute object = spriteCandidates[i];
            int index = spriteCandidateIndexes[i];
            int j = i - 1;
            while (j >= 0 && isSpriteCandidateAfter(spriteCandidates[j], spriteCandidateIndexes[j], object, index)) {
                spriteCandidates[j + 1] = spriteCandidates[j];
                spriteCandidateIndexes[j + 1] = spriteCandidateIndexes[j];
                j--;
            }
            spriteCandidates[j + 1] = object;
            spriteCandidateIndexes[j + 1] = index;
        }
    }

    private boolean isSpriteCandidateAfter(ObjectAtribute left, int leftIndex, ObjectAtribute right, int rightIndex) {
        int leftX = left.getScreenX();
        int rightX = right.getScreenX();
        return leftX > rightX || (leftX == rightX && leftIndex > rightIndex);
    }

    private boolean isBackgroundAboveSprite(ObjectAtribute object) {
        if (bgPixelColorIndex == 0) {
            return false;
        }
        if (!control.isBgOrWindowPriority()) {
            return false;
        }
        return bgPixelPriority || object.isPriority();
    }

    private DmgPalette getDmgObjectPalette(ObjectAtribute object) {
        return object.getDmgPalette() == 0 ? obj0PaletteDmg : obj1PaletteDmg;
    }

    private void execOAMRead() {
        // Read from OAM

        if (cycles == OAM_SCANLINE_CYCLES - 1) {
            prepareSpriteCandidatesForLine();
            setMode(PpuMode.VRAM_READ);
            cycles = -1;
            currentColumn = 0;
            penaltyDelay = scrollX % 8;
        }
    }

    private void execHBlank() {
        // Execute HBlank
        if (cycles == hBlankCycles - 1) {
            finishVisibleScanline();
            setCurrentLine(currentLine + 1);
            cycles = -1;
            if (currentLine == 144) {
                setMode(PpuMode.VBLANK);
                resetWindowFrameState();
                frameReady = true;
                frameNumber++;
                bus.requestInterrupt(Interrupt.VBLANK);
            } else {
                setMode(PpuMode.OAM_READ);
                beginVisibleScanline();
            }
        }
    }

    private void execVBlank() {
        // Execute VBlank
        if (cycles != VBLANK_CYCLES - 1) {
            return;
        }
        setCurrentLine(currentLine + 1);
        cycles = -1;
        if (currentLine > 153) {
            setCurrentLine(0);
            setMode(PpuMode.OAM_READ);
            resetWindowFrameState();
            beginVisibleScanline();
        }
    }

    @Override
    public boolean contains(int address) {
        return isRegister(address) ||
                videoRam.contains(address) ||
                oam.contains(address) ||
                (address >= 0xfea0 && address <= 0xfeff); // OAM + Prohibited area
    }

    private boolean isRegister(int address) {
        return switch (address) {
            case LCDC, STAT, SCY, SCX, LY, LYC,
                 BGP, OBP0, OBP1, WY, WX, VBK,
                 BGPI, BGPD, OBPI, OBPD, OPRI -> true;
            default -> false;
        };
    }

    @Override
    public byte read(int address) {
        if (videoRam.contains(address)) {
            if (isVideoRamAddress(address) && control.isEnabled() && PpuMode.VRAM_READ.equals(mode)) {
                return (byte) 0xff;
            }
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
            if (isVideoRamAddress(address) && control.isEnabled() && PpuMode.VRAM_READ.equals(mode)) {
                return;
            }
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
                writeControl(value);
                return;
            case SCY:
                scrollY = value & 0xFF;
                return;
            case SCX:
                scrollX = value & 0xFF;
                return;
            case LYC:
                lineCompare = value;
                updateStatSignal();
                return;
            case STAT:
                stat.setData(value);
                updateStatSignal();
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
        }
    }

    private void writeControl(byte value) {
        boolean wasEnabled = control.isEnabled();
        boolean wasWindowEnabled = control.isWindowEnabled();
        control.setData(value);
        boolean enabled = control.isEnabled();
        if (cgbMode && wasWindowEnabled && !control.isWindowEnabled()) {
            windowYCondition = false;
            windowLineCounter = 0;
            windowStartedOnLine = false;
        }
        if (wasEnabled && !enabled) {
            disableLcd();
        } else if (!wasEnabled && enabled) {
            enableLcd();
        }
    }

    private boolean isVideoRamAddress(int address) {
        return 0x8000 <= address && address <= 0x9FFF;
    }

    private void disableLcd() {
        currentLine = 0;
        currentColumn = 0;
        cycles = 0;
        penaltyDelay = 0;
        hBlankCycles = SCANLINE_CYCLES - OAM_SCANLINE_CYCLES - MIN_VRAM_READ_CYCLES;
        mode = PpuMode.HBLANK;
        previousStatSignal = false;
        updateStatSignal();
        resetWindowFrameState();
    }

    private void enableLcd() {
        currentLine = 0;
        currentColumn = 0;
        cycles = 0;
        penaltyDelay = 0;
        hBlankCycles = SCANLINE_CYCLES - OAM_SCANLINE_CYCLES - MIN_VRAM_READ_CYCLES;
        mode = PpuMode.OAM_READ;
        previousStatSignal = false;
        updateStatSignal();
        resetWindowFrameState();
        beginVisibleScanline();
    }

    private void resetWindowFrameState() {
        windowYCondition = false;
        windowLineCounter = 0;
        windowStartedOnLine = false;
    }

    private void beginVisibleScanline() {
        windowStartedOnLine = false;
        if (currentLine == windowY) {
            windowYCondition = true;
        }
    }

    private void finishVisibleScanline() {
        if (windowStartedOnLine) {
            windowLineCounter = (windowLineCounter + 1) & 0xFF;
        }
    }

    public RawImage getFrameBuffer() {
        return RawImage.wrapCopy(160, 144, frameImagePixels);
    }

    public void copyFrameBufferTo(int[] target) {
        if (target.length < frameImagePixels.length) {
            throw new IllegalArgumentException("Target buffer is too small");
        }
        System.arraycopy(frameImagePixels, 0, target, 0, frameImagePixels.length);
    }

    public int getResolvedColorIndex(int x, int y) {
        if (x < 0 || x >= 160 || y < 0 || y >= 144) {
            return 0;
        }
        return resolvedColorIndexes[x][y] & 0x03;
    }

    public int getBackgroundColorIndex(int x, int y) {
        if (x < 0 || x >= 160 || y < 0 || y >= 144) {
            return 0;
        }
        return bgColorIndexes[x][y] & 0x03;
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

    public boolean canRunHBlankDma() {
        return mode == PpuMode.HBLANK && currentLine < 144;
    }

    @Override
    public PpuState saveState() {
        return new PpuState(
                control.getData(),
                stat.getData(),
                bgPalette.copyData(),
                bgPalette.getPaletteIndex(),
                objPalette.copyData(),
                objPalette.getPaletteIndex(),
                bgPaletteDmg.getData(),
                obj0PaletteDmg.getData(),
                obj1PaletteDmg.getData(),
                copyIntMatrix(frameBuffer),
                copyIntMatrix(bgColorIndexes),
                copyIntMatrix(resolvedColorIndexes),
                copyBooleanMatrix(bgPriorities),
                cgbMode,
                cycles,
                mode,
                currentLine,
                currentColumn,
                scrollX,
                scrollY,
                penaltyDelay,
                hBlankCycles,
                objectPriorityMode,
                lineCompare,
                windowX,
                windowY,
                previousStatSignal,
                frameReady,
                windowYCondition,
                windowLineCounter,
                windowStartedOnLine,
                spriteCandidateCount
        );
    }

    @Override
    public void loadState(PpuState state) {
        control.setData(state.control());
        stat.setData(state.stat());
        bgPalette.restoreData(state.bgPalette(), state.bgPaletteIndex());
        objPalette.restoreData(state.objPalette(), state.objPaletteIndex());
        bgPaletteDmg.setData(state.bgPaletteDmg());
        obj0PaletteDmg.setData(state.obj0PaletteDmg());
        obj1PaletteDmg.setData(state.obj1PaletteDmg());
        restoreIntMatrix(state.frameBuffer(), frameBuffer);
        restoreFrameImage();
        restoreIntMatrix(state.bgColorIndexes(), bgColorIndexes);
        restoreIntMatrix(state.resolvedColorIndexes(), resolvedColorIndexes);
        restoreBooleanMatrix(state.bgPriorities(), bgPriorities);
        cgbMode = state.cgbMode();
        cycles = state.cycles();
        mode = state.mode();
        currentLine = state.currentLine();
        currentColumn = state.currentColumn();
        scrollX = state.scrollX();
        scrollY = state.scrollY();
        penaltyDelay = state.penaltyDelay();
        hBlankCycles = state.hBlankCycles();
        objectPriorityMode = state.objectPriorityMode();
        lineCompare = state.lineCompare();
        windowX = state.windowX();
        windowY = state.windowY();
        previousStatSignal = state.previousStatSignal();
        frameReady = state.frameReady();
        windowYCondition = state.windowYCondition();
        windowLineCounter = state.windowLineCounter();
        windowStartedOnLine = state.windowStartedOnLine();
        spriteCandidateCount = Math.max(0, Math.min(state.spriteCandidateCount(), spriteCandidates.length));
        normalizeRestoredState();
    }

    private int[][] copyIntMatrix(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private boolean[][] copyBooleanMatrix(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private void restoreIntMatrix(int[][] matrix, int[][] destination) {
        for (int i = 0; i < Math.min(matrix.length, destination.length); i++) {
            System.arraycopy(matrix[i], 0, destination[i], 0, Math.min(matrix[i].length, destination[i].length));
        }
    }

    private void restoreFrameImage() {
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                frameImagePixels[y * 160 + x] = frameBuffer[x][y];
            }
        }
    }

    private void restoreBooleanMatrix(boolean[][] matrix, boolean[][] destination) {
        for (int i = 0; i < Math.min(matrix.length, destination.length); i++) {
            System.arraycopy(matrix[i], 0, destination[i], 0, Math.min(matrix[i].length, destination[i].length));
        }
    }

    private void normalizeRestoredState() {
        currentLine = Math.max(0, Math.min(currentLine, 153));
        currentColumn = Math.max(0, Math.min(currentColumn, 160));
        cycles = Math.max(-1, Math.min(cycles, SCANLINE_CYCLES - 1));
        penaltyDelay = Math.max(0, penaltyDelay);
        hBlankCycles = Math.max(1, hBlankCycles);

        if (currentLine >= 144) {
            mode = PpuMode.VBLANK;
            currentColumn = 160;
            return;
        }

        if (mode == PpuMode.VRAM_READ && currentColumn == 160) {
            mode = PpuMode.HBLANK;
        }
        updateStatSignal();
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

    public RawImage debugTileImage(int bank) {
        RawImage image = new RawImage(16 * 8, 24 * 8);
        for (int tileIndex = 0; tileIndex < 384; tileIndex++) {
            Tile tile = videoRam.getTile(TileArea.METHOD_8000, bank & 1, tileIndex);
            int baseX = (tileIndex % 16) * 8;
            int baseY = (tileIndex / 16) * 8;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    image.setArgb(baseX + x, baseY + y, grayColor(tile.getPixel(x, y)));
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

    public RawImage debugTileMapImage(TileMapArea area) {
        RawImage image = new RawImage(256, 256);
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
                    int paletteIndex = cgbMode ? map.getPaletteIndex() : 0;
                    int mappedColorIndex = cgbMode ? colorIndex : bgPaletteDmg.getColor(colorIndex);
                    int color = bgPalette.getColor(paletteIndex, mappedColorIndex);
                    image.setArgb(baseX + x, baseY + y, color);
                }
            }
        }
        return image;
    }

    public RawImage debugPaletteImage(boolean objects) {
        int[] colors = objects ? objPalette.colors() : bgPalette.colors();
        RawImage image = new RawImage(128, 32);
        for (int palette = 0; palette < 8; palette++) {
            for (int color = 0; color < 4; color++) {
                fillRect(image, color * 32, palette * 4, 32, 4, colors[palette * 4 + color]);
            }
        }
        return image;
    }

    private void fillRect(RawImage image, int x, int y, int width, int height, int color) {
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                image.setArgb(xx, yy, color);
            }
        }
    }

    public void setCgbBackgroundPaletteBytes(int paletteIndex, byte[] data) {
        bgPalette.setPaletteBytes(paletteIndex, data);
    }

    public void setCgbObjectPaletteBytes(int paletteIndex, byte[] data) {
        objPalette.setPaletteBytes(paletteIndex, data);
    }

    public void applyCgbCompatibilityPalettes(CgbCompatibilityPaletteSelection selection) {
        setCgbObjectPaletteBytes(0, CgbCompatibilityPaletteColors.littleEndianBytes(selection.obj0PaletteWordOffset()));
        setCgbObjectPaletteBytes(1, CgbCompatibilityPaletteColors.littleEndianBytes(selection.obj1PaletteWordOffset()));
        setCgbBackgroundPaletteBytes(0, CgbCompatibilityPaletteColors.littleEndianBytes(selection.bgPaletteWordOffset()));
    }

    private void initializeDmgGrayCgbPalettes() {
        byte[] palette = new byte[] {
                (byte) 0xFF, 0x7F,
                (byte) 0x18, 0x63,
                0x10, 0x42,
                0x00, 0x00
        };
        for (int paletteIndex = 0; paletteIndex < 8; paletteIndex++) {
            bgPalette.setPaletteBytes(paletteIndex, palette);
            objPalette.setPaletteBytes(paletteIndex, palette);
        }
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

    public VideoRam getVideoRam() {
        return videoRam;
    }

    public OamRAM getOam() {
        return oam;
    }

    @Override
    public List<MemoryBank> memoryBanks() {
        return List.of(videoRam);
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

}
