package dev.vitorsilverio.gbcemu.android;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.core.ConsoleAudioOutput;

public class AndroidAudioOutput implements ConsoleAudioOutput {
    private static final int PCM_SCALE = 48;
    private static final int BUFFER_SAMPLES = 2048;

    private final int sampleRate;
    private final short[] sampleBuffer = new short[BUFFER_SAMPLES];
    private volatile AudioTrack audioTrack;
    private int samplePosition;
    private int previousLeft;
    private int previousRight;
    private volatile boolean nonBlockingWriteSupported = true;
    private volatile boolean muted;

    public AndroidAudioOutput(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public int sampleRate() {
        return sampleRate;
    }

    public synchronized void start() {
        if (audioTrack != null) {
            return;
        }
        int minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (minBuffer <= 0) {
            minBuffer = sampleRate / 10 * 4;
        }
        int bufferSize = Math.max(minBuffer, sampleRate / 10 * 4);
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        audioTrack.play();
    }

    @Override
    public void writeStereoSample(int left, int right) {
        previousLeft = left;
        previousRight = right;
        if (muted) {
            return;
        }
        if (samplePosition + 1 >= sampleBuffer.length) {
            flush();
        }
        if (samplePosition + 1 >= sampleBuffer.length) {
            samplePosition = 0;
        }
        sampleBuffer[samplePosition++] = (short) clamp(left * PCM_SCALE);
        sampleBuffer[samplePosition++] = (short) clamp(right * PCM_SCALE);
        if (samplePosition >= sampleBuffer.length) {
            flush();
        }
    }

    @Override
    public void writeSilentSample() {
        writeStereoSample(0, 0);
    }

    @Override
    public synchronized void restoreHighPassFilter(int leftCapacitor, int rightCapacitor) {
        previousLeft = leftCapacitor;
        previousRight = rightCapacitor;
        samplePosition = 0;
    }

    @Override
    public int previousLeftSample() {
        return previousLeft;
    }

    @Override
    public int previousRightSample() {
        return previousRight;
    }

    @Override
    public int bufferedSampleBytes() {
        return samplePosition * 2;
    }

    @Override
    public void applyOutputSettings(AppSettings.AudioEnhancementConfig config) {
    }

    @Override
    public synchronized void setOutputMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            samplePosition = 0;
        }
    }

    @Override
    public synchronized void close() {
        stop();
    }

    public synchronized void stop() {
        if (audioTrack == null) {
            return;
        }
        audioTrack.pause();
        audioTrack.flush();
        audioTrack.release();
        audioTrack = null;
        samplePosition = 0;
    }

    private void flush() {
        AudioTrack track = audioTrack;
        if (track != null && samplePosition > 0) {
            int written = writeNonBlocking(track, sampleBuffer, samplePosition);
            written -= written & 1;
            if (written > 0 && written < samplePosition) {
                int remaining = samplePosition - written;
                System.arraycopy(sampleBuffer, written, sampleBuffer, 0, remaining);
                samplePosition = remaining;
                return;
            }
        }
        samplePosition = 0;
    }

    private int writeNonBlocking(AudioTrack track, short[] buffer, int length) {
        try {
            if (nonBlockingWriteSupported) {
                int written = track.write(buffer, 0, length, AudioTrack.WRITE_NON_BLOCKING);
                if (written >= 0) {
                    return written;
                }
                nonBlockingWriteSupported = false;
            }
            return track.write(buffer, 0, length);
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    private int clamp(int sample) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
    }
}
