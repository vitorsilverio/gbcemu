package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class PushInstruction implements Instruction {

    private final Source source;

    public PushInstruction(Source source) {
        this.source = source;
    }

    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu);
        cpu.pushStack(value);
        cpu.incrementProgramCounter(1);
        return 12;
    }

    @Override
    public String toString() {
        return "PUSH " + source;
    }
}
