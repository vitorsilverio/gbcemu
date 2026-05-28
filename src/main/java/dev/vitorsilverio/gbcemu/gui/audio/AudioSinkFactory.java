package dev.vitorsilverio.gbcemu.gui.audio;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;

final class AudioSinkFactory {
    private static final Logger logger = LoggerFactory.getLogger(AudioSinkFactory.class);
    private static final int BYTES_PER_FRAME = 4;

    private AudioSinkFactory() {
    }

    static List<String> outputDeviceNames(int sampleRate) {
        try {
            AudioFormat format = audioFormat(sampleRate);
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
            List<String> names = new ArrayList<>();
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(lineInfo)) {
                    names.add(mixerInfo.getName());
                }
            }
            return names;
        } catch (RuntimeException e) {
            logger.debug("Failed to list audio output devices", e);
            return List.of();
        }
    }

    static AudioSink createDefault(int sampleRate, AppSettings.AudioEnhancementConfig config) {
        AppSettings.AudioEnhancementConfig normalized = AppSettings.defaults().normalizedAudioEnhancement();
        if (config != null) {
            normalized = config;
        }
        try {
            AudioFormat format = audioFormat(sampleRate);
            SourceDataLine line = sourceLine(format, normalized.outputDeviceName());
            int requestedBufferBytes = bufferMillisToBytes(sampleRate, normalized.outputBufferMillis());
            line.open(format, requestedBufferBytes);
            line.start();
            logger.info("Audio output opened: device={} requestedFormat={} actualFormat={} requestedBuffer={} nativeBuffer={}",
                    normalized.outputDeviceName().isBlank() ? "default" : normalized.outputDeviceName(),
                    format,
                    line.getFormat(),
                    requestedBufferBytes,
                    line.getBufferSize());
            return new SourceDataLineSink(line, queueBufferBytes(sampleRate, normalized.outputBufferMillis()));
        } catch (LineUnavailableException | IllegalArgumentException e) {
            logger.warn("Audio output unavailable, running APU muted", e);
            return (buffer, length) -> {
            };
        }
    }

    private static AudioFormat audioFormat(int sampleRate) {
        return new AudioFormat(sampleRate, 16, 2, true, false);
    }

    private static SourceDataLine sourceLine(AudioFormat format, String deviceName) throws LineUnavailableException {
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
        if (deviceName != null && !deviceName.isBlank()) {
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                if (mixerInfo.getName().equals(deviceName)) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    if (mixer.isLineSupported(lineInfo)) {
                        return (SourceDataLine) mixer.getLine(lineInfo);
                    }
                    break;
                }
            }
        }
        return AudioSystem.getSourceDataLine(format);
    }

    private static int bufferMillisToBytes(int sampleRate, int millis) {
        int frames = sampleRate * Math.max(20, Math.min(500, millis)) / 1000;
        return Math.max(BYTES_PER_FRAME * 256, frames * BYTES_PER_FRAME);
    }

    private static int queueBufferBytes(int sampleRate, int millis) {
        int nativeBytes = bufferMillisToBytes(sampleRate, millis);
        int queueBytes = Math.max(nativeBytes * 2, BYTES_PER_FRAME * 4096);
        return Math.min(queueBytes, BYTES_PER_FRAME * sampleRate);
    }
}
