package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationDE implements Destination {

    public final static DestinationDE INSTANCE = new DestinationDE();

    private DestinationDE() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setDe(value);
    }

    @Override
    public String toString() {
        return "DE";
    }
}
