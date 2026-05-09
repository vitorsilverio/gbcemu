package dev.vitorsilverio.gbcemu.memory;

import java.io.Serializable;

public record ZeroPageState(byte[] memory) implements Serializable {
}
