package dev.vitorsilverio.gbcemu.memory;

public interface MemorySpace {

    boolean contains(int address);
    byte read(int address);
    void write(int address, byte value);
}
