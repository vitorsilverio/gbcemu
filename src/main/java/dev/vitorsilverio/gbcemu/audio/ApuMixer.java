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

        int left = 0;
        int right = 0;
        if ((nr51 & 0x10) != 0) {
            left += channel1;
        }
        if ((nr51 & 0x20) != 0) {
            left += channel2;
        }
        if ((nr51 & 0x40) != 0) {
            left += channel3;
        }
        if ((nr51 & 0x80) != 0) {
            left += channel4;
        }
        if ((nr51 & 0x01) != 0) {
            right += channel1;
        }
        if ((nr51 & 0x02) != 0) {
            right += channel2;
        }
        if ((nr51 & 0x04) != 0) {
            right += channel3;
        }
        if ((nr51 & 0x08) != 0) {
            right += channel4;
        }

        output.writeStereoSample(
                left * leftVolume * masterVolume * leftScale / 10_000,
                right * rightVolume * masterVolume * rightScale / 10_000
        );
    }
}
