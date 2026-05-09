package dev.vitorsilverio.gbcemu.snapshot;

import java.io.Serializable;

public record SaveStateFile(
        int formatVersion,
        SaveStateMetadata metadata,
        EmulatorState state
) implements Serializable {
    public static final int CURRENT_FORMAT_VERSION = 1;
}
