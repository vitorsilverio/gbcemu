package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceH implements Source {

    public final static SourceH INSTANCE = new SourceH();

    private SourceH() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getH();
    }

    @Override
    public String toString() {
        return "H";
    }
}
