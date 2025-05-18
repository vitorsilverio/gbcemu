package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceSP implements Source {

    public final static SourceSP INSTANCE = new SourceSP();

    private SourceSP() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getSp();
    }

    @Override
    public String toString() {
        return "SP";
    }
}
