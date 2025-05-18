package dev.vitorsilverio.gbcemu.memory;

public class ZeroPage implements MemorySpace{

    private final byte[] memory = new byte[0x7F];

    @Override
    public boolean contains(int address) {
        return 0xff80 <= address && address < 0xfffe;
    }

    @Override
    public byte read(int address) {
        return memory[address - 0xff80];
    }

    @Override
    public void write(int address, byte value) {
        memory[address - 0xff80] = value;
    }
}
