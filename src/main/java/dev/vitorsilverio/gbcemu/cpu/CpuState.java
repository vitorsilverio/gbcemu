package dev.vitorsilverio.gbcemu.cpu;

import java.io.Serializable;

public record CpuState(
        int instructionTicks,
        int speedRate,
        int pc,
        int sp,
        byte a,
        byte b,
        byte c,
        byte d,
        byte e,
        byte h,
        byte l,
        boolean zeroFlag,
        boolean negativeFlag,
        boolean halfCarryFlag,
        boolean carryFlag,
        boolean halted,
        boolean stopped,
        boolean ime,
        int imeEnableDelay,
        boolean haltBug
) implements Serializable {
}
