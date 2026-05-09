package dev.vitorsilverio.gbcemu.snapshot;

import java.io.Serializable;
import java.time.Instant;

public record SaveStateMetadata(
        Instant createdAt,
        String romTitle,
        String romPath,
        String cartridgeType,
        long frameNumber,
        int pc,
        int[] previewArgb,
        int previewWidth,
        int previewHeight
) implements Serializable {
}
