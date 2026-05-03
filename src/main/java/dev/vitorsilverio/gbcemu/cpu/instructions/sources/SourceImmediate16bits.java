package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceImmediate16bits implements Source {

    public final static SourceImmediate16bits INSTANCE = new SourceImmediate16bits();

    private SourceImmediate16bits() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.readWord(cpu.getPc() + 1);
    }

    @Override
    public String toString() {
        return "n16";
    }
}
