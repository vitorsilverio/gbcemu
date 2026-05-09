package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class Apu implements MemorySpace, MachineCycle, Stateful<ApuState>, ApuContext {

    private static final int CPU_CLOCK_HZ = 4_194_304;
    private static final int SAMPLE_RATE = 44_100;

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
    private final int[] debugChannelVolumes = {100, 100, 100, 100};
    private int debugMasterVolume = 100;
    private int debugLeftVolume = 100;
    private int debugRightVolume = 100;

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
                || address == ApuAddress.PCM12_CGB_DIGITAL_OUTPUT
                || address == ApuAddress.PCM34_CGB_DIGITAL_OUTPUT;
    }

    @Override
    public byte read(int address) {
        if (address == ApuAddress.PCM12_CGB_DIGITAL_OUTPUT) {
            return (byte) (channel1.digitalOutput() | (channel2.digitalOutput() << 4));
        }
        if (address == ApuAddress.PCM34_CGB_DIGITAL_OUTPUT) {
            return (byte) (channel3.digitalOutput() | (channel4.digitalOutput() << 4));
        }
        return ApuRegisterReader.read(address, registers, readNr52());
    }

    @Override
    public void write(int address, byte value) {
        ApuRegisterWriter.write(
                address,
                value,
                registers,
                audioEnabled,
                this::setAudioEnabled,
                channel1,
                channel2,
                channel3,
                channel4
        );
    }

    int bufferedSampleBytes() {
        return output.bufferedSampleBytes();
    }

    private void writeSample() {
        int nr50 = registers.read(ApuAddress.NR50_MASTER_VOLUME) & 0xFF;
        int nr51 = registers.read(ApuAddress.NR51_SOUND_PANNING) & 0xFF;
        mixer.writeSample(
                nr50,
                nr51,
                debugOutput(0, channel1.output()),
                debugOutput(1, channel2.output()),
                debugOutput(2, channel3.output()),
                debugOutput(3, channel4.output()),
                debugMasterVolume,
                debugLeftVolume,
                debugRightVolume
        );
    }

    private int debugOutput(int channel, int output) {
        return output * debugChannelVolumes[channel] / 100;
    }

    public int debugChannelVolume(int channel) {
        return debugChannelVolumes[channelIndex(channel)];
    }

    public void setDebugChannelVolume(int channel, int volume) {
        debugChannelVolumes[channelIndex(channel)] = clampPercent(volume);
    }

    public boolean debugChannelMuted(int channel) {
        return debugChannelVolume(channel) == 0;
    }

    public void setDebugChannelMuted(int channel, boolean muted) {
        setDebugChannelVolume(channel, muted ? 0 : 100);
    }

    public int debugMasterVolume() {
        return debugMasterVolume;
    }

    public void setDebugMasterVolume(int volume) {
        debugMasterVolume = clampPercent(volume);
    }

    public int debugLeftVolume() {
        return debugLeftVolume;
    }

    public void setDebugLeftVolume(int volume) {
        debugLeftVolume = clampPercent(volume);
    }

    public int debugRightVolume() {
        return debugRightVolume;
    }

    public void setDebugRightVolume(int volume) {
        debugRightVolume = clampPercent(volume);
    }

    private int channelIndex(int channel) {
        if (channel < 1 || channel > 4) {
            throw new IllegalArgumentException("channel must be between 1 and 4");
        }
        return channel - 1;
    }

    private int clampPercent(int volume) {
        return Math.max(0, Math.min(100, volume));
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
        registers.write(ApuAddress.NR52_AUDIO_MASTER_CONTROL, (byte) (enabled ? 0x80 : 0x00));
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
