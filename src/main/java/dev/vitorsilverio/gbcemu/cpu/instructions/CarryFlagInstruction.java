package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class CarryFlagInstruction implements Instruction {

    public static final CarryFlagInstruction INSTANCE = new CarryFlagInstruction();

    private CarryFlagInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        // Set the carry flag to 1
        cpu.setCarryFlag(true);
        // Set the half carry flag to 0
        cpu.setHalfCarryFlag(false);
        // Set the negative flag to 0
        cpu.setNegativeFlag(false);
        // Set the zero flag to 0
        cpu.setZeroFlag(false);
        // Increment the program counter by 1 byte
        cpu.incrementProgramCounter(1);
        return 4; // 4 cycles for this instruction
    }

    @Override
    public String toString() {
        return "SFC";
    }
}
