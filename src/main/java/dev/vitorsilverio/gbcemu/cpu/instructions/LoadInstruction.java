package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class LoadInstruction implements Instruction {


    private final Source source;
    private final Destination destination;
    private final int bytes;
    private final int cycles;

    public LoadInstruction(Source source, Destination destination, int bytes, int cycles) {
        this.source = source;
        this.destination = destination;
        this.bytes = bytes;
        this.cycles = cycles;
    }

    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu);
        destination.setValue(cpu, value);
        cpu.incrementProgramCounter(bytes);
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("LD %s, %s", destination, source);
    }
}
