package dev.vitorsilverio.gbcemu.audio;

import java.io.Serializable;

public record SoundChannelState(
        boolean enabled,
        int lengthTimer,
        int currentVolume,
        int envelopeTimer,
        int timer
) implements Serializable {
}
