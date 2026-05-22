package dev.vitorsilverio.gbcemu.sgb;

import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class SuperGameBoy implements Stateful<SuperGameBoyState> {

    public static final int BORDER_WIDTH = 256;
    public static final int BORDER_HEIGHT = 224;
    public static final int GAME_SCREEN_X = 48;
    public static final int GAME_SCREEN_Y = 40;

    private static final int COMMAND_PAL01 = 0x00;
    private static final int COMMAND_PAL23 = 0x01;
    private static final int COMMAND_PAL03 = 0x02;
    private static final int COMMAND_PAL12 = 0x03;
    private static final int COMMAND_ATTR_BLK = 0x04;
    private static final int COMMAND_ATTR_LIN = 0x05;
    private static final int COMMAND_ATTR_DIV = 0x06;
    private static final int COMMAND_ATTR_CHR = 0x07;
    private static final int COMMAND_PAL_SET = 0x0A;
    private static final int COMMAND_PAL_TRN = 0x0B;
    private static final int COMMAND_ATTR_TRN = 0x15;
    private static final int COMMAND_ATTR_SET = 0x16;
    private static final int COMMAND_MLT_REQ = 0x11;
    private static final int COMMAND_CHR_TRN = 0x13;
    private static final int COMMAND_PCT_TRN = 0x14;
    private static final int COMMAND_MASK_EN = 0x17;
    private static final int COMMAND_OBJ_TRN = 0x18;
    private static final int PACKET_BITS = 128;
    private static final int PACKET_BYTES = 16;
    private static final int CHR_TRANSFER_SIZE = 0x1000;
    private static final int PCT_TRANSFER_SIZE = 0x1000;
    private static final int BORDER_TILE_COUNT = 256;
    private static final int BORDER_TILE_SIZE = 32;
    private static final int BORDER_MAP_BYTES = 0x700;
    private static final int BORDER_PALETTE_OFFSET = 0x800;
    private static final int BORDER_PALETTE_BYTES = 0x80;
    private static final int SGB_TRANSFER_FRAMES = 5;
    private static final int MAX_PENDING_TRANSFERS = 8;
    private static final int JOYP_START = 0x00;
    private static final int JOYP_ONE_BIT = 0x01;
    private static final int JOYP_STOP = 0x02;
    private static final int JOYP_CLOCK = 0x03;

    private final boolean enabled;
    private final Ppu ppu;
    private final int[] pulseBuffer = new int[PACKET_BITS];
    private final byte[] pendingCommand = new byte[PACKET_BYTES * 7];
    private final byte[] borderTileData = new byte[BORDER_TILE_COUNT * BORDER_TILE_SIZE];
    private final byte[] pictureTransferData = new byte[PCT_TRANSFER_SIZE];
    private final byte[] systemPaletteData = new byte[PCT_TRANSFER_SIZE];
    private final byte[] attributeFileData = new byte[90 * 45];
    private final int[][] screenPalettes = new int[4][4];
    private final byte[] screenAttributes = new byte[20 * 18];

    private int previousLines;
    private int pulseCount = -1;
    private int command;
    private int expectedPackets;
    private int receivedPackets;
    private int maskMode;
    private int joypadCount = 1;
    private int selectedJoypad;
    private boolean receivingPacket;
    private boolean waitingStopBit;
    private int lastHeaderByte;
    private String lastPacketHex = "";
    private final int[] pendingTransferCommands = new int[MAX_PENDING_TRANSFERS];
    private final int[] pendingTransferDestinations = new int[MAX_PENDING_TRANSFERS];
    private final int[] pendingTransferFrames = new int[MAX_PENDING_TRANSFERS];
    private int pendingTransferCount;
    private int transferMaskFrames;
    private boolean pendingMaskClear;
    private boolean systemPalettesReady;
    private long packetsReceived;
    private long invalidPackets;
    private long ignoredPackets;
    private BufferedImage borderImage;
    private String lastCommandName = "";
    private final String[] recentCommandNames = new String[16];
    private int recentCommandCursor;

    @Override
    public SuperGameBoyState saveState() {
        return new SuperGameBoyState(
                enabled,
                pulseBuffer.clone(),
                pendingCommand.clone(),
                borderTileData.clone(),
                pictureTransferData.clone(),
                systemPaletteData.clone(),
                attributeFileData.clone(),
                copyScreenPalettes(),
                screenAttributes.clone(),
                previousLines,
                pulseCount,
                command,
                expectedPackets,
                receivedPackets,
                maskMode,
                joypadCount,
                selectedJoypad,
                receivingPacket,
                waitingStopBit,
                lastHeaderByte,
                lastPacketHex,
                pendingTransferCommands.clone(),
                pendingTransferDestinations.clone(),
                pendingTransferFrames.clone(),
                pendingTransferCount,
                transferMaskFrames,
                pendingMaskClear,
                systemPalettesReady,
                packetsReceived,
                invalidPackets,
                ignoredPackets,
                borderImage != null,
                lastCommandName
        );
    }

    @Override
    public void loadState(SuperGameBoyState state) {
        if (!enabled || state == null || !state.enabled()) {
            reset();
            return;
        }
        copy(state.pulseBuffer(), pulseBuffer);
        copy(state.pendingCommand(), pendingCommand);
        copy(state.borderTileData(), borderTileData);
        copy(state.pictureTransferData(), pictureTransferData);
        copy(state.systemPaletteData(), systemPaletteData);
        copy(state.attributeFileData(), attributeFileData);
        restoreScreenPalettes(state.screenPalettes());
        copy(state.screenAttributes(), screenAttributes);
        previousLines = state.previousLines() & 0x03;
        pulseCount = Math.max(-1, Math.min(state.pulseCount(), PACKET_BITS + 1));
        command = Math.max(0, Math.min(state.command(), 0x1F));
        expectedPackets = Math.max(0, Math.min(state.expectedPackets(), 7));
        receivedPackets = Math.max(0, Math.min(state.receivedPackets(), 7));
        maskMode = state.maskMode() & 0x03;
        joypadCount = switch (state.joypadCount()) {
            case 2 -> 2;
            case 4 -> 4;
            default -> 1;
        };
        selectedJoypad = Math.floorMod(state.selectedJoypad(), joypadCount);
        receivingPacket = state.receivingPacket();
        waitingStopBit = state.waitingStopBit();
        lastHeaderByte = state.lastHeaderByte() & 0xFF;
        lastPacketHex = state.lastPacketHex() == null ? "" : state.lastPacketHex();
        restorePendingTransfers(state);
        transferMaskFrames = Math.max(0, state.transferMaskFrames());
        pendingMaskClear = state.pendingMaskClear();
        systemPalettesReady = state.systemPalettesReady();
        packetsReceived = Math.max(0, state.packetsReceived());
        invalidPackets = Math.max(0, state.invalidPackets());
        ignoredPackets = Math.max(0, state.ignoredPackets());
        lastCommandName = state.lastCommandName() == null ? "" : state.lastCommandName();
        if (state.borderReady()) {
            buildBorderImage();
        } else {
            borderImage = null;
        }
    }

    public SuperGameBoy(boolean enabled, Ppu ppu) {
        this.enabled = enabled;
        this.ppu = ppu;
        initializeScreenPalettes();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void writeJoypad(int selectedLines) {
        if (!enabled) {
            return;
        }
        int bits = (selectedLines >> 4) & 0x03;
        if (bits == JOYP_START) {
            pulseCount = -1;
            Arrays.fill(pulseBuffer, 0);
            receivingPacket = true;
            waitingStopBit = false;
            previousLines = bits;
            return;
        }

        if (bits == previousLines) {
            return;
        }

        if ((bits & JOYP_STOP) != 0) {
            if (!receivingPacket && waitingStopBit && joypadCount > 1) {
                waitingStopBit = false;
                selectedJoypad = (selectedJoypad + 1) % joypadCount;
            }
        } else if ((previousLines & JOYP_STOP) != 0) {
            waitingStopBit = !waitingStopBit;
        }

        previousLines = bits;

        if (pulseCount == PACKET_BITS && bits == JOYP_STOP) {
            completePacket();
            pulseCount++;
            receivingPacket = false;
            return;
        }
        if (pulseCount >= PACKET_BITS) {
            return;
        }
        switch (bits) {
            case JOYP_ONE_BIT -> {
                if (pulseCount >= 0) {
                    pulseBuffer[pulseCount] = 1;
                }
            }
            case JOYP_CLOCK -> {
                pulseCount++;
                receivingPacket = pulseCount < PACKET_BITS;
            }
            default -> {
            }
        }
    }

    public int joypadIdNibble() {
        if (!enabled || joypadCount <= 1) {
            return 0x0F;
        }
        return 0x0F - selectedJoypad;
    }

    public BufferedImage borderImage() {
        return borderImage;
    }

    public BufferedImage copyBorderImage() {
        if (borderImage == null) {
            return null;
        }
        BufferedImage copy = new BufferedImage(BORDER_WIDTH, BORDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = borderImage.getRGB(0, 0, BORDER_WIDTH, BORDER_HEIGHT, null, 0, BORDER_WIDTH);
        copy.setRGB(0, 0, BORDER_WIDTH, BORDER_HEIGHT, pixels, 0, BORDER_WIDTH);
        return copy;
    }

    public BufferedImage colorizeFrame(BufferedImage frame) {
        if (!enabled || frame == null) {
            return frame;
        }
        BufferedImage colorized = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 144; y++) {
            int attributeY = Math.min(17, y / 8);
            for (int x = 0; x < 160; x++) {
                int attributeX = Math.min(19, x / 8);
                int palette = screenAttributes[attributeY * 20 + attributeX] & 0x03;
                int colorIndex = ppu.getResolvedColorIndex(x, y);
                colorized.setRGB(x, y, screenPalettes[palette][colorIndex]);
            }
        }
        return colorized;
    }

    public BufferedImage debugAttributeImage() {
        BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
        int[] colors = {0xFF202020, 0xFFE34A4A, 0xFF4AC46B, 0xFF5277D8};
        for (int y = 0; y < 144; y++) {
            int attributeY = Math.min(17, y / 8);
            for (int x = 0; x < 160; x++) {
                int attributeX = Math.min(19, x / 8);
                int palette = screenAttributes[attributeY * 20 + attributeX] & 0x03;
                image.setRGB(x, y, colors[palette]);
            }
        }
        return image;
    }

    public int expectedPackets() {
        return expectedPackets;
    }

    public int receivedPackets() {
        return receivedPackets;
    }

    public int pulseCount() {
        return pulseCount;
    }

    public int joypadCount() {
        return joypadCount;
    }

    public int selectedJoypad() {
        return selectedJoypad;
    }

    public long packetsReceived() {
        return packetsReceived;
    }

    public long invalidPackets() {
        return invalidPackets;
    }

    public long ignoredPackets() {
        return ignoredPackets;
    }

    public boolean isReceivingPacket() {
        return receivingPacket;
    }

    public int lastHeaderByte() {
        return lastHeaderByte;
    }

    public String lastPacketHex() {
        return lastPacketHex;
    }

    public boolean hasBorder() {
        return borderImage != null;
    }

    public int maskMode() {
        return maskMode;
    }

    public String lastCommandName() {
        return lastCommandName;
    }

    public String recentCommandHistory() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < recentCommandNames.length; i++) {
            int index = (recentCommandCursor + i) % recentCommandNames.length;
            String commandName = recentCommandNames[index];
            if (commandName == null || commandName.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(commandName);
        }
        return builder.toString();
    }

    public int pendingTransferFrames() {
        return pendingTransferCount == 0 ? 0 : pendingTransferFrames[0];
    }

    public String pendingTransferName() {
        return pendingTransferCount == 0 ? "" : commandName(pendingTransferCommands[0]);
    }

    public int pendingTransferCount() {
        return pendingTransferCount;
    }

    public boolean isTransferMaskActive() {
        return transferMaskFrames > 0;
    }

    public boolean systemPalettesReady() {
        return systemPalettesReady;
    }

    public boolean shouldSuppressFrame() {
        return enabled && maskMode != 0;
    }

    public int borderTileNonZeroBytes() {
        return countNonZero(borderTileData);
    }

    public int pictureTransferNonZeroBytes() {
        return countNonZero(pictureTransferData);
    }

    public String borderTileDataSample() {
        return bytesHex(borderTileData, 0, 64);
    }

    public String pictureTransferMapSample() {
        return bytesHex(pictureTransferData, 0, 64);
    }

    public String pictureTransferPaletteSample() {
        return bytesHex(pictureTransferData, BORDER_PALETTE_OFFSET, BORDER_PALETTE_BYTES);
    }

    public String screenPalettesSample() {
        StringBuilder builder = new StringBuilder();
        for (int palette = 0; palette < screenPalettes.length; palette++) {
            if (palette > 0) {
                builder.append(" | ");
            }
            builder.append(palette).append(':');
            for (int color = 0; color < screenPalettes[palette].length; color++) {
                if (color > 0) {
                    builder.append(' ');
                }
                builder.append(String.format("%08X", screenPalettes[palette][color]));
            }
        }
        return builder.toString();
    }

    public int screenAttributeUniqueCount() {
        boolean[] seen = new boolean[4];
        int count = 0;
        for (byte attribute : screenAttributes) {
            int palette = attribute & 0x03;
            if (!seen[palette]) {
                seen[palette] = true;
                count++;
            }
        }
        return count;
    }

    public int pictureTransferUniqueMapEntries() {
        return uniqueMapEntries(pictureTransferData);
    }

    public void tickFrame() {
        if (!enabled) {
            return;
        }
        if (transferMaskFrames > 0) {
            transferMaskFrames--;
        }
        if (pendingMaskClear) {
            maskMode = 0;
            pendingMaskClear = false;
        }
    }

    public boolean consumeTransferFrame() {
        if (!enabled || pendingTransferCount == 0) {
            return false;
        }
        pendingTransferFrames[0]++;
        // mGBA starts sampling SGB transfer scanlines on the frame after the
        // command frame. This hook is called after a frame is completed, so the
        // first completion only arms the transfer and the second one copies the
        // first transfer frame's rendered pixels.
        if (pendingTransferFrames[0] == 2) {
            int transferCommand = pendingTransferCommands[0];
            int destination = pendingTransferDestinations[0];
            executeVramTransfer(transferCommand, destination);
        }
        if (pendingTransferFrames[0] <= SGB_TRANSFER_FRAMES) {
            return true;
        }
        removePendingTransfer();
        return true;
    }

    private void executeVramTransfer(int transferCommand, int destination) {
        if (transferCommand == COMMAND_CHR_TRN) {
            copyRenderedTransfer(borderTileData, destination, CHR_TRANSFER_SIZE);
        } else if (transferCommand == COMMAND_PCT_TRN) {
            copyRenderedTransfer(pictureTransferData, 0, PCT_TRANSFER_SIZE);
            buildBorderImage();
        } else if (transferCommand == COMMAND_PAL_TRN) {
            copyRenderedTransfer(systemPaletteData, 0, PCT_TRANSFER_SIZE);
            systemPalettesReady = true;
        } else if (transferCommand == COMMAND_ATTR_TRN) {
            copyRenderedTransfer(attributeFileData, 0, attributeFileData.length);
        }
    }

    private void completePacket() {
        byte[] packet = decodePacket();
        receivePacket(packet);
        pulseCount = -1;
        waitingStopBit = true;
    }

    private byte[] decodePacket() {
        byte[] packet = new byte[PACKET_BYTES];
        for (int bitIndex = 0; bitIndex < PACKET_BITS; bitIndex++) {
            int bit = pulseBuffer[bitIndex] & 0x01;
            packet[bitIndex / 8] |= (byte) (bit << (bitIndex & 7));
        }
        return packet;
    }

    private void receivePacket(byte[] packet) {
        lastHeaderByte = packet[0] & 0xFF;
        lastPacketHex = packetHex(packet);
        if (receivedPackets == 0 || expectedPackets == 0) {
            command = (packet[0] & 0xFF) >> 3;
            int length = packet[0] & 0x07;
            if (command > COMMAND_OBJ_TRN) {
                invalidPackets++;
                lastCommandName = "INVALID";
                expectedPackets = 0;
                receivedPackets = 0;
                return;
            }
            if (length == 0) {
                if (isZeroPacket(packet)) {
                    ignoredPackets++;
                    expectedPackets = 0;
                    receivedPackets = 0;
                    return;
                }
                invalidPackets++;
                lastCommandName = "INVALID";
                expectedPackets = 0;
                receivedPackets = 0;
                return;
            }
            expectedPackets = length;
            Arrays.fill(pendingCommand, (byte) 0);
        }

        int destination = receivedPackets * PACKET_BYTES;
        System.arraycopy(packet, 0, pendingCommand, destination, PACKET_BYTES);
        receivedPackets++;
        packetsReceived++;

        if (receivedPackets >= expectedPackets) {
            dispatchCommand();
            expectedPackets = 0;
            receivedPackets = 0;
        }
    }

    private void dispatchCommand() {
        lastCommandName = commandName(command);
        rememberCommand(lastCommandName);
        switch (command) {
            case COMMAND_PAL01 -> handlePalette01();
            case COMMAND_PAL23 -> handlePalette23();
            case COMMAND_PAL03 -> handlePalette03();
            case COMMAND_PAL12 -> handlePalette12();
            case COMMAND_ATTR_BLK -> handleAttributeBlock();
            case COMMAND_ATTR_LIN -> handleAttributeLine();
            case COMMAND_ATTR_DIV -> handleAttributeDivide();
            case COMMAND_ATTR_CHR -> handleAttributeCharacters();
            case COMMAND_PAL_SET -> handlePaletteSet();
            case COMMAND_PAL_TRN -> handlePaletteTransfer();
            case COMMAND_ATTR_TRN -> handleAttributeTransfer();
            case COMMAND_ATTR_SET -> handleAttributeSet();
            case COMMAND_MLT_REQ -> handleMultiplayerRequest();
            case COMMAND_CHR_TRN -> handleCharacterTransfer();
            case COMMAND_PCT_TRN -> handlePictureTransfer();
            case COMMAND_MASK_EN -> maskMode = pendingCommand[1] & 0x03;
            default -> {
                // Other SGB commands affect palettes, sound, or SNES-side state that this
                // first border implementation does not emulate yet.
            }
        }
    }

    private void handlePalette01() {
        writeScreenPaletteColor(0, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(1, readColor555(pendingCommand, 3));
        writeScreenPaletteColor(2, readColor555(pendingCommand, 5));
        writeScreenPaletteColor(3, readColor555(pendingCommand, 7));
        writeScreenPaletteColor(4, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(5, readColor555(pendingCommand, 9));
        writeScreenPaletteColor(6, readColor555(pendingCommand, 11));
        writeScreenPaletteColor(7, readColor555(pendingCommand, 13));
        writeScreenPaletteColor(8, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(12, readColor555(pendingCommand, 1));
    }

    private void handlePalette23() {
        writeScreenPaletteColor(9, readColor555(pendingCommand, 3));
        writeScreenPaletteColor(10, readColor555(pendingCommand, 5));
        writeScreenPaletteColor(11, readColor555(pendingCommand, 7));
        writeScreenPaletteColor(13, readColor555(pendingCommand, 9));
        writeScreenPaletteColor(14, readColor555(pendingCommand, 11));
        writeScreenPaletteColor(15, readColor555(pendingCommand, 13));
    }

    private void handlePalette03() {
        writeScreenPaletteColor(0, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(1, readColor555(pendingCommand, 3));
        writeScreenPaletteColor(2, readColor555(pendingCommand, 5));
        writeScreenPaletteColor(3, readColor555(pendingCommand, 7));
        writeScreenPaletteColor(4, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(8, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(12, readColor555(pendingCommand, 1));
        writeScreenPaletteColor(13, readColor555(pendingCommand, 9));
        writeScreenPaletteColor(14, readColor555(pendingCommand, 11));
        writeScreenPaletteColor(15, readColor555(pendingCommand, 13));
    }

    private void handlePalette12() {
        writeScreenPaletteColor(5, readColor555(pendingCommand, 3));
        writeScreenPaletteColor(6, readColor555(pendingCommand, 5));
        writeScreenPaletteColor(7, readColor555(pendingCommand, 7));
        writeScreenPaletteColor(9, readColor555(pendingCommand, 9));
        writeScreenPaletteColor(10, readColor555(pendingCommand, 11));
        writeScreenPaletteColor(11, readColor555(pendingCommand, 13));
    }

    private void handlePaletteSet() {
        int flags = pendingCommand[9] & 0xFF;
        if ((flags & 0x80) != 0) {
            loadAttributeSet(flags & 0x3F);
        }
        if ((flags & 0x40) != 0) {
            pendingMaskClear = true;
        }
        if (!systemPalettesReady) {
            return;
        }
        for (int palette = 0; palette < 4; palette++) {
            int offset = 1 + palette * 2;
            int paletteId = (pendingCommand[offset] & 0xFF) | ((pendingCommand[offset + 1] & 0xFF) << 8);
            copySystemPaletteToScreenPalette(paletteId, palette);
        }
    }

    private void handlePaletteTransfer() {
        scheduleVramTransfer(COMMAND_PAL_TRN, 0);
    }

    private void handleAttributeTransfer() {
        scheduleVramTransfer(COMMAND_ATTR_TRN, 0);
    }

    private void handleAttributeSet() {
        loadAttributeSet(pendingCommand[1] & 0x3F);
        if ((pendingCommand[1] & 0x40) != 0) {
            pendingMaskClear = true;
        }
    }

    private void handleAttributeBlock() {
        int dataSets = pendingCommand[1] & 0xFF;
        int offset = 2;
        for (int i = 0; i < dataSets && offset + 5 < pendingCommand.length; i++, offset += 6) {
            int control = pendingCommand[offset] & 0x07;
            int palettes = pendingCommand[offset + 1] & 0x3F;
            int x1 = Math.max(0, Math.min(19, pendingCommand[offset + 2] & 0xFF));
            int y1 = Math.max(0, Math.min(17, pendingCommand[offset + 3] & 0xFF));
            int x2 = Math.max(0, Math.min(19, pendingCommand[offset + 4] & 0xFF));
            int y2 = Math.max(0, Math.min(17, pendingCommand[offset + 5] & 0xFF));
            int insidePalette = palettes & 0x03;
            int borderPalette = (palettes >> 2) & 0x03;
            int outsidePalette = (palettes >> 4) & 0x03;
            applyAttributeBlock(control, x1, y1, x2, y2, insidePalette, borderPalette, outsidePalette);
        }
    }

    private void handleAttributeLine() {
        int dataSets = pendingCommand[1] & 0xFF;
        for (int i = 0; i < dataSets && 2 + i < pendingCommand.length; i++) {
            int data = pendingCommand[2 + i] & 0xFF;
            int line = data & 0x1F;
            int palette = (data >> 5) & 0x03;
            boolean horizontal = (data & 0x80) != 0;
            if (horizontal) {
                if (line >= 18) {
                    continue;
                }
                for (int x = 0; x < 20; x++) {
                    screenAttributes[line * 20 + x] = (byte) palette;
                }
            } else {
                if (line >= 20) {
                    continue;
                }
                for (int y = 0; y < 18; y++) {
                    screenAttributes[y * 20 + line] = (byte) palette;
                }
            }
        }
    }

    private void handleAttributeDivide() {
        int data = pendingCommand[1] & 0xFF;
        int line = pendingCommand[2] & 0xFF;
        int firstPalette = (data >> 2) & 0x03;
        int linePalette = (data >> 4) & 0x03;
        int secondPalette = data & 0x03;
        boolean horizontal = (data & 0x40) != 0;
        if (horizontal) {
            line = Math.max(0, Math.min(17, line));
            for (int y = 0; y < 18; y++) {
                int palette = y < line ? firstPalette : y == line ? linePalette : secondPalette;
                for (int x = 0; x < 20; x++) {
                    screenAttributes[y * 20 + x] = (byte) palette;
                }
            }
        } else {
            line = Math.max(0, Math.min(19, line));
            for (int y = 0; y < 18; y++) {
                for (int x = 0; x < 20; x++) {
                    int palette = x < line ? firstPalette : x == line ? linePalette : secondPalette;
                    screenAttributes[y * 20 + x] = (byte) palette;
                }
            }
        }
    }

    private void handleAttributeCharacters() {
        int x = pendingCommand[1] & 0xFF;
        int y = pendingCommand[2] & 0xFF;
        if (x >= 20) {
            x = 0;
        }
        if (y >= 18) {
            y = 0;
        }
        int dataSets = (pendingCommand[3] & 0xFF) | ((pendingCommand[4] & 0xFF) << 8);
        boolean vertical = (pendingCommand[5] & 0x01) != 0;
        int currentX = x;
        int currentY = y;
        for (int set = 0; set < dataSets; set++) {
            int packedOffset = 6 + set / 4;
            if (packedOffset >= pendingCommand.length) {
                break;
            }
            int shift = 6 - (set % 4) * 2;
            int palette = ((pendingCommand[packedOffset] & 0xFF) >> shift) & 0x03;
            if (currentX >= 0 && currentX < 20 && currentY >= 0 && currentY < 18) {
                screenAttributes[currentY * 20 + currentX] = (byte) palette;
            }
            if (vertical) {
                currentY++;
                if (currentY >= 18) {
                    currentY = 0;
                    currentX++;
                }
                if (currentX >= 20) {
                    currentX = 0;
                }
            } else {
                currentX++;
                if (currentX >= 20) {
                    currentX = 0;
                    currentY++;
                }
                if (currentY >= 18) {
                    currentY = 0;
                }
            }
        }
    }

    private void handleMultiplayerRequest() {
        int control = pendingCommand[1] & 0x03;
        if (control == 0x02) {
            selectedJoypad++;
        }
        joypadCount = switch (control) {
            case 1 -> 2;
            case 3 -> 4;
            default -> 1;
        };
        selectedJoypad &= control;
    }

    private void applyAttributeBlock(int control, int x1, int y1, int x2, int y2, int insidePalette, int borderPalette, int outsidePalette) {
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 20; x++) {
                if (y > y1 && y < y2 && x > x1 && x < x2) {
                    if ((control & 0x01) != 0) {
                        screenAttributes[y * 20 + x] = (byte) insidePalette;
                    }
                } else if (y < y1 || y > y2 || x < x1 || x > x2) {
                    if ((control & 0x04) != 0) {
                        screenAttributes[y * 20 + x] = (byte) outsidePalette;
                    }
                } else if ((control & 0x02) != 0) {
                    screenAttributes[y * 20 + x] = (byte) borderPalette;
                } else if ((control & 0x01) != 0) {
                    screenAttributes[y * 20 + x] = (byte) insidePalette;
                } else if ((control & 0x04) != 0) {
                    screenAttributes[y * 20 + x] = (byte) outsidePalette;
                }
            }
        }
    }

    private void handleCharacterTransfer() {
        int destination = (pendingCommand[1] & 0x01) * CHR_TRANSFER_SIZE;
        scheduleVramTransfer(COMMAND_CHR_TRN, destination);
    }

    private void handlePictureTransfer() {
        scheduleVramTransfer(COMMAND_PCT_TRN, 0);
    }

    private void scheduleVramTransfer(int transferCommand, int destination) {
        if (pendingTransferCount >= MAX_PENDING_TRANSFERS) {
            removePendingTransfer();
        }
        int index = pendingTransferCount++;
        pendingTransferCommands[index] = transferCommand;
        pendingTransferDestinations[index] = destination;
        pendingTransferFrames[index] = 0;
        transferMaskFrames = Math.max(transferMaskFrames, SGB_TRANSFER_FRAMES + 1);
    }

    private void copyRenderedTransfer(byte[] target, int targetOffset, int length) {
        int maxLength = Math.min(length, target.length - targetOffset);
        for (int y = 0; y < 144; y++) {
            int offset = 2 * ((y & 0x07) + (y >> 3) * 160);
            if (offset >= maxLength) {
                return;
            }
            for (int x = 0; x < 160; x += 8) {
                int byteOffset = offset + (x << 1);
                if (byteOffset + 1 >= maxLength) {
                    break;
                }
                int low = 0;
                int high = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int color = ppu.getBackgroundColorIndex(x + bit, y);
                    int shift = 7 - bit;
                    low |= (color & 0x01) << shift;
                    high |= ((color & 0x02) >> 1) << shift;
                }
                target[targetOffset + byteOffset] = (byte) low;
                target[targetOffset + byteOffset + 1] = (byte) high;
            }
        }
    }

    private boolean validTransferCommand(int transferCommand) {
        return transferCommand == COMMAND_CHR_TRN
                || transferCommand == COMMAND_PCT_TRN
                || transferCommand == COMMAND_PAL_TRN
                || transferCommand == COMMAND_ATTR_TRN;
    }

    private void removePendingTransfer() {
        if (pendingTransferCount <= 0) {
            return;
        }
        int remaining = pendingTransferCount - 1;
        if (remaining > 0) {
            System.arraycopy(pendingTransferCommands, 1, pendingTransferCommands, 0, remaining);
            System.arraycopy(pendingTransferDestinations, 1, pendingTransferDestinations, 0, remaining);
            System.arraycopy(pendingTransferFrames, 1, pendingTransferFrames, 0, remaining);
        }
        pendingTransferCommands[remaining] = 0;
        pendingTransferDestinations[remaining] = 0;
        pendingTransferFrames[remaining] = 0;
        pendingTransferCount = remaining;
    }

    private int uniqueMapEntries(byte[] data) {
        int count = 0;
        boolean[] seen = new boolean[65536];
        for (int offset = 0; offset + 1 < BORDER_MAP_BYTES; offset += 2) {
            int entry = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            if (!seen[entry]) {
                seen[entry] = true;
                count++;
            }
        }
        return count;
    }

    private void buildBorderImage() {
        BufferedImage image = new BufferedImage(BORDER_WIDTH, BORDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int tileY = 0; tileY < 28; tileY++) {
            for (int tileX = 0; tileX < 32; tileX++) {
                int mapOffset = (tileY * 32 + tileX) * 2;
                if (mapOffset + 1 >= BORDER_MAP_BYTES) {
                    continue;
                }
                int entry = (pictureTransferData[mapOffset] & 0xFF) | ((pictureTransferData[mapOffset + 1] & 0xFF) << 8);
                drawBorderTile(image, tileX * 8, tileY * 8, entry);
            }
        }
        borderImage = image;
    }

    private void copySystemPaletteToScreenPalette(int paletteId, int screenPalette) {
        int base = paletteId * 8;
        if (base < 0 || base + 7 >= systemPaletteData.length || screenPalette < 0 || screenPalette >= 4) {
            return;
        }
        for (int color = 0; color < 4; color++) {
            writeScreenPaletteColor(screenPalette * 4 + color, readColor555(systemPaletteData, base + color * 2));
        }
    }

    private void writeScreenPaletteColor(int colorSlot, int argb) {
        if (colorSlot < 0 || colorSlot >= 16) {
            return;
        }
        if (colorSlot != 0 && (colorSlot & 0x03) == 0) {
            argb = screenPalettes[0][0];
        }
        screenPalettes[colorSlot >> 2][colorSlot & 0x03] = argb;
        if (colorSlot == 0) {
            screenPalettes[1][0] = argb;
            screenPalettes[2][0] = argb;
            screenPalettes[3][0] = argb;
            if (borderImage != null) {
                buildBorderImage();
            }
        }
    }

    private void loadAttributeSet(int set) {
        if (set > 0x2C) {
            return;
        }
        int offset = set * 90;
        if (offset + 90 > attributeFileData.length) {
            return;
        }
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 20; x++) {
                int packed = attributeFileData[offset + (x >> 2) + y * 5] & 0xFF;
                int shift = 2 * (3 - (x & 0x03));
                screenAttributes[y * 20 + x] = (byte) ((packed >> shift) & 0x03);
            }
        }
    }

    private void drawBorderTile(BufferedImage image, int baseX, int baseY, int entry) {
        int tileIndex = entry & 0xFF;
        int palette = (entry >> 10) & 0x07;
        boolean hFlip = (entry & 0x4000) != 0;
        boolean vFlip = (entry & 0x8000) != 0;
        int tileOffset = tileIndex * BORDER_TILE_SIZE;
        for (int y = 0; y < 8; y++) {
            int sourceY = vFlip ? 7 - y : y;
            int low = borderTileData[tileOffset + sourceY * 2] & 0xFF;
            int high = borderTileData[tileOffset + sourceY * 2 + 1] & 0xFF;
            int extraLow = borderTileData[tileOffset + 16 + sourceY * 2] & 0xFF;
            int extraHigh = borderTileData[tileOffset + 16 + sourceY * 2 + 1] & 0xFF;
            for (int x = 0; x < 8; x++) {
                int targetX = baseX + x;
                int targetY = baseY + y;
                int sourceX = hFlip ? x : 7 - x;
                int colorIndex = ((low >> sourceX) & 1)
                        | (((high >> sourceX) & 1) << 1)
                        | (((extraLow >> sourceX) & 1) << 2)
                        | (((extraHigh >> sourceX) & 1) << 3);
                image.setRGB(targetX, targetY, paletteColor(palette, colorIndex));
            }
        }
    }

    private int paletteColor(int palette, int colorIndex) {
        if ((colorIndex & 0x0F) == 0) {
            return screenPalettes[0][0];
        }
        int paletteSlot = Math.max(0, Math.min(3, palette - 4));
        int offset = BORDER_PALETTE_OFFSET + paletteSlot * 32 + colorIndex * 2;
        if (offset + 1 >= BORDER_PALETTE_OFFSET + BORDER_PALETTE_BYTES) {
            return 0xFF000000;
        }
        int rgb555 = (pictureTransferData[offset] & 0xFF) | ((pictureTransferData[offset + 1] & 0xFF) << 8);
        int red = expand5To8(rgb555 & 0x1F);
        int green = expand5To8((rgb555 >> 5) & 0x1F);
        int blue = expand5To8((rgb555 >> 10) & 0x1F);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private int expand5To8(int value) {
        return (value << 3) | (value >> 2);
    }

    private void reset() {
        Arrays.fill(pulseBuffer, 0);
        Arrays.fill(pendingCommand, (byte) 0);
        Arrays.fill(borderTileData, (byte) 0);
        Arrays.fill(pictureTransferData, (byte) 0);
        Arrays.fill(systemPaletteData, (byte) 0);
        Arrays.fill(attributeFileData, (byte) 0);
        Arrays.fill(pendingTransferCommands, 0);
        Arrays.fill(pendingTransferDestinations, 0);
        Arrays.fill(pendingTransferFrames, 0);
        initializeScreenPalettes();
        previousLines = JOYP_START;
        pulseCount = -1;
        command = 0;
        expectedPackets = 0;
        receivedPackets = 0;
        maskMode = 0;
        joypadCount = 1;
        selectedJoypad = 0;
        receivingPacket = false;
        waitingStopBit = false;
        lastHeaderByte = 0;
        lastPacketHex = "";
        pendingTransferCount = 0;
        transferMaskFrames = 0;
        pendingMaskClear = false;
        systemPalettesReady = false;
        packetsReceived = 0;
        invalidPackets = 0;
        ignoredPackets = 0;
        borderImage = null;
        lastCommandName = "";
        Arrays.fill(recentCommandNames, null);
        recentCommandCursor = 0;
    }

    private void copy(int[] source, int[] target) {
        Arrays.fill(target, 0);
        if (source != null) {
            System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
        }
    }

    private void copy(byte[] source, byte[] target) {
        Arrays.fill(target, (byte) 0);
        if (source != null) {
            System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
        }
    }

    private void initializeScreenPalettes() {
        int[] gray = {0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF000000};
        for (int palette = 0; palette < screenPalettes.length; palette++) {
            System.arraycopy(gray, 0, screenPalettes[palette], 0, gray.length);
        }
        Arrays.fill(screenAttributes, (byte) 0);
    }

    private int[][] copyScreenPalettes() {
        int[][] copy = new int[screenPalettes.length][screenPalettes[0].length];
        for (int i = 0; i < screenPalettes.length; i++) {
            System.arraycopy(screenPalettes[i], 0, copy[i], 0, screenPalettes[i].length);
        }
        return copy;
    }

    private void restoreScreenPalettes(int[][] palettes) {
        initializeScreenPalettes();
        if (palettes == null) {
            return;
        }
        for (int palette = 0; palette < Math.min(palettes.length, screenPalettes.length); palette++) {
            if (palettes[palette] == null) {
                continue;
            }
            System.arraycopy(palettes[palette], 0, screenPalettes[palette], 0, Math.min(palettes[palette].length, screenPalettes[palette].length));
        }
    }

    private int readColor555(byte[] data, int offset) {
        if (offset + 1 >= data.length) {
            return 0xFF000000;
        }
        int color = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        return color555ToArgb(color);
    }

    private int color555ToArgb(int color) {
        int red = color & 0x1F;
        int green = (color >> 5) & 0x1F;
        int blue = (color >> 10) & 0x1F;
        return 0xFF000000 | (expand5To8(red) << 16) | (expand5To8(green) << 8) | expand5To8(blue);
    }

    private void restorePendingTransfers(SuperGameBoyState state) {
        Arrays.fill(pendingTransferCommands, 0);
        Arrays.fill(pendingTransferDestinations, 0);
        Arrays.fill(pendingTransferFrames, 0);
        int[] commands = state.pendingTransferCommands();
        int[] destinations = state.pendingTransferDestinations();
        int[] frames = state.pendingTransferFrames();
        if (commands == null || destinations == null || frames == null) {
            pendingTransferCount = 0;
            return;
        }
        int count = Math.min(
                Math.min(Math.max(0, state.pendingTransferCount()), MAX_PENDING_TRANSFERS),
                Math.min(commands.length, Math.min(destinations.length, frames.length))
        );
        pendingTransferCount = 0;
        for (int i = 0; i < count; i++) {
            if (!validTransferCommand(commands[i])) {
                continue;
            }
            int targetIndex = pendingTransferCount++;
            pendingTransferCommands[targetIndex] = commands[i];
            pendingTransferDestinations[targetIndex] = Math.max(0, Math.min(destinations[i], CHR_TRANSFER_SIZE));
            pendingTransferFrames[targetIndex] = Math.max(0, frames[i]);
        }
    }

    private String commandName(int command) {
        return switch (command) {
            case 0x00 -> "PAL01";
            case 0x01 -> "PAL23";
            case 0x02 -> "PAL03";
            case 0x03 -> "PAL12";
            case 0x04 -> "ATTR_BLK";
            case 0x05 -> "ATTR_LIN";
            case 0x06 -> "ATTR_DIV";
            case 0x07 -> "ATTR_CHR";
            case 0x08 -> "SOUND";
            case 0x09 -> "SOU_TRN";
            case COMMAND_PAL_SET -> "PAL_SET";
            case COMMAND_PAL_TRN -> "PAL_TRN";
            case 0x0C -> "ATRC_EN";
            case 0x0D -> "TEST_EN";
            case 0x0E -> "ICON_EN";
            case 0x0F -> "DATA_SND";
            case 0x10 -> "DATA_TRN";
            case COMMAND_MLT_REQ -> "MLT_REQ";
            case 0x12 -> "JUMP";
            case COMMAND_CHR_TRN -> "CHR_TRN";
            case COMMAND_PCT_TRN -> "PCT_TRN";
            case 0x15 -> "ATTR_TRN";
            case 0x16 -> "ATTR_SET";
            case COMMAND_MASK_EN -> "MASK_EN";
            case COMMAND_OBJ_TRN -> "OBJ_TRN";
            default -> String.format("SGB_%02X", command);
        };
    }

    private void rememberCommand(String commandName) {
        recentCommandNames[recentCommandCursor] = commandName;
        recentCommandCursor = (recentCommandCursor + 1) % recentCommandNames.length;
    }

    private String packetHex(byte[] packet) {
        StringBuilder builder = new StringBuilder(packet.length * 2);
        for (byte value : packet) {
            builder.append(String.format("%02X", value & 0xFF));
        }
        return builder.toString();
    }

    private boolean isZeroPacket(byte[] packet) {
        for (byte value : packet) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private int countNonZero(byte[] data) {
        int count = 0;
        for (byte value : data) {
            if (value != 0) {
                count++;
            }
        }
        return count;
    }

    private String bytesHex(byte[] data, int offset, int length) {
        StringBuilder builder = new StringBuilder(length * 3);
        int end = Math.min(data.length, offset + length);
        for (int i = offset; i < end; i++) {
            if (i > offset) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", data[i] & 0xFF));
        }
        return builder.toString();
    }
}
