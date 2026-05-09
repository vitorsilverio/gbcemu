package dev.vitorsilverio.gbcemu.audio;

class WaveChannel extends SoundChannel {
    private int period;
    private int sampleIndex;

    WaveChannel(ApuContext context) {
        super(context);
    }

    WaveChannelState saveState() {
        return new WaveChannelState(saveCommonState(), period, sampleIndex);
    }

    void loadState(WaveChannelState state) {
        loadCommonState(state.common());
        period = state.period();
        sampleIndex = state.sampleIndex() & 0x1F;
    }

    void updatePeriod() {
        period = context.period(Apu.NR33_CHANNEL_3_FREQUENCY_LO, Apu.NR34_CHANNEL_3_FREQUENCY_HI);
    }

    void trigger() {
        boolean lengthWasZero = lengthTimer == 0;
        enabled = true;
        if (lengthWasZero) {
            lengthTimer = 256;
        }
        if (lengthWasZero) {
            clockLengthAfterTriggerIfNeeded(Apu.NR34_CHANNEL_3_FREQUENCY_HI);
        }
        if ((context.register(Apu.NR30_CHANNEL_3_ON_OFF) & 0x80) == 0) {
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
        int packed = context.wavePatternRam(sampleIndex / 2) & 0xFF;
        int sample = (sampleIndex & 1) == 0 ? packed >> 4 : packed & 0x0F;
        int volumeCode = (context.register(Apu.NR32_CHANNEL_3_VOLUME) >> 5) & 0x03;
        return switch (volumeCode) {
            case 0 -> 0;
            case 1 -> sample;
            case 2 -> sample >> 1;
            case 3 -> sample >> 2;
            default -> 0;
        };
    }

    private int waveTimerPeriod() {
        return Math.max(2, (2048 - period) * 2);
    }

    void tickLength() {
        tickLength(Apu.NR34_CHANNEL_3_FREQUENCY_HI);
    }
}
