package dev.vitorsilverio.gbcemu.peripherals;

import java.io.Serializable;

public record TimerState(
        int systemCounter,
        byte timerCounter,
        byte timerModulo,
        byte timerControl,
        int overflowDelay
) implements Serializable {
}
