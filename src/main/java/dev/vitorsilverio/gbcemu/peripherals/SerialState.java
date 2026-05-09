package dev.vitorsilverio.gbcemu.peripherals;

import java.io.Serializable;

public record SerialState(
        int sb,
        int sc,
        int transferCyclesRemaining,
        int outgoingByte,
        String pendingText
) implements Serializable {
}
