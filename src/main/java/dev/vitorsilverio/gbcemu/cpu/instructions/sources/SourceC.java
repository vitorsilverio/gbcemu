package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceC implements Source {

    public final static SourceC INSTANCE = new SourceC();

    private SourceC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getC();
    }

    @Override
    public String toString() {
        return "C";
    }
}
