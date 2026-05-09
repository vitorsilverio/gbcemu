package dev.vitorsilverio.gbcemu.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

final class AudioSinkFactory {
    private static final Logger logger = LoggerFactory.getLogger(AudioSinkFactory.class);

    private AudioSinkFactory() {
    }

    static AudioSink createDefault(int sampleRate) {
        try {
            AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, 32 * 1024);
            line.start();
            return new SourceDataLineSink(line);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            logger.warn("Audio output unavailable, running APU muted", e);
            return (buffer, length) -> {
            };
        }
    }
}
