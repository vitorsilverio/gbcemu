package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceHighReferenceC implements Source {

    public final static SourceHighReferenceC INSTANCE = new SourceHighReferenceC();

    private SourceHighReferenceC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int getValue(dev.vitorsilverio.gbcemu.cpu.Cpu cpu) {
        var address = cpu.getC() + 0xff00;
        return cpu.getBus().read(address);
    }

    @Override
    public String toString() {
        return "(C)";
    }
}
