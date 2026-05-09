package dev.vitorsilverio.gbcemu.audio;

final class ApuRegisterReader {
    private ApuRegisterReader() {
    }

    static byte read(int address, ApuRegisters registers, byte nr52) {
        return read(address, registers, nr52, null);
    }

    static byte read(int address, ApuRegisters registers, byte nr52, WaveChannel channel3) {
        if (registers.isUnusedRegister(address)) {
            return (byte) 0xFF;
        }
        if (registers.isWaveRam(address)) {
            if (channel3 != null && channel3.isPlaying()) {
                return registers.readWaveRamOffset(channel3.currentWaveRamOffset());
            }
            return registers.readWaveRamAddress(address);
        }

        return switch (address) {
            case ApuAddress.NR10_CHANNEL_1_SWEEP -> (byte) ((registers.read(address) & 0x7F) | 0x80);
            case ApuAddress.NR11_CHANNEL_1_DUTY, ApuAddress.NR21_CHANNEL_2_DUTY -> (byte) ((registers.read(address) & 0xC0) | 0x3F);
            case ApuAddress.NR13_CHANNEL_1_FREQUENCY_LO, ApuAddress.NR23_CHANNEL_2_FREQUENCY_LO,
                 ApuAddress.NR33_CHANNEL_3_FREQUENCY_LO -> (byte) 0xFF;
            case ApuAddress.NR14_CHANNEL_1_FREQUENCY_HI, ApuAddress.NR24_CHANNEL_2_FREQUENCY_HI,
                 ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI, ApuAddress.NR44_CHANNEL_4_CONTROL -> (byte) ((registers.read(address) & 0x40) | 0xBF);
            case ApuAddress.NR30_CHANNEL_3_ON_OFF -> (byte) ((registers.read(address) & 0x80) | 0x7F);
            case ApuAddress.NR31_CHANNEL_3_LENGTH, ApuAddress.NR41_CHANNEL_4_LENGTH -> (byte) 0xFF;
            case ApuAddress.NR32_CHANNEL_3_VOLUME -> (byte) ((registers.read(address) & 0x60) | 0x9F);
            case ApuAddress.NR43_CHANNEL_4_FREQUENCY -> registers.read(address);
            case ApuAddress.NR52_AUDIO_MASTER_CONTROL -> nr52;
            case ApuAddress.NR20_UNUSED, ApuAddress.NR40_UNUSED -> (byte) 0xFF;
            default -> registers.read(address);
        };
    }
}
