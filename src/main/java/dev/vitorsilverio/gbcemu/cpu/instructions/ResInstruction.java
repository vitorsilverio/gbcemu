package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class ResInstruction implements Instruction {

    private final int bit;
    private final Source source;
    private final Destination destination;
    private final int cycles;

    public ResInstruction(int bit, Source source, Destination destination, int cycles) {
        this.bit = bit;
        this.source = source;
        this.destination = destination;
        this.cycles = cycles;
    }


    @Override
    public int execute(Cpu cpu) {
        var value = source.getValue(cpu);
        var mask = ~(1 << bit);
        var result = value & mask;
        destination.setValue(cpu, (byte) result);
        cpu.incrementProgramCounter(1);
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("RES %d, %s", bit, source);
    }
}
