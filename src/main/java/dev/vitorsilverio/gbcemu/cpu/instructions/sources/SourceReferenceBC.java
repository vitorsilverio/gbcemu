package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceReferenceBC implements Source {

    public final static SourceReferenceBC INSTANCE = new SourceReferenceBC();

    private SourceReferenceBC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getBus().read(cpu.getBc());
    }

    @Override
    public String toString() {
        return "[BC]";
    }
}
