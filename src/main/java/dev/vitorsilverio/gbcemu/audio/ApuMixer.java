package dev.vitorsilverio.gbcemu.audio;

final class ApuMixer {
    private final AudioOutput output;

    ApuMixer(AudioOutput output) {
        this.output = output;
    }

    void writeSample(
            int nr50,
            int nr51,
            int channel1,
            int channel2,
            int channel3,
            int channel4,
            int masterVolume,
            int leftScale,
            int rightScale
    ) {
        int leftVolume = ((nr50 >> 4) & 0x07) + 1;
        int rightVolume = (nr50 & 0x07) + 1;
        int[] outputs = {channel1, channel2, channel3, channel4};

        int left = 0;
        int right = 0;
        for (int i = 0; i < outputs.length; i++) {
            if ((nr51 & (1 << (i + 4))) != 0) {
                left += outputs[i];
            }
            if ((nr51 & (1 << i)) != 0) {
                right += outputs[i];
            }
        }

        output.writeStereoSample(
                left * leftVolume * masterVolume * leftScale / 10_000,
                right * rightVolume * masterVolume * rightScale / 10_000
        );
    }
}
