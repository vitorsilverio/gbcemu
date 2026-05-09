package dev.vitorsilverio.gbcemu.audio;

final class AudioOutput {
    private final AudioSink sink;
    private final byte[] sampleBuffer = new byte[1024];
    private int sampleBufferPosition;
    private int previousLeftSample;
    private int previousRightSample;

    AudioOutput(AudioSink sink) {
        this.sink = sink;
    }

    void restoreSmoothing(int previousLeftSample, int previousRightSample) {
        this.previousLeftSample = previousLeftSample;
        this.previousRightSample = previousRightSample;
        sampleBufferPosition = 0;
    }

    void reset() {
        sampleBufferPosition = 0;
        previousLeftSample = 0;
        previousRightSample = 0;
    }

    void writeStereoSample(int left, int right) {
        previousLeftSample = smoothSample(previousLeftSample, clampSample(left));
        previousRightSample = smoothSample(previousRightSample, clampSample(right));
        putPcm16(previousLeftSample);
        putPcm16(previousRightSample);
    }

    int previousLeftSample() {
        return previousLeftSample;
    }

    int previousRightSample() {
        return previousRightSample;
    }

    int bufferedSampleBytes() {
        return sampleBufferPosition;
    }

    private int clampSample(int value) {
        int scaled = value * 128;
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled));
    }

    private int smoothSample(int previous, int current) {
        return previous + ((current - previous) >> 2);
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
