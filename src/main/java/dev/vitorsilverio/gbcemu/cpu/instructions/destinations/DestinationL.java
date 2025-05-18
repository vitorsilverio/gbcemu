package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationL implements Destination {

    public final static DestinationL INSTANCE = new DestinationL();

    private DestinationL() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setL((byte) value);
    }

    @Override
    public String toString() {
        return "L";
    }
}
