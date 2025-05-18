package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceHighReference8bits implements Source {

    public final static SourceHighReference8bits INSTANCE = new SourceHighReference8bits();

    private SourceHighReference8bits() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(dev.vitorsilverio.gbcemu.cpu.Cpu cpu) {
        var address = cpu.getBus().read(cpu.getPc()+1) + 0xff00;
        return cpu.getBus().read(address);
    }

    @Override
    public String toString() {
        return "(a8)";
    }
}
