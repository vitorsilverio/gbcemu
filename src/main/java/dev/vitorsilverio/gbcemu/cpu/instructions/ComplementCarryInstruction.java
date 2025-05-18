package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class ComplementCarryInstruction implements Instruction {

    public static final ComplementCarryInstruction INSTANCE = new ComplementCarryInstruction();

    private ComplementCarryInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        // Complement the carry flag
        cpu.setCarryFlag(!cpu.isCarryFlag());
        cpu.setHalfCarryFlag(false);
        cpu.setNegativeFlag(false);
        cpu.setZeroFlag(false);
        cpu.incrementProgramCounter(1);
        return 4;
    }

    @Override
    public String toString() {
        return "CCF";
    }
}
