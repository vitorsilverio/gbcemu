package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class DecrementInstruction implements Instruction {
    private final Source source;
    private final Destination destination;
    private final int cycles;
    private final boolean arithmetic;

    public DecrementInstruction(Source source, Destination destination, int cycles, boolean arithmetic) {
        this.source = source;
        this.destination = destination;
        this.cycles = cycles;
        this.arithmetic = arithmetic;
    }

    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu) - 1;
        destination.setValue(cpu, value);
        cpu.incrementProgramCounter(1);
        if(arithmetic){
            // Set the zero flag if the result is zero
            cpu.setZeroFlag(value == 0x00);

            // Set the half carry flag if the result is less than 0x10
            cpu.setHalfCarryFlag((value & 0x0F) == 0x00);

            cpu.setNegativeFlag(true);
        }
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("DEC %s", source);
    }

}
