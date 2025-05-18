package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationD implements Destination {

    public final static DestinationD INSTANCE = new DestinationD();

    private DestinationD() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setD((byte) value);
    }

    @Override
    public String toString() {
        return "D";
    }
}
