package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceE implements Source {

    public final static SourceE INSTANCE = new SourceE();

    private SourceE() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getE();
    }

    @Override
    public String toString() {
        return "E";
    }
}
