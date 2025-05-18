package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationAF implements Destination {

    public final static DestinationAF INSTANCE = new DestinationAF();

    private DestinationAF() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setAf(value);
    }

    @Override
    public String toString() {
        return "AF";
    }
}
