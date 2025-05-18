package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class EnableInterruptsInstruction implements Instruction {

    public static final EnableInterruptsInstruction INSTANCE = new EnableInterruptsInstruction();

    private EnableInterruptsInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        cpu.setIme(true);
        cpu.incrementProgramCounter(1);
        return 4;
    }

    @Override
    public String toString() {
        return "EI";
    }
}
