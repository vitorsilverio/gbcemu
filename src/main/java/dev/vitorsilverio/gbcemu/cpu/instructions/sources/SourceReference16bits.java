package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceReference16bits implements Source {

    public final static SourceReference16bits INSTANCE = new SourceReference16bits();

    private SourceReference16bits() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        int address = cpu.readWord(cpu.getPc() + 1);
        return cpu.readByte(address);
    }

    @Override
    public String toString() {
        return "[a16]";
    }
}
