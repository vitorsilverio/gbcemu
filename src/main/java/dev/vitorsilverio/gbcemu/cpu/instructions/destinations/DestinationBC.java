package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationBC implements Destination {

    public final static DestinationBC INSTANCE = new DestinationBC();

    private DestinationBC() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setBc(value);
    }

    @Override
    public String toString() {
        return "BC";
    }
}
