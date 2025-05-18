package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationReferenceDE implements Destination {

    public final static DestinationReferenceDE INSTANCE = new DestinationReferenceDE();

    private DestinationReferenceDE() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        int address = cpu.getDe();
        cpu.getBus().write(address, (byte) value);
    }

    @Override
    public String toString() {
        return "[DE]";
    }
}
