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
        var address = cpu.readByte(cpu.getPc() + 1) + 0xff00;
        cpu.writeByte(address, value);
    }

    @Override
    public String toString() {
        return "(a8)";
    }
}
