package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceA implements Source {

    public final static SourceA INSTANCE = new SourceA();

    private SourceA() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getA();
    }

    @Override
    public String toString() {
        return "A";
    }
}
