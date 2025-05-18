package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationB implements Destination {

    public final static DestinationB INSTANCE = new DestinationB();

    private DestinationB() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setB((byte) value);
    }

    @Override
    public String toString() {
        return "B";
    }
}
