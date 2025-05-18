package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationReference16bits implements Destination {

    public final static DestinationReference16bits INSTANCE = new DestinationReference16bits();

    private DestinationReference16bits() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        int address = cpu.getBus().readWord(cpu.getPc());
        cpu.getBus().write(address, (byte) value);
    }

    @Override
    public String toString() {
        return "[a16]";
    }
}
