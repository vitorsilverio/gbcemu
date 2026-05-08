package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Apu implements MemorySpace, MachineCycle, Snapshottable {

    private static final Logger logger = LoggerFactory.getLogger(Apu.class);

    private static final int CPU_CLOCK_HZ = 4_194_304;
    private static final int SAMPLE_RATE = 44_100;
    private static final int FRAME_SEQUENCER_CYCLES = CPU_CLOCK_HZ / 512;

    private static final int NR50_MASTER_VOLUME = 0xFF24;
    private static final int NR51_SOUND_PANNING = 0xFF25;
    private static final int NR52_AUDIO_MASTER_CONTROL = 0xFF26;
    private static final int PCM12_CGB_DIGITAL_OUTPUT = 0xFF76;
    private static final int PCM34_CGB_DIGITAL_OUTPUT = 0xFF77;

    private static final int NR10_CHANNEL_1_SWEEP = 0xFF10;
    private static final int NR11_CHANNEL_1_DUTY = 0xFF11;
    private static final int NR12_CHANNEL_1_VOLUME = 0xFF12;
    private static final int NR13_CHANNEL_1_FREQUENCY_LO = 0xFF13;
    private static final int NR14_CHANNEL_1_FREQUENCY_HI = 0xFF14;

    private static final int NR20_UNUSED = 0xFF15;
    private static final int NR21_CHANNEL_2_DUTY = 0xFF16;
    private static final int NR22_CHANNEL_2_VOLUME = 0xFF17;
    private static final int NR23_CHANNEL_2_FREQUENCY_LO = 0xFF18;
    private static final int NR24_CHANNEL_2_FREQUENCY_HI = 0xFF19;

    private static final int NR30_CHANNEL_3_ON_OFF = 0xFF1A;
    private static final int NR31_CHANNEL_3_LENGTH = 0xFF1B;
    private static final int NR32_CHANNEL_3_VOLUME = 0xFF1C;
    private static final int NR33_CHANNEL_3_FREQUENCY_LO = 0xFF1D;
    private static final int NR34_CHANNEL_3_FREQUENCY_HI = 0xFF1E;

    private static final int NR40_UNUSED = 0xFF1F;
    private static final int NR41_CHANNEL_4_LENGTH = 0xFF20;
    private static final int NR42_CHANNEL_4_VOLUME = 0xFF21;
    private static final int NR43_CHANNEL_4_FREQUENCY = 0xFF22;
    private static final int NR44_CHANNEL_4_CONTROL = 0xFF23;

    private static final List<Integer> REGISTERS = List.of(
            NR50_MASTER_VOLUME,
            NR51_SOUND_PANNING,
            NR52_AUDIO_MASTER_CONTROL,
            NR10_CHANNEL_1_SWEEP,
            NR11_CHANNEL_1_DUTY,
            NR12_CHANNEL_1_VOLUME,
            NR13_CHANNEL_1_FREQUENCY_LO,
            NR14_CHANNEL_1_FREQUENCY_HI,
            NR20_UNUSED,
            NR21_CHANNEL_2_DUTY,
            NR22_CHANNEL_2_VOLUME,
            NR23_CHANNEL_2_FREQUENCY_LO,
            NR24_CHANNEL_2_FREQUENCY_HI,
            NR30_CHANNEL_3_ON_OFF,
            NR31_CHANNEL_3_LENGTH,
            NR32_CHANNEL_3_VOLUME,
            NR33_CHANNEL_3_FREQUENCY_LO,
            NR34_CHANNEL_3_FREQUENCY_HI,
            NR40_UNUSED,
            NR41_CHANNEL_4_LENGTH,
            NR42_CHANNEL_4_VOLUME,
            NR43_CHANNEL_4_FREQUENCY,
            NR44_CHANNEL_4_CONTROL
    );

    @Savable private final byte[] registers = new byte[0x30];
    @Savable private final byte[] wavePatternRam = new byte[0x10];
    private final AudioSink sink;
    @Savable private final byte[] sampleBuffer = new byte[1024];

    @Savable private int sampleAccumulator;
    @Savable  private int frameSequencerCycles;
    @Savable private int frameSequencerStep;
    @Savable private int sampleBufferPosition;
    @Savable private int previousLeftSample;
    @Savable private int previousRightSample;
    @Savable private boolean audioEnabled = true;

    @Savable private PulseChannel channel1 = new PulseChannel(0);
    @Savable private PulseChannel channel2 = new PulseChannel(1);
    @Savable private WaveChannel channel3 = new WaveChannel();
    @Savable private NoiseChannel channel4 = new NoiseChannel();

    public Apu() {
        this(createDefaultSink());
    }

    public static Apu muted() {
        return new Apu((buffer, length) -> {
        });
    }

    Apu(AudioSink sink) {
        this.sink = sink;
        registers[index(NR50_MASTER_VOLUME)] = 0x77;
        registers[index(NR51_SOUND_PANNING)] = (byte) 0xFF;
        registers[index(NR52_AUDIO_MASTER_CONTROL)] = (byte) 0x80;
    }

    @Override
    public void tick() {
        if (!audioEnabled) {
            return;
        }

        channel1.tick();
        channel2.tick();
        channel3.tick();
        channel4.tick();
        tickFrameSequencer();

        sampleAccumulator += SAMPLE_RATE;
        if (sampleAccumulator >= CPU_CLOCK_HZ) {
            sampleAccumulator -= CPU_CLOCK_HZ;
            writeSample();
        }
    }

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address)
                || (address >= 0xFF27 && address <= 0xFF3F)
                || address == PCM12_CGB_DIGITAL_OUTPUT
                || address == PCM34_CGB_DIGITAL_OUTPUT;
    }

    @Override
    public byte read(int address) {
        if (address == PCM12_CGB_DIGITAL_OUTPUT) {
            return (byte) (channel1.digitalOutput() | (channel2.digitalOutput() << 4));
        }
        if (address == PCM34_CGB_DIGITAL_OUTPUT) {
            return (byte) (channel3.digitalOutput() | (channel4.digitalOutput() << 4));
        }
        if (address >= 0xFF27 && address <= 0xFF2F) {
            return (byte) 0xFF;
        }
        if (address >= 0xFF30 && address <= 0xFF3F) {
            return wavePatternRam[address - 0xFF30];
        }

        return switch (address) {
            case NR10_CHANNEL_1_SWEEP -> (byte) ((registers[index(address)] & 0x7F) | 0x80);
            case NR11_CHANNEL_1_DUTY, NR21_CHANNEL_2_DUTY -> (byte) ((registers[index(address)] & 0xC0) | 0x3F);
            case NR13_CHANNEL_1_FREQUENCY_LO, NR23_CHANNEL_2_FREQUENCY_LO,
                 NR33_CHANNEL_3_FREQUENCY_LO -> (byte) 0xFF;
            case NR14_CHANNEL_1_FREQUENCY_HI, NR24_CHANNEL_2_FREQUENCY_HI,
                 NR34_CHANNEL_3_FREQUENCY_HI, NR44_CHANNEL_4_CONTROL -> (byte) ((registers[index(address)] & 0x40) | 0xBF);
            case NR30_CHANNEL_3_ON_OFF -> (byte) ((registers[index(address)] & 0x80) | 0x7F);
            case NR31_CHANNEL_3_LENGTH, NR41_CHANNEL_4_LENGTH -> (byte) 0xFF;
            case NR32_CHANNEL_3_VOLUME -> (byte) ((registers[index(address)] & 0x60) | 0x9F);
            case NR43_CHANNEL_4_FREQUENCY -> registers[index(address)];
            case NR52_AUDIO_MASTER_CONTROL -> readNr52();
            case NR20_UNUSED, NR40_UNUSED -> (byte) 0xFF;
            default -> registers[index(address)];
        };
    }

    @Override
    public void write(int address, byte value) {
        if (address == PCM12_CGB_DIGITAL_OUTPUT || address == PCM34_CGB_DIGITAL_OUTPUT) {
            return;
        }
        if (address >= 0xFF27 && address <= 0xFF2F) {
            return;
        }
        if (address >= 0xFF30 && address <= 0xFF3F) {
            wavePatternRam[address - 0xFF30] = value;
            return;
        }

        if (address == NR52_AUDIO_MASTER_CONTROL) {
            setAudioEnabled((value & 0x80) != 0);
            return;
        }

        if (!audioEnabled) {
            return;
        }

        byte oldValue = registers[index(address)];
        registers[index(address)] = value;
        switch (address) {
            case NR10_CHANNEL_1_SWEEP -> channel1.setSweep(oldValue, value);
            case NR11_CHANNEL_1_DUTY -> channel1.setLength(64 - (value & 0x3F));
            case NR12_CHANNEL_1_VOLUME -> channel1.setEnvelope(value);
            case NR13_CHANNEL_1_FREQUENCY_LO -> channel1.updatePeriod();
            case NR14_CHANNEL_1_FREQUENCY_HI -> {
                channel1.clockLengthOnEnable(oldValue, value);
                channel1.updatePeriod();
                if ((value & 0x80) != 0) {
                    channel1.trigger();
                }
            }
            case NR21_CHANNEL_2_DUTY -> channel2.setLength(64 - (value & 0x3F));
            case NR22_CHANNEL_2_VOLUME -> channel2.setEnvelope(value);
            case NR23_CHANNEL_2_FREQUENCY_LO -> channel2.updatePeriod();
            case NR24_CHANNEL_2_FREQUENCY_HI -> {
                channel2.clockLengthOnEnable(oldValue, value);
                channel2.updatePeriod();
                if ((value & 0x80) != 0) {
                    channel2.trigger();
                }
            }
            case NR30_CHANNEL_3_ON_OFF -> {
                if ((value & 0x80) == 0) {
                    channel3.enabled = false;
                }
            }
            case NR31_CHANNEL_3_LENGTH -> channel3.lengthTimer = 256 - (value & 0xFF);
            case NR33_CHANNEL_3_FREQUENCY_LO -> channel3.updatePeriod();
            case NR34_CHANNEL_3_FREQUENCY_HI -> {
                channel3.clockLengthOnEnable(oldValue, value);
                channel3.updatePeriod();
                if ((value & 0x80) != 0) {
                    channel3.trigger();
                }
            }
            case NR41_CHANNEL_4_LENGTH -> channel4.lengthTimer = 64 - (value & 0x3F);
            case NR42_CHANNEL_4_VOLUME -> channel4.setEnvelope(value);
            case NR44_CHANNEL_4_CONTROL -> {
                channel4.clockLengthOnEnable(oldValue, value);
                if ((value & 0x80) != 0) {
                    channel4.trigger();
                }
            }
            default -> {
            }
        }
    }

    int bufferedSampleBytes() {
        return sampleBufferPosition;
    }

    private void tickFrameSequencer() {
        frameSequencerCycles++;
        if (frameSequencerCycles < FRAME_SEQUENCER_CYCLES) {
            return;
        }
        frameSequencerCycles = 0;
        frameSequencerStep = (frameSequencerStep + 1) & 0x07;

        if ((frameSequencerStep & 1) == 0) {
            channel1.tickLength();
            channel2.tickLength();
            channel3.tickLength();
            channel4.tickLength();
        }
        if (frameSequencerStep == 2 || frameSequencerStep == 6) {
            channel1.tickSweep();
        }
        if (frameSequencerStep == 7) {
            channel1.tickEnvelope();
            channel2.tickEnvelope();
            channel4.tickEnvelope();
        }
    }

    private void writeSample() {
        int nr50 = registers[index(NR50_MASTER_VOLUME)] & 0xFF;
        int nr51 = registers[index(NR51_SOUND_PANNING)] & 0xFF;
        int leftVolume = ((nr50 >> 4) & 0x07) + 1;
        int rightVolume = (nr50 & 0x07) + 1;
        int[] outputs = {
                channel1.output(),
                channel2.output(),
                channel3.output(),
                channel4.output()
        };

        int left = 0;
        int right = 0;
        for (int i = 0; i < outputs.length; i++) {
            if ((nr51 & (1 << (i + 4))) != 0) {
                left += outputs[i];
            }
            if ((nr51 & (1 << i)) != 0) {
                right += outputs[i];
            }
        }

        previousLeftSample = smoothSample(previousLeftSample, clampSample(left * leftVolume));
        previousRightSample = smoothSample(previousRightSample, clampSample(right * rightVolume));
        putPcm16(previousLeftSample);
        putPcm16(previousRightSample);
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

    private byte readNr52() {
        int value = audioEnabled ? 0x80 : 0;
        value |= 0x70;
        value |= channel1.enabled ? 0x01 : 0;
        value |= channel2.enabled ? 0x02 : 0;
        value |= channel3.enabled ? 0x04 : 0;
        value |= channel4.enabled ? 0x08 : 0;
        return (byte) value;
    }

    private void setAudioEnabled(boolean enabled) {
        audioEnabled = enabled;
        registers[index(NR52_AUDIO_MASTER_CONTROL)] = (byte) (enabled ? 0x80 : 0x00);
        if (enabled) {
            frameSequencerCycles = 0;
            frameSequencerStep = 0;
        }
        if (!enabled) {
            Arrays.fill(registers, (byte) 0);
            channel1.disable();
            channel2.disable();
            channel3.disable();
            channel4.disable();
            frameSequencerCycles = 0;
            frameSequencerStep = 0;
            sampleBufferPosition = 0;
            previousLeftSample = 0;
            previousRightSample = 0;
        }
    }

    private int period(int lowAddress, int highAddress) {
        return (registers[index(lowAddress)] & 0xFF) | ((registers[index(highAddress)] & 0x07) << 8);
    }

    private static int index(int address) {
        return address - 0xFF10;
    }

    private static AudioSink createDefaultSink() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
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

    interface AudioSink {
        void write(byte[] buffer, int length);
    }

    private static class SourceDataLineSink implements AudioSink {
        private final SourceDataLine line;
        private static final int BUFFER_SIZE = 65536;
        private static final int PREBUFFER_BYTES = 4096;
        private static final int WRITE_CHUNK_SIZE = 512;

        private final byte[] buffer = new byte[BUFFER_SIZE];
        private int readPosition;
        private int writePosition;
        private int size;
        private boolean primed;

        private SourceDataLineSink(SourceDataLine line) {
            this.line = line;
            Thread thread = new Thread(this::run, "gbcemu-audio");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public synchronized void write(byte[] source, int length) {
            int written = 0;
            while (written < length) {
                while (size == BUFFER_SIZE) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                int writable = Math.min(length - written, BUFFER_SIZE - size);
                for (int i = 0; i < writable; i++) {
                    buffer[writePosition] = source[written + i];
                    writePosition = (writePosition + 1) % BUFFER_SIZE;
                }
                size += writable;
                written += writable;
                notifyAll();
            }
        }

        private synchronized int read(byte[] destination) throws InterruptedException {
            while (!primed && size < PREBUFFER_BYTES) {
                wait();
            }
            primed = true;

            int length = Math.min(destination.length, Math.min(size, line.available()));
            if (length <= 0) {
                if (size == 0) {
                    primed = false;
                }
                return 0;
            }

            for (int i = 0; i < length; i++) {
                destination[i] = buffer[readPosition];
                readPosition = (readPosition + 1) % BUFFER_SIZE;
            }
            size -= length;
            notifyAll();
            return length;
        }

        private void run() {
            byte[] chunk = new byte[WRITE_CHUNK_SIZE];
            while (true) {
                try {
                    int length = read(chunk);
                    if (length > 0) {
                        line.write(chunk, 0, length);
                    } else {
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private abstract class SoundChannel {
        protected boolean enabled;
        protected int lengthTimer;
        protected int currentVolume;
        protected int envelopeTimer;
        protected int timer;

        protected void setEnvelope(byte value) {
            if ((value & 0xF8) == 0) {
                enabled = false;
            }
        }

        protected void triggerEnvelope(int envelopeRegisterAddress) {
            currentVolume = (registers[index(envelopeRegisterAddress)] >> 4) & 0x0F;
            envelopeTimer = registers[index(envelopeRegisterAddress)] & 0x07;
        }

        protected void tickEnvelope(int envelopeRegisterAddress) {
            int envelope = registers[index(envelopeRegisterAddress)] & 0xFF;
            int pace = envelope & 0x07;
            if (!enabled || pace == 0) {
                return;
            }
            envelopeTimer--;
            if (envelopeTimer > 0) {
                return;
            }
            envelopeTimer = pace;
            if ((envelope & 0x08) != 0 && currentVolume < 15) {
                currentVolume++;
            } else if ((envelope & 0x08) == 0 && currentVolume > 0) {
                currentVolume--;
            }
        }

        protected void tickLength(int controlRegisterAddress) {
            if ((registers[index(controlRegisterAddress)] & 0x40) == 0 || lengthTimer <= 0) {
                return;
            }
            lengthTimer--;
            if (lengthTimer == 0) {
                enabled = false;
            }
        }

        protected void clockLengthOnEnable(byte oldValue, byte newValue) {
            boolean oldEnabled = (oldValue & 0x40) != 0;
            boolean newEnabled = (newValue & 0x40) != 0;
            if (!oldEnabled && newEnabled && shouldClockLengthOnEnable() && lengthTimer > 0) {
                lengthTimer--;
                if (lengthTimer == 0) {
                    enabled = false;
                }
            }
        }

        private boolean shouldClockLengthOnEnable() {
            return (frameSequencerStep & 1) == 0;
        }

        protected void clockLengthAfterTriggerIfNeeded(int controlRegisterAddress) {
            if ((registers[index(controlRegisterAddress)] & 0x40) != 0 && shouldClockLengthOnEnable() && lengthTimer > 0) {
                lengthTimer--;
                if (lengthTimer == 0) {
                    enabled = false;
                }
            }
        }

        protected void disable() {
            enabled = false;
            timer = 0;
            lengthTimer = 0;
            currentVolume = 0;
            envelopeTimer = 0;
        }

        abstract void tick();

        abstract int output();

        abstract int digitalOutput();
    }

    private class PulseChannel extends SoundChannel implements Serializable {
        private final int channel;
        private int period;
        private int dutyStep;
        private int sweepShadowPeriod;
        private int sweepTimer;
        private boolean sweepEnabled;
        private boolean sweepNegateUsed;

        private final int[][] dutyPatterns = {
                {0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 1, 1, 1},
                {0, 1, 1, 1, 1, 1, 1, 0}
        };

        private PulseChannel(int channel) {
            this.channel = channel;
        }

        private void setLength(int length) {
            lengthTimer = length == 0 ? 64 : length;
        }

        private void updatePeriod() {
            period = channel == 0
                    ? period(NR13_CHANNEL_1_FREQUENCY_LO, NR14_CHANNEL_1_FREQUENCY_HI)
                    : period(NR23_CHANNEL_2_FREQUENCY_LO, NR24_CHANNEL_2_FREQUENCY_HI);
        }

        private void setSweep(byte oldValue, byte newValue) {
            if (channel != 0) {
                return;
            }
            boolean wasNegate = (oldValue & 0x08) != 0;
            boolean isNegate = (newValue & 0x08) != 0;
            if (wasNegate && !isNegate && sweepNegateUsed) {
                enabled = false;
            }
        }

        private void trigger() {
            int envelopeAddress = channel == 0 ? NR12_CHANNEL_1_VOLUME : NR22_CHANNEL_2_VOLUME;
            boolean lengthWasZero = lengthTimer == 0;
            enabled = true;
            if (lengthWasZero) {
                lengthTimer = 64;
            }
            if (lengthWasZero) {
                clockLengthAfterTriggerIfNeeded(channel == 0 ? NR14_CHANNEL_1_FREQUENCY_HI : NR24_CHANNEL_2_FREQUENCY_HI);
            }
            boolean sweepAllowsChannel = triggerSweep();
            if ((registers[index(envelopeAddress)] & 0xF8) == 0) {
                enabled = false;
                return;
            }
            if (!sweepAllowsChannel) {
                enabled = false;
                return;
            }
            enabled = true;
            triggerEnvelope(envelopeAddress);
            timer = pulseTimerPeriod();
        }

        @Override
        void tick() {
            if (!enabled) {
                return;
            }
            timer--;
            if (timer <= 0) {
                timer += pulseTimerPeriod();
                dutyStep = (dutyStep + 1) & 0x07;
            }
        }

        @Override
        int output() {
            if (!enabled) {
                return 0;
            }
            return digitalOutput() - 8;
        }

        @Override
        int digitalOutput() {
            if (!enabled) {
                return 0;
            }
            int dutyAddress = channel == 0 ? NR11_CHANNEL_1_DUTY : NR21_CHANNEL_2_DUTY;
            int duty = (registers[index(dutyAddress)] >> 6) & 0x03;
            return dutyPatterns[duty][dutyStep] == 0 ? 0 : currentVolume;
        }

        private int pulseTimerPeriod() {
            return Math.max(4, (2048 - period) * 4);
        }

        private void tickLength() {
            tickLength(channel == 0 ? NR14_CHANNEL_1_FREQUENCY_HI : NR24_CHANNEL_2_FREQUENCY_HI);
        }

        private void tickEnvelope() {
            tickEnvelope(channel == 0 ? NR12_CHANNEL_1_VOLUME : NR22_CHANNEL_2_VOLUME);
        }

        private boolean triggerSweep() {
            if (channel != 0) {
                return true;
            }
            sweepShadowPeriod = period;
            sweepTimer = sweepPace();
            if (sweepTimer == 0) {
                sweepTimer = 8;
            }
            sweepEnabled = sweepPace() != 0 || sweepStep() != 0;
            sweepNegateUsed = false;
            if (sweepStep() == 0) {
                return true;
            }
            return calculateSweepPeriod() <= 0x7FF;
        }

        private void tickSweep() {
            if (channel != 0 || !sweepEnabled) {
                return;
            }
            sweepTimer--;
            if (sweepTimer > 0) {
                return;
            }
            sweepTimer = sweepPace();
            if (sweepTimer == 0) {
                sweepTimer = 8;
            }
            if (sweepPace() == 0) {
                return;
            }

            int calculatedPeriod = calculateSweepPeriod();
            if (calculatedPeriod > 0x7FF) {
                enabled = false;
                return;
            }
            if (sweepStep() == 0) {
                return;
            }

            sweepShadowPeriod = calculatedPeriod;
            period = calculatedPeriod;
            writeChannel1Period(calculatedPeriod);
            if (calculateSweepPeriod() > 0x7FF) {
                enabled = false;
            }
        }

        private int calculateSweepPeriod() {
            int delta = sweepShadowPeriod >> sweepStep();
            if (sweepNegate()) {
                sweepNegateUsed = true;
                return (sweepShadowPeriod - delta) & 0x7FF;
            }
            return sweepShadowPeriod + delta;
        }

        private void writeChannel1Period(int value) {
            registers[index(NR13_CHANNEL_1_FREQUENCY_LO)] = (byte) value;
            int high = registers[index(NR14_CHANNEL_1_FREQUENCY_HI)] & 0xF8;
            registers[index(NR14_CHANNEL_1_FREQUENCY_HI)] = (byte) (high | ((value >> 8) & 0x07));
        }

        private int sweepPace() {
            return (registers[index(NR10_CHANNEL_1_SWEEP)] >> 4) & 0x07;
        }

        private boolean sweepNegate() {
            return (registers[index(NR10_CHANNEL_1_SWEEP)] & 0x08) != 0;
        }

        private int sweepStep() {
            return registers[index(NR10_CHANNEL_1_SWEEP)] & 0x07;
        }
    }

    private class WaveChannel extends SoundChannel implements Serializable {
        private int period;
        private int sampleIndex;

        private void updatePeriod() {
            period = period(NR33_CHANNEL_3_FREQUENCY_LO, NR34_CHANNEL_3_FREQUENCY_HI);
        }

        private void trigger() {
            boolean lengthWasZero = lengthTimer == 0;
            enabled = true;
            if (lengthWasZero) {
                lengthTimer = 256;
            }
            if (lengthWasZero) {
                clockLengthAfterTriggerIfNeeded(NR34_CHANNEL_3_FREQUENCY_HI);
            }
            if ((registers[index(NR30_CHANNEL_3_ON_OFF)] & 0x80) == 0) {
                enabled = false;
                return;
            }
            enabled = true;
            timer = waveTimerPeriod();
            sampleIndex = 0;
        }

        @Override
        void tick() {
            if (!enabled) {
                return;
            }
            timer--;
            if (timer <= 0) {
                timer += waveTimerPeriod();
                sampleIndex = (sampleIndex + 1) & 0x1F;
            }
        }

        @Override
        int output() {
            if (!enabled) {
                return 0;
            }
            return digitalOutput() - 8;
        }

        @Override
        int digitalOutput() {
            if (!enabled) {
                return 0;
            }
            int packed = wavePatternRam[sampleIndex / 2] & 0xFF;
            int sample = (sampleIndex & 1) == 0 ? packed >> 4 : packed & 0x0F;
            int volumeCode = (registers[index(NR32_CHANNEL_3_VOLUME)] >> 5) & 0x03;
            int shifted = switch (volumeCode) {
                case 0 -> 0;
                case 1 -> sample;
                case 2 -> sample >> 1;
                case 3 -> sample >> 2;
                default -> 0;
            };
            return shifted;
        }

        private int waveTimerPeriod() {
            return Math.max(2, (2048 - period) * 2);
        }

        private void tickLength() {
            tickLength(NR34_CHANNEL_3_FREQUENCY_HI);
        }
    }

    private class NoiseChannel extends SoundChannel implements Serializable {
        private int lfsr = 0x7FFF;

        private void trigger() {
            boolean lengthWasZero = lengthTimer == 0;
            enabled = true;
            if (lengthWasZero) {
                lengthTimer = 64;
            }
            if (lengthWasZero) {
                clockLengthAfterTriggerIfNeeded(NR44_CHANNEL_4_CONTROL);
            }
            if ((registers[index(NR42_CHANNEL_4_VOLUME)] & 0xF8) == 0) {
                enabled = false;
                return;
            }
            enabled = true;
            lfsr = 0x7FFF;
            triggerEnvelope(NR42_CHANNEL_4_VOLUME);
            timer = noiseTimerPeriod();
        }

        @Override
        void tick() {
            if (!enabled) {
                return;
            }
            timer--;
            if (timer <= 0) {
                timer += noiseTimerPeriod();
                int xor = (lfsr & 1) ^ ((lfsr >> 1) & 1);
                lfsr = (lfsr >> 1) | (xor << 14);
                if ((registers[index(NR43_CHANNEL_4_FREQUENCY)] & 0x08) != 0) {
                    lfsr = (lfsr & ~(1 << 6)) | (xor << 6);
                }
            }
        }

        @Override
        int output() {
            if (!enabled) {
                return 0;
            }
            return digitalOutput() - 8;
        }

        @Override
        int digitalOutput() {
            if (!enabled) {
                return 0;
            }
            return (lfsr & 1) == 0 ? currentVolume : 0;
        }

        private int noiseTimerPeriod() {
            int value = registers[index(NR43_CHANNEL_4_FREQUENCY)] & 0xFF;
            int divisorCode = value & 0x07;
            int divisor = divisorCode == 0 ? 8 : divisorCode * 16;
            int shift = (value >> 4) & 0x0F;
            return Math.max(8, divisor << shift);
        }

        private void tickLength() {
            tickLength(NR44_CHANNEL_4_CONTROL);
        }

        private void tickEnvelope() {
            tickEnvelope(NR42_CHANNEL_4_VOLUME);
        }
    }
}
