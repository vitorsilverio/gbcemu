package dev.vitorsilverio.gbcemu.audio;

class PulseChannel extends SoundChannel {
    private static final int[][] DUTY_PATTERNS = {
            {0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 0}
    };

    private final int channel;
    private int period;
    private int dutyStep;
    private int sweepShadowPeriod;
    private int sweepTimer;
    private boolean sweepEnabled;
    private boolean sweepNegateUsed;

    PulseChannel(ApuContext context, int channel) {
        super(context);
        this.channel = channel;
    }

    PulseChannelState saveState() {
        return new PulseChannelState(
                saveCommonState(),
                period,
                dutyStep,
                sweepShadowPeriod,
                sweepTimer,
                sweepEnabled,
                sweepNegateUsed
        );
    }

    void loadState(PulseChannelState state) {
        loadCommonState(state.common());
        period = state.period();
        dutyStep = state.dutyStep() & 0x07;
        sweepShadowPeriod = state.sweepShadowPeriod();
        sweepTimer = state.sweepTimer();
        sweepEnabled = state.sweepEnabled();
        sweepNegateUsed = state.sweepNegateUsed();
    }

    void setLength(int length) {
        lengthTimer = length == 0 ? 64 : length;
    }

    void updatePeriod() {
        period = channel == 0
                ? context.period(Apu.NR13_CHANNEL_1_FREQUENCY_LO, Apu.NR14_CHANNEL_1_FREQUENCY_HI)
                : context.period(Apu.NR23_CHANNEL_2_FREQUENCY_LO, Apu.NR24_CHANNEL_2_FREQUENCY_HI);
    }

    void setSweep(byte oldValue, byte newValue) {
        if (channel != 0) {
            return;
        }
        boolean wasNegate = (oldValue & 0x08) != 0;
        boolean isNegate = (newValue & 0x08) != 0;
        if (wasNegate && !isNegate && sweepNegateUsed) {
            enabled = false;
        }
    }

    void trigger() {
        int envelopeAddress = channel == 0 ? Apu.NR12_CHANNEL_1_VOLUME : Apu.NR22_CHANNEL_2_VOLUME;
        boolean lengthWasZero = lengthTimer == 0;
        enabled = true;
        if (lengthWasZero) {
            lengthTimer = 64;
        }
        if (lengthWasZero) {
            clockLengthAfterTriggerIfNeeded(channel == 0 ? Apu.NR14_CHANNEL_1_FREQUENCY_HI : Apu.NR24_CHANNEL_2_FREQUENCY_HI);
        }
        boolean sweepAllowsChannel = triggerSweep();
        if ((context.register(envelopeAddress) & 0xF8) == 0) {
            enabled = false;
            return;
        }
        if (!sweepAllowsChannel) {
            enabled = false;
            return;
        }
        enabled = true;
        triggerEnvelope(envelopeAddress);
        timer = pulseTimerPeriod();
    }

    @Override
    void tick() {
        if (!enabled) {
            return;
        }
        timer--;
        if (timer <= 0) {
            timer += pulseTimerPeriod();
            dutyStep = (dutyStep + 1) & 0x07;
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
        int dutyAddress = channel == 0 ? Apu.NR11_CHANNEL_1_DUTY : Apu.NR21_CHANNEL_2_DUTY;
        int duty = (context.register(dutyAddress) >> 6) & 0x03;
        return DUTY_PATTERNS[duty][dutyStep] == 0 ? 0 : currentVolume;
    }

    private int pulseTimerPeriod() {
        return Math.max(4, (2048 - period) * 4);
    }

    void tickLength() {
        tickLength(channel == 0 ? Apu.NR14_CHANNEL_1_FREQUENCY_HI : Apu.NR24_CHANNEL_2_FREQUENCY_HI);
    }

    void tickEnvelope() {
        tickEnvelope(channel == 0 ? Apu.NR12_CHANNEL_1_VOLUME : Apu.NR22_CHANNEL_2_VOLUME);
    }

    private boolean triggerSweep() {
        if (channel != 0) {
            return true;
        }
        sweepShadowPeriod = period;
        sweepTimer = sweepPace();
        if (sweepTimer == 0) {
            sweepTimer = 8;
        }
        sweepEnabled = sweepPace() != 0 || sweepStep() != 0;
        sweepNegateUsed = false;
        if (sweepStep() == 0) {
            return true;
        }
        return calculateSweepPeriod() <= 0x7FF;
    }

    void tickSweep() {
        if (channel != 0 || !sweepEnabled) {
            return;
        }
        sweepTimer--;
        if (sweepTimer > 0) {
            return;
        }
        sweepTimer = sweepPace();
        if (sweepTimer == 0) {
            sweepTimer = 8;
        }
        if (sweepPace() == 0) {
            return;
        }

        int calculatedPeriod = calculateSweepPeriod();
        if (calculatedPeriod > 0x7FF) {
            enabled = false;
            return;
        }
        if (sweepStep() == 0) {
            return;
        }

        sweepShadowPeriod = calculatedPeriod;
        period = calculatedPeriod;
        writeChannel1Period(calculatedPeriod);
        if (calculateSweepPeriod() > 0x7FF) {
            enabled = false;
        }
    }

    private int calculateSweepPeriod() {
        int delta = sweepShadowPeriod >> sweepStep();
        if (sweepNegate()) {
            sweepNegateUsed = true;
            return (sweepShadowPeriod - delta) & 0x7FF;
        }
        return sweepShadowPeriod + delta;
    }

    private void writeChannel1Period(int value) {
        context.setRegister(Apu.NR13_CHANNEL_1_FREQUENCY_LO, (byte) value);
        int high = context.register(Apu.NR14_CHANNEL_1_FREQUENCY_HI) & 0xF8;
        context.setRegister(Apu.NR14_CHANNEL_1_FREQUENCY_HI, (byte) (high | ((value >> 8) & 0x07)));
    }

    private int sweepPace() {
        return (context.register(Apu.NR10_CHANNEL_1_SWEEP) >> 4) & 0x07;
    }

    private boolean sweepNegate() {
        return (context.register(Apu.NR10_CHANNEL_1_SWEEP) & 0x08) != 0;
    }

    private int sweepStep() {
        return context.register(Apu.NR10_CHANNEL_1_SWEEP) & 0x07;
    }
}
