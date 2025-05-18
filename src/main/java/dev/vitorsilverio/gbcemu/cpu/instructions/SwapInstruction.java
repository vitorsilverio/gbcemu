package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class SwapInstruction implements Instruction {

    private final Source source;
    private final Destination destination;
    private final int cycles; // Number of cycles for the instruction

    public SwapInstruction(Source source, Destination destination, int cycles) {
        this.source = source;
        this.destination = destination;
        this.cycles = cycles;
    }


    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu);
        int swappedValue = ((value & 0x0F) << 4) | ((value & 0xF0) >> 4);
        destination.setValue(cpu, swappedValue);
        cpu.setZeroFlag(swappedValue == 0);
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.incrementProgramCounter(1);
        return cycles;
    }

    @Override
    public String toString() {
        return "SWAP " + source;
    }
}
