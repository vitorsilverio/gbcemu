package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationHighReference8bits implements Destination {

    public final static DestinationHighReference8bits INSTANCE = new DestinationHighReference8bits();

    private DestinationHighReference8bits() {
        // Private constructor to prevent instantiation
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        var address = cpu.getBus().read(cpu.getPc() + 1) + 0xff00; // Read the next byte to get the high byte
        cpu.getBus().write(address, (byte) value);
    }

    @Override
    public String toString() {
        return "(a8)";
    }
}
