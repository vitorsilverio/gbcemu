package dev.vitorsilverio.gbcemu.audio;

import java.io.Serializable;

public record NoiseChannelState(
        SoundChannelState common,
        int lfsr
) implements Serializable {
}
