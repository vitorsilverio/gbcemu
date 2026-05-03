package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class AddSignedImmediateToStackPointerInstruction implements Instruction {

    private final Destination destination;
    private final int cycles;

    public AddSignedImmediateToStackPointerInstruction(Destination destination, int cycles) {
        this.destination = destination;
        this.cycles = cycles;
    }

    @Override
    public int execute(Cpu cpu) {
        int sp = cpu.getSp();
        int immediate = cpu.readByte(cpu.getPc() + 1);
        int signedImmediate = (byte) immediate;
        int result = (sp + signedImmediate) & 0xFFFF;

        cpu.setZeroFlag(false);
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(((sp & 0x0F) + (immediate & 0x0F)) > 0x0F);
        cpu.setCarryFlag(((sp & 0xFF) + immediate) > 0xFF);
        destination.setValue(cpu, result);
        cpu.incrementProgramCounter(2);
        return cycles;
    }

    @Override
    public String toString() {
        return destination + ", SP+e8";
    }
}
