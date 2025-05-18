package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceHL implements Source {

    public final static SourceHL INSTANCE = new SourceHL();

    private SourceHL() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getHl();
    }

    @Override
    public String toString() {
        return "HL";
    }
}
