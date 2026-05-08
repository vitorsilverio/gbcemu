package dev.vitorsilverio.gbcemu.snapshot;

import java.util.Map;

public record Snapshot(
        String className,
        int version,
        Map<String, Object> state
) {
}
