package dev.vitorsilverio.gbcemu.ppu;

import java.io.Serializable;

public record VideoRamState(
        int bank,
        byte[][][] tileData,
        byte[] tileMapIndexes,
        byte[] tileMapAttributes
) implements Serializable {
}
