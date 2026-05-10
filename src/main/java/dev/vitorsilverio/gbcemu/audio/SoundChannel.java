package dev.vitorsilverio.gbcemu.audio;

abstract class SoundChannel {
    protected final ApuContext context;
    protected boolean enabled;
    protected int lengthTimer;
    protected int currentVolume;
    protected int envelopeTimer;
    protected int timer;

    SoundChannel(ApuContext context) {
        this.context = context;
    }

    protected SoundChannelState saveCommonState() {
        return new SoundChannelState(enabled, lengthTimer, currentVolume, envelopeTimer, timer);
    }

    protected void loadCommonState(SoundChannelState state) {
        enabled = state.enabled();
        lengthTimer = state.lengthTimer();
        currentVolume = state.currentVolume();
        envelopeTimer = state.envelopeTimer();
        timer = state.timer();
    }

    protected void setEnvelope(byte value) {
        if ((value & 0xF8) == 0) {
            enabled = false;
        }
    }

    protected boolean envelopeDacEnabled(int envelopeRegisterAddress) {
        return (context.register(envelopeRegisterAddress) & 0xF8) != 0;
    }

    protected void triggerEnvelope(int envelopeRegisterAddress) {
        currentVolume = (context.register(envelopeRegisterAddress) >> 4) & 0x0F;
        envelopeTimer = context.register(envelopeRegisterAddress) & 0x07;
    }

    protected void tickEnvelope(int envelopeRegisterAddress) {
        int envelope = context.register(envelopeRegisterAddress) & 0xFF;
        int pace = envelope & 0x07;
        if (!enabled || pace == 0) {
            return;
        }
        envelopeTimer--;
        if (envelopeTimer > 0) {
            return;
        }
        envelopeTimer = pace;
        if ((envelope & 0x08) != 0 && currentVolume < 15) {
            currentVolume++;
        } else if ((envelope & 0x08) == 0 && currentVolume > 0) {
            currentVolume--;
        }
    }

    protected void tickLength(int controlRegisterAddress) {
        if ((context.register(controlRegisterAddress) & 0x40) == 0 || lengthTimer <= 0) {
            return;
        }
        lengthTimer--;
        if (lengthTimer == 0) {
            enabled = false;
        }
    }

    protected void clockLengthOnEnable(byte oldValue, byte newValue) {
        boolean oldEnabled = (oldValue & 0x40) != 0;
        boolean newEnabled = (newValue & 0x40) != 0;
        if (!oldEnabled && newEnabled && shouldClockLengthOnEnable() && lengthTimer > 0) {
            lengthTimer--;
            if (lengthTimer == 0) {
                enabled = false;
            }
        }
    }

    private boolean shouldClockLengthOnEnable() {
        return (context.frameSequencerStep() & 1) == 0;
    }

    protected void clockLengthAfterTriggerIfNeeded(int controlRegisterAddress) {
        if ((context.register(controlRegisterAddress) & 0x40) != 0 && shouldClockLengthOnEnable() && lengthTimer > 0) {
            lengthTimer--;
            if (lengthTimer == 0) {
                enabled = false;
            }
        }
    }

    protected void disable() {
        enabled = false;
        timer = 0;
        lengthTimer = 0;
        currentVolume = 0;
        envelopeTimer = 0;
    }

    abstract void tick();

    abstract int output();

    abstract int digitalOutput();

    protected ApuChannelDebugSnapshot debugSnapshot(
            int channel,
            String name,
            boolean dacEnabled,
            int period,
            double frequencyHz,
            int sequencerPosition,
            int extra,
            String detail
    ) {
        return new ApuChannelDebugSnapshot(
                channel,
                name,
                enabled,
                dacEnabled,
                period,
                frequencyHz,
                lengthTimer,
                currentVolume,
                envelopeTimer,
                timer,
                digitalOutput(),
                output(),
                sequencerPosition,
                extra,
                detail
        );
    }
}
