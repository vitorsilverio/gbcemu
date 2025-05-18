package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class StopInstruction implements Instruction {

    public static final StopInstruction INSTANCE = new StopInstruction();

    private StopInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        // Stop the CPU until an interrupt occurs
        cpu.setStopped(true);
        cpu.incrementProgramCounter(2);
        return 4;
    }

    @Override
    public String toString() {
        return "STOP n8";
    }
}
