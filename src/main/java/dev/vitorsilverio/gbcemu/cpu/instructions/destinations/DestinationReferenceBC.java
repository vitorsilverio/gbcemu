package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationReferenceBC implements Destination {

    public final static DestinationReferenceBC INSTANCE = new DestinationReferenceBC();

    private DestinationReferenceBC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        int address = cpu.getBc();
        cpu.getBus().write(address, (byte) value);
    }

    @Override
    public String toString() {
        return "[BC]";
    }
}
