package dev.vitorsilverio.gbcemu.gui.audio;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.core.ConsoleAudioOutput;

public final class AudioOutput implements ConsoleAudioOutput {
    private static final int CGB_HIGH_PASS_FACTOR = 912;
    private static final int HIGH_PASS_DIVISOR = 1000;
    private static final int FILTER_DIVISOR = 1000;
    private static final int PCM_SCALE = 48;

    private final int sampleRate;
    private volatile AudioSink sink;
    private final AudioEnhancer enhancer = new AudioEnhancer();
    private final SoundFontSynth soundFontSynth = new SoundFontSynth();
    private final int[] enhancedSample = new int[2];
    private final byte[] sampleBuffer = new byte[1024];
    private int sampleBufferPosition;
    private int leftCapacitor;
    private int rightCapacitor;
    private int previousLeftOutput;
    private int previousRightOutput;
    private int lowPassAlpha = 350;
    private boolean sinkMuted;
    private AppSettings.AudioEnhancementConfig currentConfig;

    AudioOutput(int sampleRate, AudioSink sink) {
        this.sampleRate = sampleRate;
        this.sink = sink;
    }

    public static AudioOutput createDefault(int sampleRate, AppSettings.AudioEnhancementConfig config) {
        AudioOutput output = new AudioOutput(sampleRate, AudioSinkFactory.createDefault(sampleRate, config));
        output.currentConfig = config;
        return output;
    }

    public static AudioOutput muted() {
        return new AudioOutput(48_000, (buffer, length) -> {
        });
    }

    public static java.util.List<String> outputDeviceNames(int sampleRate) {
        return AudioSinkFactory.outputDeviceNames(sampleRate);
    }

    @Override
    public void restoreHighPassFilter(int leftCapacitor, int rightCapacitor) {
        this.leftCapacitor = leftCapacitor;
        this.rightCapacitor = rightCapacitor;
        sampleBufferPosition = 0;
        previousLeftOutput = 0;
        previousRightOutput = 0;
        enhancer.reset();
    }

    public void reset() {
        sampleBufferPosition = 0;
        leftCapacitor = 0;
        rightCapacitor = 0;
        previousLeftOutput = 0;
        previousRightOutput = 0;
        enhancer.reset();
    }

    public void close() {
        sampleBufferPosition = 0;
        soundFontSynth.close();
        sink.close();
    }

    public void applyAudioOutputConfig(AppSettings.AudioEnhancementConfig config) {
        AudioSink previous = sink;
        sampleBufferPosition = 0;
        sink = AudioSinkFactory.createDefault(sampleRate, config);
        previous.close();
        currentConfig = config;
    }

    @Override
    public void applyOutputSettings(AppSettings.AudioEnhancementConfig config) {
        AppSettings.AudioEnhancementConfig normalized = config == null
                ? AppSettings.defaults().normalizedAudioEnhancement()
                : config;
        if (currentConfig == null
                || !currentConfig.outputDeviceName().equals(normalized.outputDeviceName())
                || currentConfig.outputBufferMillis() != normalized.outputBufferMillis()) {
            applyAudioOutputConfig(normalized);
        }
        applyEnhancement(normalized);
    }

    @Override
    public void writeStereoSample(int left, int right) {
        if (soundFontSynth.suppressPcmOutput()) {
            left = 0;
            right = 0;
        }
        int filteredLeft = highPassLeft(clampSample(left));
        int filteredRight = highPassRight(clampSample(right));
        previousLeftOutput += (filteredLeft - previousLeftOutput) * lowPassAlpha / FILTER_DIVISOR;
        previousRightOutput += (filteredRight - previousRightOutput) * lowPassAlpha / FILTER_DIVISOR;
        enhancer.process(previousLeftOutput, previousRightOutput, enhancedSample);
        putPcm16(clampPcm(enhancedSample[0]));
        putPcm16(clampPcm(enhancedSample[1]));
    }

    @Override
    public void writeSilentSample() {
        writeStereoSample(0, 0);
    }

    @Override
    public int previousLeftSample() {
        return leftCapacitor;
    }

    @Override
    public int previousRightSample() {
        return rightCapacitor;
    }

    @Override
    public int bufferedSampleBytes() {
        return sampleBufferPosition;
    }

    @Override
    public int lowPassAlpha() {
        return lowPassAlpha;
    }

    @Override
    public void setLowPassAlpha(int lowPassAlpha) {
        this.lowPassAlpha = Math.max(50, Math.min(1000, lowPassAlpha));
    }

    public void setSinkMuted(boolean sinkMuted) {
        if (this.sinkMuted == sinkMuted) {
            return;
        }
        this.sinkMuted = sinkMuted;
        sampleBufferPosition = 0;
    }

    @Override
    public void setOutputMuted(boolean muted) {
        setSinkMuted(muted);
    }

    public void applyEnhancement(AppSettings.AudioEnhancementConfig config) {
        enhancer.apply(config);
        soundFontSynth.apply(config);
    }

    public String sinkDebugDescription() {
        String soundFont = soundFontSynth.debugDescription();
        if ("SoundFont off".equals(soundFont)) {
            return sink.debugDescription();
        }
        return sink.debugDescription() + ", " + soundFont;
    }

    @Override
    public String debugDescription() {
        return sinkDebugDescription();
    }

    @Override
    public void updateChannelState(int channel, boolean enabled, double frequencyHz, int volume, boolean noise) {
        soundFontSynth.updateChannelState(channel, enabled, frequencyHz, volume, noise);
    }

    @Override
    public boolean observesChannelState() {
        return true;
    }

    @Override
    public boolean suppressPcmOutput() {
        return soundFontSynth.suppressPcmOutput();
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
        if (sinkMuted) {
            return;
        }
        sampleBuffer[sampleBufferPosition++] = (byte) (sample & 0xFF);
        sampleBuffer[sampleBufferPosition++] = (byte) ((sample >> 8) & 0xFF);
        if (sampleBufferPosition == sampleBuffer.length) {
            sink.write(sampleBuffer, sampleBufferPosition);
            sampleBufferPosition = 0;
        }
    }
}
