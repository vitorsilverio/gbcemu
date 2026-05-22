package dev.vitorsilverio.gbcemu.sgb;

import java.io.Serializable;

public record SuperGameBoyState(
        boolean enabled,
        int[] pulseBuffer,
        byte[] pendingCommand,
        byte[] borderTileData,
        byte[] pictureTransferData,
        byte[] systemPaletteData,
        byte[] attributeFileData,
        int[][] screenPalettes,
        byte[] screenAttributes,
        int previousLines,
        int pulseCount,
        int command,
        int expectedPackets,
        int receivedPackets,
        int maskMode,
        int joypadCount,
        int selectedJoypad,
        boolean receivingPacket,
        boolean waitingStopBit,
        int lastHeaderByte,
        String lastPacketHex,
        int[] pendingTransferCommands,
        int[] pendingTransferDestinations,
        int[] pendingTransferFrames,
        int pendingTransferCount,
        int transferMaskFrames,
        boolean pendingMaskClear,
        boolean systemPalettesReady,
        long packetsReceived,
        long invalidPackets,
        long ignoredPackets,
        boolean borderReady,
        String lastCommandName
) implements Serializable {
}
