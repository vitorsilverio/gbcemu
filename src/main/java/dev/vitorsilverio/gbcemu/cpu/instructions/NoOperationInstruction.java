package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class NoOperationInstruction implements Instruction {

    public static final NoOperationInstruction INSTANCE = new NoOperationInstruction();

    private NoOperationInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public String toString() {
        return "NOP";
    }

    @Override
    public int execute(Cpu cpu) {
        cpu.incrementProgramCounter(1);
        return 4; // NOP takes 4 cycles
    }
}
