package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class InfraredPort implements MemorySpace, Stateful<InfraredState> {

    private static final int RP_REGISTER = 0xFF56;

    private byte data = 0x02;

    @Override
    public InfraredState saveState() {
        return new InfraredState(data);
    }

    @Override
    public void loadState(InfraredState state) {
        data = state.data();
    }

    @Override
    public boolean contains(int address) {
        return address == RP_REGISTER;
    }

    @Override
    public byte read(int address) {
        return data;
    }

    @Override
    public void write(int address, byte value) {
        data = value;
    }
}
