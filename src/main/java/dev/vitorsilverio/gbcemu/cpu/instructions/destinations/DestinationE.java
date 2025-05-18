package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationE implements Destination {

    public final static DestinationE INSTANCE = new DestinationE();

    private DestinationE() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        cpu.setE((byte) (value & 0xFF));
    }

    @Override
    public String toString() {
        return "E";
    }
}
