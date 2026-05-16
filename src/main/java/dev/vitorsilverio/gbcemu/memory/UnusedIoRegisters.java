package dev.vitorsilverio.gbcemu.memory;

public class UnusedIoRegisters implements MemorySpace {

    private final byte[] memory = new byte[0x80];

    @Override
    public boolean contains(int address) {
        return address == 0xFF03 ||
                (address >= 0xFF08 && address <= 0xFF0E) ||
                (address >= 0xFF27 && address <= 0xFF2F) ||
                address == 0xFF4E ||
                (address >= 0xFF57 && address <= 0xFF67) ||
                (address >= 0xFF6D && address <= 0xFF6F) ||
                address == 0xFF71 ||
                address == 0xFF7F;
    }

    @Override
    public byte read(int address) {
        return memory[address - 0xFF00];
    }

    @Override
    public void write(int address, byte value) {
        memory[address - 0xFF00] = value;
    }
}
