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
        int a = cpu.getA();

        if (cpu.isNegativeFlag()){
            if (cpu.isHalfCarryFlag()) {
                a -= 0x06;
            }
            if (cpu.isCarryFlag()) {
                a -= 0x60;
            }
        } else {
            if (cpu.isHalfCarryFlag() || (a & 0x0F) > 0x09) {
                a += 0x06;
            }
            if (cpu.isCarryFlag() || a > 0x99) {
                a += 0x60;
            }
        }
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(a > 0xFF);
        cpu.setZeroFlag(a == 0);

        cpu.setA((byte) a);
        cpu.incrementProgramCounter(1);
        return 0;
    }

    @Override
    public String toString() {
        return "DAA";
    }
}
