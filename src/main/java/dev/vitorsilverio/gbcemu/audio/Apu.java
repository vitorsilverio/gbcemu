package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.ArrayList;
import java.util.List;

public class Apu implements MemorySpace, MachineCycle, Stateful<ApuState>, ApuContext {

    private static final int CPU_CLOCK_HZ = 4_194_304;
    private static final int SAMPLE_RATE = 48_000;
    private static final int REGISTER_WRITE_HISTORY_SIZE = 8192;

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
    private final boolean[] debugChannelMuted = {false, false, false, false};
    private int debugMasterVolume = 100;
    private int debugLeftVolume = 100;
    private int debugRightVolume = 100;
    private final ApuRegisterWrite[] recentWrites = new ApuRegisterWrite[REGISTER_WRITE_HISTORY_SIZE];
    private int recentWriteIndex;
    private long recentWriteSequence;
    private boolean debugWriteTraceEnabled;
    private final int[] lastRecordedRegisterValues = new int[ApuAddress.REGISTER_END - ApuAddress.REGISTER_START + 1];

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
        for (int i = 0; i < lastRecordedRegisterValues.length; i++) {
            lastRecordedRegisterValues[i] = -1;
        }
    }

    @Override
    public ApuState saveState() {
        return new ApuState(
                registers.copyRegisters(),
                registers.copyWavePatternRam(),
                sampleAccumulator,
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
        frameSequencer.load(state.frameSequencerStep());
        output.restoreHighPassFilter(state.previousLeftSample(), state.previousRightSample());
        audioEnabled = state.audioEnabled();
        channel1.loadState(state.channel1());
        channel2.loadState(state.channel2());
        channel3.loadState(state.channel3());
        channel4.loadState(state.channel4());
    }

    @Override
    public void tick() {
        if (audioEnabled) {
            channel1.tick();
            channel2.tick();
            channel3.tick();
            channel4.tick();
        }

        sampleAccumulator += SAMPLE_RATE;
        if (sampleAccumulator >= CPU_CLOCK_HZ) {
            sampleAccumulator -= CPU_CLOCK_HZ;
            if (audioEnabled) {
                writeSample();
            } else {
                output.writeSilentSample();
            }
        }
    }

    public void clockFrameSequencer() {
        frameSequencer.clock(channel1, channel2, channel3, channel4);
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
        return ApuRegisterReader.read(address, registers, readNr52(), channel3);
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
        recordRegisterWrite(address, value);
    }

    public ApuDebugSnapshot debugSnapshot() {
        int nr52 = readNr52() & 0xFF;
        return new ApuDebugSnapshot(
                audioEnabled,
                frameSequencer.step(),
                SAMPLE_RATE,
                sampleAccumulator,
                bufferedSampleBytes(),
                registers.read(ApuAddress.NR50_MASTER_VOLUME) & 0xFF,
                registers.read(ApuAddress.NR51_SOUND_PANNING) & 0xFF,
                nr52,
                output.lowPassAlpha(),
                output.sinkDebugDescription(),
                channel1.debugSnapshot(1),
                channel2.debugSnapshot(2),
                channel3.debugSnapshot(),
                channel4.debugSnapshot(),
                recentWritesSnapshot()
        );
    }

    public void setDebugWriteTraceEnabled(boolean enabled) {
        debugWriteTraceEnabled = enabled;
    }

    int bufferedSampleBytes() {
        return output.bufferedSampleBytes();
    }

    public void close() {
        output.close();
    }

    public void setFastForwardAudioMuted(boolean muted) {
        output.setSinkMuted(muted);
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
        if (debugChannelMuted[channel]) {
            return 0;
        }
        return output * debugChannelVolumes[channel] / 100;
    }

    private void recordRegisterWrite(int address, byte value) {
        if (!debugWriteTraceEnabled) {
            return;
        }
        if (!registers.contains(address)) {
            return;
        }
        int unsignedValue = value & 0xFF;
        if (isDuplicateMixerWrite(address, unsignedValue)) {
            return;
        }
        recentWrites[recentWriteIndex] = new ApuRegisterWrite(
                ++recentWriteSequence,
                address,
                unsignedValue,
                describeRegisterWrite(address, unsignedValue)
        );
        recentWriteIndex = (recentWriteIndex + 1) % recentWrites.length;
    }

    private boolean isDuplicateMixerWrite(int address, int value) {
        if (address != ApuAddress.NR50_MASTER_VOLUME && address != ApuAddress.NR51_SOUND_PANNING) {
            return false;
        }
        int index = Apu.index(address);
        if (lastRecordedRegisterValues[index] == value) {
            return true;
        }
        lastRecordedRegisterValues[index] = value;
        return false;
    }

    private String describeRegisterWrite(int address, int value) {
        return switch (address) {
            case ApuAddress.NR10_CHANNEL_1_SWEEP -> "pace=" + ((value >> 4) & 0x07)
                    + " dir=" + ((value & 0x08) == 0 ? "up" : "down")
                    + " step=" + (value & 0x07);
            case ApuAddress.NR11_CHANNEL_1_DUTY, ApuAddress.NR21_CHANNEL_2_DUTY ->
                    "duty=" + ((value >> 6) & 0x03) + " lengthLoad=" + (value & 0x3F);
            case ApuAddress.NR12_CHANNEL_1_VOLUME, ApuAddress.NR22_CHANNEL_2_VOLUME,
                 ApuAddress.NR42_CHANNEL_4_VOLUME ->
                    "volume=" + ((value >> 4) & 0x0F)
                            + " env=" + ((value & 0x08) == 0 ? "down" : "up")
                            + " pace=" + (value & 0x07);
            case ApuAddress.NR13_CHANNEL_1_FREQUENCY_LO, ApuAddress.NR14_CHANNEL_1_FREQUENCY_HI ->
                    describePulseFrequency("CH1",
                            registers.period(ApuAddress.NR13_CHANNEL_1_FREQUENCY_LO, ApuAddress.NR14_CHANNEL_1_FREQUENCY_HI),
                            address == ApuAddress.NR14_CHANNEL_1_FREQUENCY_HI && (value & 0x80) != 0);
            case ApuAddress.NR23_CHANNEL_2_FREQUENCY_LO, ApuAddress.NR24_CHANNEL_2_FREQUENCY_HI ->
                    describePulseFrequency("CH2",
                            registers.period(ApuAddress.NR23_CHANNEL_2_FREQUENCY_LO, ApuAddress.NR24_CHANNEL_2_FREQUENCY_HI),
                            address == ApuAddress.NR24_CHANNEL_2_FREQUENCY_HI && (value & 0x80) != 0);
            case ApuAddress.NR30_CHANNEL_3_ON_OFF -> (value & 0x80) == 0 ? "DAC off" : "DAC on";
            case ApuAddress.NR32_CHANNEL_3_VOLUME -> "volumeCode=" + ((value >> 5) & 0x03);
            case ApuAddress.NR33_CHANNEL_3_FREQUENCY_LO, ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI ->
                    describeWaveFrequency(
                            registers.period(ApuAddress.NR33_CHANNEL_3_FREQUENCY_LO, ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI),
                            address == ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI && (value & 0x80) != 0);
            case ApuAddress.NR43_CHANNEL_4_FREQUENCY -> describeNoiseFrequency(value);
            case ApuAddress.NR44_CHANNEL_4_CONTROL -> (value & 0x80) == 0 ? "" : "trigger";
            case ApuAddress.NR50_MASTER_VOLUME -> "leftVol=" + (((value >> 4) & 0x07) + 1)
                    + " rightVol=" + ((value & 0x07) + 1);
            case ApuAddress.NR51_SOUND_PANNING -> "panning=" + String.format("%8s", Integer.toBinaryString(value)).replace(' ', '0');
            case ApuAddress.NR52_AUDIO_MASTER_CONTROL -> (value & 0x80) == 0 ? "APU off" : "APU on";
            default -> "";
        };
    }

    private String describePulseFrequency(String channel, int period, boolean trigger) {
        int distance = 2048 - period;
        double frequency = distance <= 0 ? 0 : 131_072.0 / distance;
        return channel + " period=" + period + " hz=" + String.format("%.2f", frequency) + (trigger ? " trigger" : "");
    }

    private String describeWaveFrequency(int period, boolean trigger) {
        int distance = 2048 - period;
        double frequency = distance <= 0 ? 0 : 65_536.0 / distance;
        return "CH3 period=" + period + " hz=" + String.format("%.2f", frequency) + (trigger ? " trigger" : "");
    }

    private String describeNoiseFrequency(int value) {
        int divisorCode = value & 0x07;
        int divisor = divisorCode == 0 ? 8 : divisorCode * 16;
        int shift = (value >> 4) & 0x0F;
        int width = (value & 0x08) == 0 ? 15 : 7;
        double frequency = shift >= 14 ? 0 : 4_194_304.0 / (divisor << shift);
        return "div=" + divisor + " shift=" + shift + " width=" + width + "-bit hz=" + String.format("%.2f", frequency);
    }

    private List<ApuRegisterWrite> recentWritesSnapshot() {
        List<ApuRegisterWrite> writes = new ArrayList<>(recentWrites.length);
        for (int i = 0; i < recentWrites.length; i++) {
            int index = (recentWriteIndex + i) % recentWrites.length;
            ApuRegisterWrite write = recentWrites[index];
            if (write != null) {
                writes.add(write);
            }
        }
        return List.copyOf(writes);
    }

    public int debugChannelVolume(int channel) {
        return debugChannelVolumes[channelIndex(channel)];
    }

    public void setDebugChannelVolume(int channel, int volume) {
        debugChannelVolumes[channelIndex(channel)] = clampPercent(volume);
    }

    public boolean debugChannelMuted(int channel) {
        return debugChannelMuted[channelIndex(channel)];
    }

    public void setDebugChannelMuted(int channel, boolean muted) {
        debugChannelMuted[channelIndex(channel)] = muted;
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

    public int debugLowPassAlpha() {
        return output.lowPassAlpha();
    }

    public void setDebugLowPassAlpha(int lowPassAlpha) {
        output.setLowPassAlpha(lowPassAlpha);
    }

    public void applyDebugVolumes(int masterVolume, int leftVolume, int rightVolume, int[] channelVolumes, boolean[] channelMuted) {
        setDebugMasterVolume(masterVolume);
        setDebugLeftVolume(leftVolume);
        setDebugRightVolume(rightVolume);
        for (int i = 0; i < Math.min(channelVolumes.length, debugChannelVolumes.length); i++) {
            debugChannelVolumes[i] = clampPercent(channelVolumes[i]);
        }
        for (int i = 0; i < Math.min(channelMuted.length, debugChannelMuted.length); i++) {
            debugChannelMuted[i] = channelMuted[i];
        }
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
