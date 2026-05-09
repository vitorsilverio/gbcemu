package dev.vitorsilverio.gbcemu.cartridge;

import java.io.Serializable;

public record CartridgeRamState(byte[] data, int currentBank) implements Serializable {
}
