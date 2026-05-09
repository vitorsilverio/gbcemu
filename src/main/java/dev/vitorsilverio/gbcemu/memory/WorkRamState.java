package dev.vitorsilverio.gbcemu.memory;

import java.io.Serializable;

public record WorkRamState(int bank, byte[] bank0, byte[][] banks) implements Serializable {
}
