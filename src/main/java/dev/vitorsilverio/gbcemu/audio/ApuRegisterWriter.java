package dev.vitorsilverio.gbcemu.audio;

import java.util.function.Consumer;

final class ApuRegisterWriter {
    private ApuRegisterWriter() {
    }

    static void write(
            int address,
            byte value,
            ApuRegisters registers,
            boolean audioEnabled,
            Consumer<Boolean> audioEnabledSetter,
            PulseChannel channel1,
            PulseChannel channel2,
            WaveChannel channel3,
            NoiseChannel channel4
    ) {
        if (address == ApuAddress.PCM12_CGB_DIGITAL_OUTPUT || address == ApuAddress.PCM34_CGB_DIGITAL_OUTPUT) {
            return;
        }
        if (registers.isUnusedRegister(address)) {
            return;
        }
        if (registers.isWaveRam(address)) {
            if (audioEnabled && channel3.isPlaying()) {
                registers.writeWaveRamOffset(channel3.currentWaveRamOffset(), value);
                return;
            }
            registers.writeWaveRamAddress(address, value);
            return;
        }

        if (address == ApuAddress.NR52_AUDIO_MASTER_CONTROL) {
            audioEnabledSetter.accept((value & 0x80) != 0);
            return;
        }

        if (!audioEnabled) {
            return;
        }

        byte oldValue = registers.read(address);
        registers.write(address, value);
        switch (address) {
            case ApuAddress.NR10_CHANNEL_1_SWEEP -> channel1.setSweep(oldValue, value);
            case ApuAddress.NR11_CHANNEL_1_DUTY -> channel1.setLength(64 - (value & 0x3F));
            case ApuAddress.NR12_CHANNEL_1_VOLUME -> channel1.setEnvelope(value);
            case ApuAddress.NR13_CHANNEL_1_FREQUENCY_LO -> channel1.updatePeriod();
            case ApuAddress.NR14_CHANNEL_1_FREQUENCY_HI -> {
                channel1.clockLengthOnEnable(oldValue, value);
                channel1.updatePeriod();
                if ((value & 0x80) != 0) {
                    channel1.trigger();
                }
            }
            case ApuAddress.NR21_CHANNEL_2_DUTY -> channel2.setLength(64 - (value & 0x3F));
            case ApuAddress.NR22_CHANNEL_2_VOLUME -> channel2.setEnvelope(value);
            case ApuAddress.NR23_CHANNEL_2_FREQUENCY_LO -> channel2.updatePeriod();
            case ApuAddress.NR24_CHANNEL_2_FREQUENCY_HI -> {
                channel2.clockLengthOnEnable(oldValue, value);
                channel2.updatePeriod();
                if ((value & 0x80) != 0) {
                    channel2.trigger();
                }
            }
            case ApuAddress.NR30_CHANNEL_3_ON_OFF -> {
                if ((value & 0x80) == 0) {
                    channel3.enabled = false;
                }
            }
            case ApuAddress.NR31_CHANNEL_3_LENGTH -> channel3.lengthTimer = 256 - (value & 0xFF);
            case ApuAddress.NR33_CHANNEL_3_FREQUENCY_LO -> channel3.updatePeriod();
            case ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI -> {
                channel3.clockLengthOnEnable(oldValue, value);
                channel3.updatePeriod();
                if ((value & 0x80) != 0) {
                    channel3.trigger();
                }
            }
            case ApuAddress.NR41_CHANNEL_4_LENGTH -> channel4.lengthTimer = 64 - (value & 0x3F);
            case ApuAddress.NR42_CHANNEL_4_VOLUME -> channel4.setEnvelope(value);
            case ApuAddress.NR44_CHANNEL_4_CONTROL -> {
                channel4.clockLengthOnEnable(oldValue, value);
                if ((value & 0x80) != 0) {
                    channel4.trigger();
                }
            }
            default -> {
            }
        }
    }
}
