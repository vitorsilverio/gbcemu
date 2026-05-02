package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class DecimalAdjustInstruction implements Instruction {

    public static final DecimalAdjustInstruction INSTANCE = new DecimalAdjustInstruction();

    private DecimalAdjustInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        int a = cpu.getA() & 0xFF;
        boolean carry = cpu.isCarryFlag();

        if (cpu.isNegativeFlag()) {
            if (cpu.isCarryFlag()) {
                a -= 0x60;
            }
            if (cpu.isHalfCarryFlag()) {
                a -= 0x06;
            }
        } else {
            if (cpu.isCarryFlag() || a > 0x99) {
                a += 0x60;
                carry = true;
            }
            if (cpu.isHalfCarryFlag() || (a & 0x0F) > 0x09) {
                a += 0x06;
            }
        }
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(carry);
        cpu.setZeroFlag((a & 0xFF) == 0);

        cpu.setA((byte) a);
        cpu.incrementProgramCounter(1);
        return 4;
    }

    @Override
    public String toString() {
        return "DAA";
    }
}
