package dev.vitorsilverio.gbcemu.snapshot;

import java.io.Serializable;

public record EmulatorState(
        int version,
        Snapshot interruptManager,
        Snapshot bios,
        Snapshot cpu,
        Snapshot timer,
        Snapshot ppu,
        Snapshot videoRam,
        Snapshot oam,
        Snapshot serial,
        Snapshot hdma,
        Snapshot dma,
        Snapshot workRam,
        Snapshot zeroPage,
        Snapshot cart,
        Snapshot key0,
        Snapshot key1,
        Snapshot infrared
) implements Serializable {
    public static final int CURRENT_VERSION = 1;
}
