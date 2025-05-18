package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationHighReferenceC implements Destination {

    public final static DestinationHighReferenceC INSTANCE = new DestinationHighReferenceC();

    private DestinationHighReferenceC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        var address = cpu.getC() + 0xff00;
        cpu.getBus().write(address, (byte) value);
    }

    @Override
    public String toString() {
        return "(C)";
    }
}
