package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationA implements Destination {

    public final static DestinationA INSTANCE = new DestinationA();

    private DestinationA() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setA((byte) value);
    }

    @Override
    public String toString() {
        return "A";
    }
}
