package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class ComplementInstruction implements Instruction {

    public static final ComplementInstruction INSTANCE = new ComplementInstruction();

    private ComplementInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        // Complement the value in register A
        int a = cpu.getA();
        a = a ^ 0xFF;
        cpu.setNegativeFlag(true);
        cpu.setHalfCarryFlag(true);
        cpu.setA((byte) a); // Ensure the result is 8 bits
        cpu.incrementProgramCounter(1);
        return 4;
    }

    @Override
    public String toString() {
        return "CPL";
    }
}
