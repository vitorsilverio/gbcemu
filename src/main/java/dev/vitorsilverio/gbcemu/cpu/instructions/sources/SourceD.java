package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceD implements Source {

    public final static SourceD INSTANCE = new SourceD();

    private SourceD() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(Cpu cpu) {
        return cpu.getD();
    }

    @Override
    public String toString() {
        return "D";
    }
}
