package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceB implements Source {

    public final static SourceB INSTANCE = new SourceB();

    private SourceB() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getB();
    }

    @Override
    public String toString() {
        return "B";
    }
}
