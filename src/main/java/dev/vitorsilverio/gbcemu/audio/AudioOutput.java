package dev.vitorsilverio.gbcemu.audio;

final class AudioOutput {
    private static final int CGB_HIGH_PASS_FACTOR = 912;
    private static final int HIGH_PASS_DIVISOR = 1000;
    private static final int LOW_PASS_SHIFT = 1;
    private static final int PCM_SCALE = 96;

    private final AudioSink sink;
    private final byte[] sampleBuffer = new byte[1024];
    private int sampleBufferPosition;
    private int leftCapacitor;
    private int rightCapacitor;
    private int previousLeftOutput;
    private int previousRightOutput;

    AudioOutput(AudioSink sink) {
        this.sink = sink;
    }

    void restoreHighPassFilter(int leftCapacitor, int rightCapacitor) {
        this.leftCapacitor = leftCapacitor;
        this.rightCapacitor = rightCapacitor;
        sampleBufferPosition = 0;
        previousLeftOutput = 0;
        previousRightOutput = 0;
    }

    void reset() {
        sampleBufferPosition = 0;
        leftCapacitor = 0;
        rightCapacitor = 0;
        previousLeftOutput = 0;
        previousRightOutput = 0;
    }

    void close() {
        sampleBufferPosition = 0;
        sink.close();
    }

    void writeStereoSample(int left, int right) {
        int filteredLeft = highPassLeft(clampSample(left));
        int filteredRight = highPassRight(clampSample(right));
        previousLeftOutput += (filteredLeft - previousLeftOutput) >> LOW_PASS_SHIFT;
        previousRightOutput += (filteredRight - previousRightOutput) >> LOW_PASS_SHIFT;
        putPcm16(clampPcm(previousLeftOutput));
        putPcm16(clampPcm(previousRightOutput));
    }

    void writeSilentSample() {
        writeStereoSample(0, 0);
    }

    int previousLeftSample() {
        return leftCapacitor;
    }

    int previousRightSample() {
        return rightCapacitor;
    }

    int bufferedSampleBytes() {
        return sampleBufferPosition;
    }

    private int clampSample(int value) {
        int scaled = value * PCM_SCALE;
        return clampPcm(scaled);
    }

    private int clampPcm(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    private int highPassLeft(int sample) {
        int filtered = sample - leftCapacitor;
        leftCapacitor = sample - filtered * CGB_HIGH_PASS_FACTOR / HIGH_PASS_DIVISOR;
        return filtered;
    }

    private int highPassRight(int sample) {
        int filtered = sample - rightCapacitor;
        rightCapacitor = sample - filtered * CGB_HIGH_PASS_FACTOR / HIGH_PASS_DIVISOR;
        return filtered;
    }

    private void putPcm16(int sample) {
        sampleBuffer[sampleBufferPosition++] = (byte) (sample & 0xFF);
        sampleBuffer[sampleBufferPosition++] = (byte) ((sample >> 8) & 0xFF);
        if (sampleBufferPosition == sampleBuffer.length) {
            sink.write(sampleBuffer, sampleBufferPosition);
            sampleBufferPosition = 0;
        }
    }
}
