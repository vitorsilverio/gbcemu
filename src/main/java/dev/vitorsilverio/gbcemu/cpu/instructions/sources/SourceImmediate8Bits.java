package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceImmediate8Bits implements Source {

    public final static SourceImmediate8Bits INSTANCE = new SourceImmediate8Bits();

    private SourceImmediate8Bits() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return  cpu.getBus().read(cpu.getPc() + 1);
    }

    @Override
    public String toString() {
        return "n8";
    }
}
