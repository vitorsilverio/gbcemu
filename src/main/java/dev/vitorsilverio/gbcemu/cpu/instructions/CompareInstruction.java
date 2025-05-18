package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class CompareInstruction implements Instruction {

    private final Source source;
    private final int bytes;
    private final int cycles;

    public CompareInstruction(Source source, int bytes, int cycles) {
        this.source = source;
        this.bytes = bytes;
        this.cycles = cycles;
    }

    public CompareInstruction(Source source, int cycles) {
        this(source, 1, cycles);
    }

    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu);
        int result = (cpu.getA() & 0x00ff) - value;
        cpu.setCarryFlag(result < 0);
        cpu.setHalfCarryFlag(((cpu.getA() & 0x0F) - (value & 0x0F)) < 0);
        cpu.setNegativeFlag(true);
        cpu.setZeroFlag(result == 0);
        cpu.incrementProgramCounter(bytes);
        return cycles;
    }

    @Override
    public String toString() {
        return "CP " + source.toString();
    }
}
