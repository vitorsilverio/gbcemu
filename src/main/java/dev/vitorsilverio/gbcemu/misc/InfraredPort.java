package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;

public class InfraredPort implements MemorySpace {

    private static final int RP_REGISTER = 0xFF56;

    private byte data = 0x02;

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
