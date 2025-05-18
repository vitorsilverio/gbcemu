package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class IncrementInstruction implements Instruction {
    private final Source source;
    private final Destination destination;
    private final int cycles;
    private final boolean arithmetic;

    public IncrementInstruction(Source source, Destination destination, int cycles, boolean arithmetic) {
        this.source = source;
        this.destination = destination;
        this.cycles = cycles;
        this.arithmetic = arithmetic;
    }

    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu);
        destination.setValue(cpu, value + 1);
        cpu.incrementProgramCounter(1);
        if(arithmetic){
            // Set the zero flag if the result is zero
            cpu.setZeroFlag(value == 0xFF);
            // Set the half carry flag if there was a carry from bit 3 to bit 4
            cpu.setHalfCarryFlag((value & 0x0F) == 0x0F);
            cpu.setNegativeFlag(false);
        }
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("INC %s", source);
    }
}
