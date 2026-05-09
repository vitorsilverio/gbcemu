package dev.vitorsilverio.gbcemu.snapshot;

import java.io.Serializable;
import java.util.List;

public record SaveStateFile(
        int formatVersion,
        SaveStateMetadata metadata,
        List<Snapshot> snapshots
) implements Serializable {
    public static final int CURRENT_FORMAT_VERSION = 1;
}
