package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class PopInstruction implements Instruction {

    private final Destination destination;

    public PopInstruction(Destination destination) {
        this.destination = destination;
    }

    @Override
    public int execute(Cpu cpu) {
        int value = cpu.popStack();
        destination.setValue(cpu, value);
        cpu.incrementProgramCounter(1);
        return 12;
    }

    @Override
    public String toString() {
        return "POP " + destination;
    }
}
