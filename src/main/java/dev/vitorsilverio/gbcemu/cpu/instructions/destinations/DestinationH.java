package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationH implements Destination {

    public final static DestinationH INSTANCE = new DestinationH();

    private DestinationH() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setH((byte) value);
    }

    @Override
    public String toString() {
        return "H";
    }
}
