package dev.vitorsilverio.gbcemu.audio;

import java.io.Serializable;

public record WaveChannelState(
        SoundChannelState common,
        int period,
        int sampleIndex
) implements Serializable {
}
