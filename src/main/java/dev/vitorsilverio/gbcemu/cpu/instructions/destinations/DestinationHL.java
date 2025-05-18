package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationHL implements Destination {

    public final static DestinationHL INSTANCE = new DestinationHL();

    private DestinationHL() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setHl(value);
    }

    @Override
    public String toString() {
        return "HL";
    }
}
