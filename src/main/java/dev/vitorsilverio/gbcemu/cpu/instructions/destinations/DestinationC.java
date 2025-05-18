package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationC implements Destination {

    public final static DestinationC INSTANCE = new DestinationC();

    private DestinationC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setC((byte) value);
    }

    @Override
    public String toString() {
        return "C";
    }
}
