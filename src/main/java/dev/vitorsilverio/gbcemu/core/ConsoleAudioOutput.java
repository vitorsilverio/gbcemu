package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.audio.AudioSampleOutput;
import dev.vitorsilverio.gbcemu.config.AppSettings;

public interface ConsoleAudioOutput extends AudioSampleOutput {
    ConsoleAudioOutput MUTED = new ConsoleAudioOutput() {
        @Override
        public void writeStereoSample(int left, int right) {
        }

        @Override
        public void writeSilentSample() {
        }

        @Override
        public void restoreHighPassFilter(int leftCapacitor, int rightCapacitor) {
        }

        @Override
        public int previousLeftSample() {
            return 0;
        }

        @Override
        public int previousRightSample() {
            return 0;
        }
    };

    default void applyOutputSettings(AppSettings.AudioEnhancementConfig config) {
    }

    default void setOutputMuted(boolean muted) {
    }
}
