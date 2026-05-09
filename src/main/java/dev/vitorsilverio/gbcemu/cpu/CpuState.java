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
    public int aUnsigned() {
        return a & 0xFF;
    }

    public int bUnsigned() {
        return b & 0xFF;
    }

    public int cUnsigned() {
        return c & 0xFF;
    }

    public int dUnsigned() {
        return d & 0xFF;
    }

    public int eUnsigned() {
        return e & 0xFF;
    }

    public int hUnsigned() {
        return h & 0xFF;
    }

    public int lUnsigned() {
        return l & 0xFF;
    }

    public int af() {
        return (aUnsigned() << 8)
                | (zeroFlag ? 0x80 : 0)
                | (negativeFlag ? 0x40 : 0)
                | (halfCarryFlag ? 0x20 : 0)
                | (carryFlag ? 0x10 : 0);
    }

    public int bc() {
        return (bUnsigned() << 8) | cUnsigned();
    }

    public int de() {
        return (dUnsigned() << 8) | eUnsigned();
    }

    public int hl() {
        return (hUnsigned() << 8) | lUnsigned();
    }
}
