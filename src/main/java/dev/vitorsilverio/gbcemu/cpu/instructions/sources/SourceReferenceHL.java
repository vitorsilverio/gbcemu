package dev.vitorsilverio.gbcemu.cpu.instructions.sources;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.Source;

public class SourceReferenceHL implements Source {

    public final static SourceReferenceHL INSTANCE_DEFAULT = new SourceReferenceHL(0);
    public final static SourceReferenceHL INSTANCE_INCREMENT = new SourceReferenceHL(1);
    public final static SourceReferenceHL INSTANCE_DECREMENT = new SourceReferenceHL(-1);

    private final int increment;

    private SourceReferenceHL(int increment) {
        // Private constructor to prevent instantiation
        this.increment = increment;
    }

    @Override
    public int getValue(Cpu cpu) {
        var address = cpu.getHl();
        int value =  cpu.getBus().read(address);
        cpu.setHl((cpu.getHl() + increment) & 0xFFFF);
        return value & 0xFF;
    }

    @Override
    public String toString() {
        return "[HL" + (increment > 0 ? "+" : increment < 0 ? "-" : "") + "]";
    }
}
