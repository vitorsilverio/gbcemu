package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class ResetInstruction implements Instruction {

    private final int address;

    public ResetInstruction(int address) {
        this.address = address;
    }

    @Override
    public int execute(Cpu cpu) {
        // Push the current program counter onto the stack
        cpu.pushStack(cpu.getPc());
        // Set the program counter to the reset vector address
        cpu.setPc(address);
        return 16; // 16 cycles for a reset instruction
    }

    @Override
    public String toString() {
        return "RST " + String.format("#%02X", address);
    }
}
