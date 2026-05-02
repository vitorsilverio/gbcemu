package dev.vitorsilverio.gbcemu.cpu.instructions.destinations;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Destination;

public class DestinationReference16bits implements Destination {

    public final static DestinationReference16bits INSTANCE = new DestinationReference16bits(false);
    public final static DestinationReference16bits INSTANCE_WORD = new DestinationReference16bits(true);

    private final boolean word;

    private DestinationReference16bits(boolean word) {
        // Private constructor to prevent instantiation
        this.word = word;
    }

    @Override
    public void setValue(Cpu cpu, int value) {
        int address = cpu.getBus().readWord(cpu.getPc() + 1);
        if (word) {
            cpu.getBus().writeWord(address, value);
        } else {
            cpu.getBus().write(address, (byte) value);
        }
    }

    @Override
    public String toString() {
        return "[a16]";
    }
}
