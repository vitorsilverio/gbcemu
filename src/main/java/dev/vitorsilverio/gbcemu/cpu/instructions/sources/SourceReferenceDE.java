package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceReferenceDE implements Source {

    public final static SourceReferenceDE INSTANCE = new SourceReferenceDE();

    private SourceReferenceDE() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getBus().read(cpu.getDe());
    }

    @Override
    public String toString() {
        return "[DE]";
    }
}
