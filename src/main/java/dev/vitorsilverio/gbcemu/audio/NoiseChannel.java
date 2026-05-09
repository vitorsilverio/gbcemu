package dev.vitorsilverio.gbcemu.audio;

class NoiseChannel extends SoundChannel {
    private int lfsr;

    NoiseChannel(ApuContext context) {
        super(context);
    }

    NoiseChannelState saveState() {
        return new NoiseChannelState(saveCommonState(), lfsr);
    }

    void loadState(NoiseChannelState state) {
        loadCommonState(state.common());
        lfsr = state.lfsr() & 0x7FFF;
    }

    void trigger() {
        boolean lengthWasZero = lengthTimer == 0;
        enabled = true;
        if (lengthWasZero) {
            lengthTimer = 64;
        }
        if (lengthWasZero) {
            clockLengthAfterTriggerIfNeeded(ApuAddress.NR44_CHANNEL_4_CONTROL);
        }
        if ((context.register(ApuAddress.NR42_CHANNEL_4_VOLUME) & 0xF8) == 0) {
            enabled = false;
            return;
        }
        enabled = true;
        lfsr = 0;
        triggerEnvelope(ApuAddress.NR42_CHANNEL_4_VOLUME);
        timer = noiseTimerPeriod();
    }

    @Override
    void tick() {
        if (!enabled) {
            return;
        }
        timer--;
        if (timer <= 0) {
            timer += noiseTimerPeriod();
            if (((context.register(ApuAddress.NR43_CHANNEL_4_FREQUENCY) >> 4) & 0x0F) >= 14) {
                return;
            }
            int nextBit = ((lfsr & 1) ^ ((lfsr >> 1) & 1)) ^ 1;
            lfsr = (lfsr >> 1) | (nextBit << 14);
            if ((context.register(ApuAddress.NR43_CHANNEL_4_FREQUENCY) & 0x08) != 0) {
                lfsr = (lfsr & ~(1 << 6)) | (nextBit << 6);
            }
        }
    }

    @Override
    int output() {
        if (!envelopeDacEnabled(ApuAddress.NR42_CHANNEL_4_VOLUME)) {
            return 0;
        }
        return 8 - digitalOutput();
    }

    @Override
    int digitalOutput() {
        if (!enabled) {
            return 0;
        }
        return (lfsr & 1) == 0 ? currentVolume : 0;
    }

    private int noiseTimerPeriod() {
        int value = context.register(ApuAddress.NR43_CHANNEL_4_FREQUENCY) & 0xFF;
        int divisorCode = value & 0x07;
        int divisor = divisorCode == 0 ? 8 : divisorCode * 16;
        int shift = (value >> 4) & 0x0F;
        return Math.max(8, divisor << shift);
    }

    void tickLength() {
        tickLength(ApuAddress.NR44_CHANNEL_4_CONTROL);
    }

    void tickEnvelope() {
        tickEnvelope(ApuAddress.NR42_CHANNEL_4_VOLUME);
    }

    @Override
    protected void disable() {
        super.disable();
        lfsr = 0;
    }
}
