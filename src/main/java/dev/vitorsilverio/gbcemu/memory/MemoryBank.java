package dev.vitorsilverio.gbcemu.memory;

public interface MemoryBank {
    String bankName();

    int bankCount();

    int bankSize();

    int currentBank();

    byte readBank(int bank, int offset);

    void writeBank(int bank, int offset, byte value);
}
