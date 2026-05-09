package dev.vitorsilverio.gbcemu.audio;

final class ApuAddress {
    static final int REGISTER_START = 0xFF10;
    static final int REGISTER_END = 0xFF3F;
    static final int WAVE_RAM_START = 0xFF30;
    static final int WAVE_RAM_END = 0xFF3F;

    static final int NR10_CHANNEL_1_SWEEP = 0xFF10;
    static final int NR11_CHANNEL_1_DUTY = 0xFF11;
    static final int NR12_CHANNEL_1_VOLUME = 0xFF12;
    static final int NR13_CHANNEL_1_FREQUENCY_LO = 0xFF13;
    static final int NR14_CHANNEL_1_FREQUENCY_HI = 0xFF14;

    static final int NR20_UNUSED = 0xFF15;
    static final int NR21_CHANNEL_2_DUTY = 0xFF16;
    static final int NR22_CHANNEL_2_VOLUME = 0xFF17;
    static final int NR23_CHANNEL_2_FREQUENCY_LO = 0xFF18;
    static final int NR24_CHANNEL_2_FREQUENCY_HI = 0xFF19;

    static final int NR30_CHANNEL_3_ON_OFF = 0xFF1A;
    static final int NR31_CHANNEL_3_LENGTH = 0xFF1B;
    static final int NR32_CHANNEL_3_VOLUME = 0xFF1C;
    static final int NR33_CHANNEL_3_FREQUENCY_LO = 0xFF1D;
    static final int NR34_CHANNEL_3_FREQUENCY_HI = 0xFF1E;

    static final int NR40_UNUSED = 0xFF1F;
    static final int NR41_CHANNEL_4_LENGTH = 0xFF20;
    static final int NR42_CHANNEL_4_VOLUME = 0xFF21;
    static final int NR43_CHANNEL_4_FREQUENCY = 0xFF22;
    static final int NR44_CHANNEL_4_CONTROL = 0xFF23;

    static final int NR50_MASTER_VOLUME = 0xFF24;
    static final int NR51_SOUND_PANNING = 0xFF25;
    static final int NR52_AUDIO_MASTER_CONTROL = 0xFF26;
    static final int PCM12_CGB_DIGITAL_OUTPUT = 0xFF76;
    static final int PCM34_CGB_DIGITAL_OUTPUT = 0xFF77;

    private ApuAddress() {
    }
}
