package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceBC implements Source {

    public final static SourceBC INSTANCE = new SourceBC();

    private SourceBC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getBc();
    }

    @Override
    public String toString() {
        return "BC";
    }
}
