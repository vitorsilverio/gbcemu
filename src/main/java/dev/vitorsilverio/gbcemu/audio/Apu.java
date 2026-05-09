package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.List;

public class Apu implements MemorySpace, MachineCycle, Stateful<ApuState>, ApuContext {

    private static final Logger logger = LoggerFactory.getLogger(Apu.class);

    private static final int CPU_CLOCK_HZ = 4_194_304;
    private static final int SAMPLE_RATE = 44_100;
    private static final int FRAME_SEQUENCER_CYCLES = CPU_CLOCK_HZ / 512;

    private static final int NR50_MASTER_VOLUME = 0xFF24;
    private static final int NR51_SOUND_PANNING = 0xFF25;
    private static final int NR52_AUDIO_MASTER_CONTROL = 0xFF26;
    private static final int PCM12_CGB_DIGITAL_OUTPUT = 0xFF76;
    private static final int PCM34_CGB_DIGITAL_OUTPUT = 0xFF77;

    static final int NR10_CHANNEL_1_SWEEP = 0xFF10;
    static final int NR11_CHANNEL_1_DUTY = 0xFF11;
    static final int NR12_CHANNEL_1_VOLUME = 0xFF12;
    static final int NR13_CHANNEL_1_FREQUENCY_LO = 0xFF13;
    static final int NR14_CHANNEL_1_FREQUENCY_HI = 0xFF14;

    private static final int NR20_UNUSED = 0xFF15;
    static final int NR21_CHANNEL_2_DUTY = 0xFF16;
    static final int NR22_CHANNEL_2_VOLUME = 0xFF17;
    static final int NR23_CHANNEL_2_FREQUENCY_LO = 0xFF18;
    static final int NR24_CHANNEL_2_FREQUENCY_HI = 0xFF19;

    static final int NR30_CHANNEL_3_ON_OFF = 0xFF1A;
    private static final int NR31_CHANNEL_3_LENGTH = 0xFF1B;
    static final int NR32_CHANNEL_3_VOLUME = 0xFF1C;
    static final int NR33_CHANNEL_3_FREQUENCY_LO = 0xFF1D;
    static final int NR34_CHANNEL_3_FREQUENCY_HI = 0xFF1E;

    private static final int NR40_UNUSED = 0xFF1F;
    private static final int NR41_CHANNEL_4_LENGTH = 0xFF20;
    static final int NR42_CHANNEL_4_VOLUME = 0xFF21;
    static final int NR43_CHANNEL_4_FREQUENCY = 0xFF22;
    static final int NR44_CHANNEL_4_CONTROL = 0xFF23;

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

    private final byte[] registers = new byte[0x30];
    private final byte[] wavePatternRam = new byte[0x10];
    private final AudioSink sink;
    private final byte[] sampleBuffer = new byte[1024];

    private int sampleAccumulator;
    private int frameSequencerCycles;
    private int frameSequencerStep;
    private int sampleBufferPosition;
    private int previousLeftSample;
    private int previousRightSample;
    private boolean audioEnabled = true;

    private final PulseChannel channel1 = new PulseChannel(this, 0);
    private final PulseChannel channel2 = new PulseChannel(this, 1);
    private final WaveChannel channel3 = new WaveChannel(this);
    private final NoiseChannel channel4 = new NoiseChannel(this);

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
    public ApuState saveState() {
        return new ApuState(
                registers.clone(),
                wavePatternRam.clone(),
                sampleAccumulator,
                frameSequencerCycles,
                frameSequencerStep,
                previousLeftSample,
                previousRightSample,
                audioEnabled,
                channel1.saveState(),
                channel2.saveState(),
                channel3.saveState(),
                channel4.saveState()
        );
    }

    @Override
    public void loadState(ApuState state) {
        System.arraycopy(state.registers(), 0, registers, 0, Math.min(registers.length, state.registers().length));
        System.arraycopy(state.wavePatternRam(), 0, wavePatternRam, 0, Math.min(wavePatternRam.length, state.wavePatternRam().length));
        sampleAccumulator = state.sampleAccumulator();
        frameSequencerCycles = state.frameSequencerCycles();
        frameSequencerStep = state.frameSequencerStep() & 0x07;
        previousLeftSample = state.previousLeftSample();
        previousRightSample = state.previousRightSample();
        audioEnabled = state.audioEnabled();
        sampleBufferPosition = 0;
        channel1.loadState(state.channel1());
        channel2.loadState(state.channel2());
        channel3.loadState(state.channel3());
        channel4.loadState(state.channel4());
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

    @Override
    public byte register(int address) {
        return registers[index(address)];
    }

    @Override
    public void setRegister(int address, byte value) {
        registers[index(address)] = value;
    }

    @Override
    public byte wavePatternRam(int offset) {
        return wavePatternRam[offset & 0x0F];
    }

    @Override
    public int frameSequencerStep() {
        return frameSequencerStep;
    }

    @Override
    public int period(int lowAddress, int highAddress) {
        return (registers[index(lowAddress)] & 0xFF) | ((registers[index(highAddress)] & 0x07) << 8);
    }

    static int index(int address) {
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

}
