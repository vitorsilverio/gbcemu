package dev.vitorsilverio.gbcemu.audio;

public interface AudioSampleOutput {
    void writeStereoSample(int left, int right);

    void writeSilentSample();

    default void updateChannelState(int channel, boolean enabled, double frequencyHz, int volume, boolean noise) {
    }

    default boolean suppressPcmOutput() {
        return false;
    }

    void restoreHighPassFilter(int leftCapacitor, int rightCapacitor);

    int previousLeftSample();

    int previousRightSample();

    default int bufferedSampleBytes() {
        return 0;
    }

    default int lowPassAlpha() {
        return 0;
    }

    default void setLowPassAlpha(int lowPassAlpha) {
    }

    default String debugDescription() {
        return "External audio output";
    }

    default void close() {
    }
}
