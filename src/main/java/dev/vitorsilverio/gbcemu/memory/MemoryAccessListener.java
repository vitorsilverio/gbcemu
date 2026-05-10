package dev.vitorsilverio.gbcemu.memory;

public interface MemoryAccessListener {
    void onRead(int address, byte value);

    void onWrite(int address, byte value);
}
