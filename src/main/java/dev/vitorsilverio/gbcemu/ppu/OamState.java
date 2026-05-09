package dev.vitorsilverio.gbcemu.ppu;

import java.io.Serializable;

public record OamState(byte[] data) implements Serializable {
}
