package dev.vitorsilverio.gbcemu.audio;

class WaveChannel extends SoundChannel {
    private int period;
    private int sampleIndex;
    private int lastSample;

    WaveChannel(ApuContext context) {
        super(context);
    }

    WaveChannelState saveState() {
        return new WaveChannelState(saveCommonState(), period, sampleIndex, lastSample);
    }

    void loadState(WaveChannelState state) {
        loadCommonState(state.common());
        period = state.period();
        sampleIndex = state.sampleIndex() & 0x1F;
        lastSample = state.lastSample() & 0x0F;
    }

    void updatePeriod() {
        period = context.period(ApuAddress.NR33_CHANNEL_3_FREQUENCY_LO, ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI);
    }

    void trigger() {
        boolean lengthWasZero = lengthTimer == 0;
        enabled = true;
        if (lengthWasZero) {
            lengthTimer = 256;
        }
        if (lengthWasZero) {
            clockLengthAfterTriggerIfNeeded(ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI);
        }
        if ((context.register(ApuAddress.NR30_CHANNEL_3_ON_OFF) & 0x80) == 0) {
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
            lastSample = readWaveSample();
        }
    }

    @Override
    int output() {
        if ((context.register(ApuAddress.NR30_CHANNEL_3_ON_OFF) & 0x80) == 0) {
            return 0;
        }
        return 8 - digitalOutput();
    }

    @Override
    int digitalOutput() {
        if (!enabled) {
            return 0;
        }
        int sample = lastSample;
        int volumeCode = (context.register(ApuAddress.NR32_CHANNEL_3_VOLUME) >> 5) & 0x03;
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

    ApuChannelDebugSnapshot debugSnapshot() {
        int periodDistance = 2048 - period;
        double frequencyHz = periodDistance <= 0 ? 0 : 65_536.0 / periodDistance;
        int volumeCode = (context.register(ApuAddress.NR32_CHANNEL_3_VOLUME) >> 5) & 0x03;
        return debugSnapshot(
                3,
                "CH3 Wave",
                (context.register(ApuAddress.NR30_CHANNEL_3_ON_OFF) & 0x80) != 0,
                period,
                frequencyHz,
                sampleIndex,
                volumeCode,
                "volumeCode=" + volumeCode + " sample=" + lastSample
        );
    }

    boolean isPlaying() {
        return enabled;
    }

    int currentWaveRamOffset() {
        return sampleIndex >> 1;
    }

    private int readWaveSample() {
        int packed = context.wavePatternRam(sampleIndex >> 1) & 0xFF;
        return (sampleIndex & 1) == 0 ? packed >> 4 : packed & 0x0F;
    }

    void tickLength() {
        tickLength(ApuAddress.NR34_CHANNEL_3_FREQUENCY_HI);
    }

    @Override
    protected void disable() {
        super.disable();
        sampleIndex = 0;
        lastSample = 0;
    }
}
