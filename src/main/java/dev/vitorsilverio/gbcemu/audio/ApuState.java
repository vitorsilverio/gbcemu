package dev.vitorsilverio.gbcemu.audio;

import java.io.Serializable;

public record ApuState(
        byte[] registers,
        byte[] wavePatternRam,
        int sampleAccumulator,
        int frameSequencerStep,
        int previousLeftSample,
        int previousRightSample,
        boolean audioEnabled,
        PulseChannelState channel1,
        PulseChannelState channel2,
        WaveChannelState channel3,
        NoiseChannelState channel4
) implements Serializable {
}
