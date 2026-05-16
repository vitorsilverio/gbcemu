package dev.vitorsilverio.gbcemu.misc;

import java.io.Serializable;

public record HdmaState(
        boolean active,
        int total,
        int sourceAddress,
        int destinationAddress,
        int mode,
        int cycles,
        int counter,
        boolean completed,
        boolean hblankBlockTransferred
) implements Serializable {
}
