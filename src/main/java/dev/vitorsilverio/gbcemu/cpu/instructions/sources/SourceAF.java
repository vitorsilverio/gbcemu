package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceAF implements Source {

    public final static SourceAF INSTANCE = new SourceAF();

    private SourceAF() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getAf();
    }

    @Override
    public String toString() {
        return "AF";
    }
}
