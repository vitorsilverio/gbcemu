package dev.vitorsilverio.gbcemu.audio;

public record ApuChannelDebugSnapshot(
        int channel,
        String name,
        boolean enabled,
        boolean dacEnabled,
        int period,
        double frequencyHz,
        int lengthTimer,
        int currentVolume,
        int envelopeTimer,
        int timer,
        int digitalOutput,
        int analogOutput,
        int sequencerPosition,
        int extra,
        String detail
) {
}
