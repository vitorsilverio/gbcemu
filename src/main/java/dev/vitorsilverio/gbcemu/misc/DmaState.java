package dev.vitorsilverio.gbcemu.misc;

import java.io.Serializable;

public record DmaState(
        int cycles,
        int baseAddress,
        boolean active
) implements Serializable {
}
