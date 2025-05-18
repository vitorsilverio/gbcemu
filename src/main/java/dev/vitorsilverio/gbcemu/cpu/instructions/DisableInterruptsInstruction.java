package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class DisableInterruptsInstruction implements Instruction {

    public static final DisableInterruptsInstruction INSTANCE = new DisableInterruptsInstruction();

    private DisableInterruptsInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        cpu.setIme(false);
        cpu.incrementProgramCounter(1);
        return 4;
    }

    @Override
    public String toString() {
        return "DI";
    }
}
