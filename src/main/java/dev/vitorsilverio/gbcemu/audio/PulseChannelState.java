package dev.vitorsilverio.gbcemu.audio;

import java.io.Serializable;

public record PulseChannelState(
        SoundChannelState common,
        int period,
        int dutyStep,
        int sweepShadowPeriod,
        int sweepTimer,
        boolean sweepEnabled,
        boolean sweepNegateUsed
) implements Serializable {
}
