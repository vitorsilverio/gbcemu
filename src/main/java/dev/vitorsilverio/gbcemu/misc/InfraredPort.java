package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class InfraredPort implements MemorySpace, Stateful<InfraredState> {

    private static final int RP_REGISTER = 0xFF56;

    private byte control;

    @Override
    public InfraredState saveState() {
        return new InfraredState(control);
    }

    @Override
    public void loadState(InfraredState state) {
        control = (byte) (state.data() & 0xC1);
    }

    @Override
    public boolean contains(int address) {
        return address == RP_REGISTER;
    }

    @Override
    public byte read(int address) {
        return (byte) (0x3E | (control & 0xC1));
    }

    @Override
    public void write(int address, byte value) {
        control = (byte) (value & 0xC1);
    }
}
