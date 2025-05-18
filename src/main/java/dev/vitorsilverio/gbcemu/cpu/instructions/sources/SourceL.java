package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceL implements Source {

    public final static SourceL INSTANCE = new SourceL();

    private SourceL() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getL();
    }

    @Override
    public String toString() {
        return "L";
    }
}
