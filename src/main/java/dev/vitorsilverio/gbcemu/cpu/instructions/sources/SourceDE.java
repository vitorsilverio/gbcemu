package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceDE implements Source {

    public final static SourceDE INSTANCE = new SourceDE();

    private SourceDE() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getDe();
    }

    @Override
    public String toString() {
        return "DE";
    }
}
