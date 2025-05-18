package dev.vitorsilverio.gbcemu.memory;

public class EchoRam implements MemorySpace {

    private final WorkRam workRam;

    public EchoRam(WorkRam workRam) {
        this.workRam = workRam;
    }

    @Override
    public boolean contains(int address) {
        return address >= 0xE000 && address < 0xFE00;
    }

    @Override
    public byte read(int address) {
        return workRam.read(address - 0x2000);
    }

    @Override
    public void write(int address, byte value) {
        workRam.write(address - 0x2000, value);
    }
}
