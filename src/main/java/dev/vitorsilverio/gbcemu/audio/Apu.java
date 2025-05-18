package dev.vitorsilverio.gbcemu.audio;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;

import java.util.List;

public class Apu implements MemorySpace, MachineCycle {

    private final static int NR50_MASTER_VOLUME = 0xFF24;
    private final static int NR51_SOUND_PANNING = 0xFF25;
    private final static int NR52_AUDIO_MASTER_CONTROL = 0xFF26;

    private final static int NR10_CHANNEL_1_SWEEP = 0xFF10;
    private final static int NR11_CHANNEL_1_DUTY = 0xFF11;
    private final static int NR12_CHANNEL_1_VOLUME = 0xFF12;
    private final static int NR13_CHANNEL_1_FREQUENCY_LO = 0xFF13;
    private final static int NR14_CHANNEL_1_FREQUENCY_HI = 0xFF14;

    private final static int NR20_CHANNEL_2_SWEEP = 0xFF15;
    private final static int NR21_CHANNEL_2_DUTY = 0xFF16;
    private final static int NR22_CHANNEL_2_VOLUME = 0xFF17;
    private final static int NR23_CHANNEL_2_FREQUENCY_LO = 0xFF18;
    private final static int NR24_CHANNEL_2_FREQUENCY_HI = 0xFF19;

    private final static int NR30_CHANNEL_3_ON_OFF = 0xFF1A;
    private final static int NR31_CHANNEL_3_LENGTH = 0xFF1B;
    private final static int NR32_CHANNEL_3_VOLUME = 0xFF1C;
    private final static int NR33_CHANNEL_3_FREQUENCY_LO = 0xFF1D;
    private final static int NR34_CHANNEL_3_FREQUENCY_HI = 0xFF1E;

    private final static int NR40_CHANNEL_4_SWEEP = 0xFF1F;
    private final static int NR41_CHANNEL_4_LENGTH = 0xFF20;
    private final static int NR42_CHANNEL_4_VOLUME = 0xFF21;
    private final static int NR43_CHANNEL_4_FREQUENCY = 0xFF22;
    private final static int NR44_CHANNEL_4_FREQUENCY_HI = 0xFF23;

    private final static List<Integer> REGISTERS = List.of(
            NR50_MASTER_VOLUME,
            NR51_SOUND_PANNING,
            NR52_AUDIO_MASTER_CONTROL,
            NR10_CHANNEL_1_SWEEP,
            NR11_CHANNEL_1_DUTY,
            NR12_CHANNEL_1_VOLUME,
            NR13_CHANNEL_1_FREQUENCY_LO,
            NR14_CHANNEL_1_FREQUENCY_HI,
            NR20_CHANNEL_2_SWEEP,
            NR21_CHANNEL_2_DUTY,
            NR22_CHANNEL_2_VOLUME,
            NR23_CHANNEL_2_FREQUENCY_LO,
            NR24_CHANNEL_2_FREQUENCY_HI,
            NR30_CHANNEL_3_ON_OFF,
            NR31_CHANNEL_3_LENGTH,
            NR32_CHANNEL_3_VOLUME,
            NR33_CHANNEL_3_FREQUENCY_LO,
            NR34_CHANNEL_3_FREQUENCY_HI,
            NR40_CHANNEL_4_SWEEP,
            NR41_CHANNEL_4_LENGTH,
            NR42_CHANNEL_4_VOLUME,
            NR43_CHANNEL_4_FREQUENCY,
            NR44_CHANNEL_4_FREQUENCY_HI
    );

    private final byte[] wavePatternRam = new byte[0x20];

    @Override
    public void tick() {

    }

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address) || (address >= 0xFF30 && address < 0xFF40);
    }

    @Override
    public byte read(int address) {
        return (byte)0xff;
    }

    @Override
    public void write(int address, byte value) {

    }
}
