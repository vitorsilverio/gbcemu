package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class Apu implements MemorySpace, MachineCycle, Stateful<ApuState>, ApuContext {

    private static final int CPU_CLOCK_HZ = 4_194_304;
    private static final int SAMPLE_RATE = 44_100;

    static final int NR50_MASTER_VOLUME = 0xFF24;
    static final int NR51_SOUND_PANNING = 0xFF25;
    static final int NR52_AUDIO_MASTER_CONTROL = 0xFF26;
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

    private final ApuRegisters registers = new ApuRegisters();
    private final AudioOutput output;
    private final ApuMixer mixer;
    private final ApuFrameSequencer frameSequencer = new ApuFrameSequencer();

    private int sampleAccumulator;
    private boolean audioEnabled = true;

    private final PulseChannel channel1 = new PulseChannel(this, 0);
    private final PulseChannel channel2 = new PulseChannel(this, 1);
    private final WaveChannel channel3 = new WaveChannel(this);
    private final NoiseChannel channel4 = new NoiseChannel(this);

    public Apu() {
        this(AudioSinkFactory.createDefault(SAMPLE_RATE));
    }

    public static Apu muted() {
        return new Apu((buffer, length) -> {
        });
    }

    Apu(AudioSink sink) {
        this.output = new AudioOutput(sink);
        this.mixer = new ApuMixer(output);
    }

    @Override
    public ApuState saveState() {
        return new ApuState(
                registers.copyRegisters(),
                registers.copyWavePatternRam(),
                sampleAccumulator,
                frameSequencer.cycles(),
                frameSequencer.step(),
                output.previousLeftSample(),
                output.previousRightSample(),
                audioEnabled,
                channel1.saveState(),
                channel2.saveState(),
                channel3.saveState(),
                channel4.saveState()
        );
    }

    @Override
    public void loadState(ApuState state) {
        registers.loadRegisters(state.registers());
        registers.loadWavePatternRam(state.wavePatternRam());
        sampleAccumulator = state.sampleAccumulator();
        frameSequencer.load(state.frameSequencerCycles(), state.frameSequencerStep());
        output.restoreSmoothing(state.previousLeftSample(), state.previousRightSample());
        audioEnabled = state.audioEnabled();
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
        frameSequencer.tick(channel1, channel2, channel3, channel4);

        sampleAccumulator += SAMPLE_RATE;
        if (sampleAccumulator >= CPU_CLOCK_HZ) {
            sampleAccumulator -= CPU_CLOCK_HZ;
            writeSample();
        }
    }

    @Override
    public boolean contains(int address) {
        return registers.contains(address)
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
        if (registers.isUnusedRegister(address)) {
            return (byte) 0xFF;
        }
        if (registers.isWaveRam(address)) {
            return registers.readWaveRamAddress(address);
        }

        return switch (address) {
            case NR10_CHANNEL_1_SWEEP -> (byte) ((registers.read(address) & 0x7F) | 0x80);
            case NR11_CHANNEL_1_DUTY, NR21_CHANNEL_2_DUTY -> (byte) ((registers.read(address) & 0xC0) | 0x3F);
            case NR13_CHANNEL_1_FREQUENCY_LO, NR23_CHANNEL_2_FREQUENCY_LO,
                 NR33_CHANNEL_3_FREQUENCY_LO -> (byte) 0xFF;
            case NR14_CHANNEL_1_FREQUENCY_HI, NR24_CHANNEL_2_FREQUENCY_HI,
                 NR34_CHANNEL_3_FREQUENCY_HI, NR44_CHANNEL_4_CONTROL -> (byte) ((registers.read(address) & 0x40) | 0xBF);
            case NR30_CHANNEL_3_ON_OFF -> (byte) ((registers.read(address) & 0x80) | 0x7F);
            case NR31_CHANNEL_3_LENGTH, NR41_CHANNEL_4_LENGTH -> (byte) 0xFF;
            case NR32_CHANNEL_3_VOLUME -> (byte) ((registers.read(address) & 0x60) | 0x9F);
            case NR43_CHANNEL_4_FREQUENCY -> registers.read(address);
            case NR52_AUDIO_MASTER_CONTROL -> readNr52();
            case NR20_UNUSED, NR40_UNUSED -> (byte) 0xFF;
            default -> registers.read(address);
        };
    }

    @Override
    public void write(int address, byte value) {
        if (address == PCM12_CGB_DIGITAL_OUTPUT || address == PCM34_CGB_DIGITAL_OUTPUT) {
            return;
        }
        if (registers.isUnusedRegister(address)) {
            return;
        }
        if (registers.isWaveRam(address)) {
            registers.writeWaveRamAddress(address, value);
            return;
        }

        if (address == NR52_AUDIO_MASTER_CONTROL) {
            setAudioEnabled((value & 0x80) != 0);
            return;
        }

        if (!audioEnabled) {
            return;
        }

        byte oldValue = registers.read(address);
        registers.write(address, value);
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
        return output.bufferedSampleBytes();
    }

    private void writeSample() {
        int nr50 = registers.read(NR50_MASTER_VOLUME) & 0xFF;
        int nr51 = registers.read(NR51_SOUND_PANNING) & 0xFF;
        mixer.writeSample(
                nr50,
                nr51,
                channel1.output(),
                channel2.output(),
                channel3.output(),
                channel4.output()
        );
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
        registers.write(NR52_AUDIO_MASTER_CONTROL, (byte) (enabled ? 0x80 : 0x00));
        if (enabled) {
            frameSequencer.reset();
        }
        if (!enabled) {
            registers.clearRegisters();
            channel1.disable();
            channel2.disable();
            channel3.disable();
            channel4.disable();
            frameSequencer.reset();
            output.reset();
        }
    }

    @Override
    public byte register(int address) {
        return registers.read(address);
    }

    @Override
    public void setRegister(int address, byte value) {
        registers.write(address, value);
    }

    @Override
    public byte wavePatternRam(int offset) {
        return registers.readWaveRamOffset(offset);
    }

    @Override
    public int frameSequencerStep() {
        return frameSequencer.step();
    }

    @Override
    public int period(int lowAddress, int highAddress) {
        return registers.period(lowAddress, highAddress);
    }

    static int index(int address) {
        return ApuRegisters.index(address);
    }

}
