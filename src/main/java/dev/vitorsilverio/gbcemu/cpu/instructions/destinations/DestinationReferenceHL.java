package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationReferenceHL implements Destination {

    public final static DestinationReferenceHL INSTANCE_DEFAULT = new DestinationReferenceHL(0);
    public final static DestinationReferenceHL INSTANCE_INCREMENT = new DestinationReferenceHL(1);
    public final static DestinationReferenceHL INSTANCE_DECREMENT = new DestinationReferenceHL(-1);

    private final int increment;

    private DestinationReferenceHL(int increment) {
        // Private constructor to prevent instantiation
        this.increment = increment;
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        int address = cpu.getHl();
        cpu.getBus().write(address, (byte) value);
        cpu.setHl((cpu.getHl() + increment) & 0xFFFF);
    }

    @Override
    public String toString() {
        return "[HL"+ (increment > 0 ? "+" : increment < 0 ? "-" : "") + "]";
    }
}
