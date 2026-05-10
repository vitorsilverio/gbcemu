package dev.vitorsilverio.gbcemu.audio;

import java.util.List;

public record ApuDebugSnapshot(
        boolean audioEnabled,
        int frameSequencerStep,
        int sampleRate,
        int sampleAccumulator,
        int bufferedSampleBytes,
        int nr50,
        int nr51,
        int nr52,
        int lowPassAlpha,
        String audioSink,
        ApuChannelDebugSnapshot channel1,
        ApuChannelDebugSnapshot channel2,
        ApuChannelDebugSnapshot channel3,
        ApuChannelDebugSnapshot channel4,
        List<ApuRegisterWrite> recentWrites
) {
}
