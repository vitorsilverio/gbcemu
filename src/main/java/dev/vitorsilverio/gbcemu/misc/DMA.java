package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;

public class DMA implements MemorySpace, MachineCycle {

    private static final int DMA_REQUEST_REGISTER = 0xff46;

    private int cycles = 0;
    private int baseAddress = 0;
    private boolean active;
    private final Bus bus;

    public DMA(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void tick() {
        int offset = 0xa0 - cycles;
        cycles--;
        var value = bus.read((baseAddress << 8) | offset);
        bus.write(0xfe00 | offset, value);
        if (cycles == 0) {
            active = false;
        }
    }

    @Override
    public boolean contains(int address) {
        return address == DMA_REQUEST_REGISTER;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        baseAddress = value & 0xFF;
        active = true;
        cycles = 160;
    }

    public boolean isActive() {
        return active;
    }
}
