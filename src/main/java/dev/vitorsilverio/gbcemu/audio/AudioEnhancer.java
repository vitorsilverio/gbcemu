package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.config.AppSettings;

final class AudioEnhancer {
    private static final int SAMPLE_RATE = 48_000;
    private static final int MAX_DELAY_SAMPLES = 2048;

    private final int[] delayLeft = new int[MAX_DELAY_SAMPLES];
    private final int[] delayRight = new int[MAX_DELAY_SAMPLES];
    private final int[] scratch = new int[2];
    private int delayPosition;
    private int lfoPosition;
    private int previousLeft;
    private int previousRight;
    private AppSettings.AudioEnhancementConfig config = AppSettings.defaults().audioEnhancement();

    void apply(AppSettings.AudioEnhancementConfig config) {
        this.config = config == null ? AppSettings.defaults().audioEnhancement() : config;
        reset();
    }

    void reset() {
        delayPosition = 0;
        lfoPosition = 0;
        previousLeft = 0;
        previousRight = 0;
        java.util.Arrays.fill(delayLeft, 0);
        java.util.Arrays.fill(delayRight, 0);
    }

    void process(int left, int right, int[] output) {
        if (config == null || "Raw".equals(config.dspPreset())) {
            output[0] = left;
            output[1] = right;
            return;
        }

        int intensity = config.dspIntensity();
        int reverb = config.reverbAmount();
        int chorus = config.chorusAmount();
        String preset = config.dspPreset();
        if ("Warm".equals(preset)) {
            left = warmLeft(left, intensity);
            right = warmRight(right, intensity);
        } else if ("Wide".equals(preset)) {
            left = warmLeft(left, intensity);
            right = warmRight(right, intensity);
            chorus(left, right, Math.max(chorus, intensity / 2), scratch);
            left = scratch[0];
            right = scratch[1];
        } else if ("Room".equals(preset)) {
            left = warmLeft(left, intensity);
            right = warmRight(right, intensity);
            chorus(left, right, Math.max(chorus, intensity / 3), scratch);
            reverb(scratch[0], scratch[1], Math.max(reverb, intensity / 3), scratch);
            left = scratch[0];
            right = scratch[1];
        } else if ("Toy Synth".equals(preset)) {
            left = saturate(left, 60 + intensity);
            right = saturate(right, 60 + intensity);
            chorus(left, right, Math.max(chorus, 35 + intensity / 3), scratch);
            reverb(scratch[0], scratch[1], Math.max(reverb, 20), scratch);
            left = scratch[0];
            right = scratch[1];
        }
        output[0] = clamp(left);
        output[1] = clamp(right);
    }

    private int warmLeft(int sample, int intensity) {
        int alpha = 80 + intensity * 5;
        previousLeft += (sample - previousLeft) * Math.min(alpha, 900) / 1000;
        return saturate(previousLeft, intensity);
    }

    private int warmRight(int sample, int intensity) {
        int alpha = 80 + intensity * 5;
        previousRight += (sample - previousRight) * Math.min(alpha, 900) / 1000;
        return saturate(previousRight, intensity);
    }

    private int saturate(int sample, int intensity) {
        long driven = (long) sample * (100 + intensity * 2) / 100;
        long shaped = driven - (driven * driven / Short.MAX_VALUE) * driven / (Short.MAX_VALUE * 3L);
        return clamp((int) shaped);
    }

    private void chorus(int left, int right, int amount, int[] output) {
        int depth = 12 + amount / 6;
        int baseDelay = 240;
        int mod = (int) (Math.sin(2.0 * Math.PI * lfoPosition / (SAMPLE_RATE / 2.0)) * depth);
        lfoPosition = (lfoPosition + 1) % SAMPLE_RATE;
        int delay = Math.max(1, Math.min(MAX_DELAY_SAMPLES - 1, baseDelay + mod));
        int read = (delayPosition - delay + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES;
        int wetLeft = delayRight[read];
        int wetRight = delayLeft[read];
        delayLeft[delayPosition] = left;
        delayRight[delayPosition] = right;
        delayPosition = (delayPosition + 1) % MAX_DELAY_SAMPLES;
        output[0] = clamp(left + wetLeft * amount / 250);
        output[1] = clamp(right + wetRight * amount / 250);
    }

    private void reverb(int left, int right, int amount, int[] output) {
        int read1 = (delayPosition - 907 + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES;
        int read2 = (delayPosition - 1321 + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES;
        int wetLeft = (delayLeft[read1] + delayRight[read2]) / 2;
        int wetRight = (delayRight[read1] + delayLeft[read2]) / 2;
        delayLeft[delayPosition] = clamp(left + wetLeft * amount / 300);
        delayRight[delayPosition] = clamp(right + wetRight * amount / 300);
        delayPosition = (delayPosition + 1) % MAX_DELAY_SAMPLES;
        output[0] = clamp(left + wetLeft * amount / 180);
        output[1] = clamp(right + wetRight * amount / 180);
    }

    private int clamp(int sample) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
    }
}
