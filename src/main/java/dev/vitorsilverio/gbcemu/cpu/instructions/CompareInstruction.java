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
        int a = cpu.getA() & 0xFF;
        int result = a - value;
        cpu.setCarryFlag(a < value);
        cpu.setHalfCarryFlag((a & 0x0F) < (value & 0x0F));
        cpu.setNegativeFlag(true);
        cpu.setZeroFlag((result & 0xFF) == 0);
        cpu.incrementProgramCounter(bytes);
        return cycles;
    }

    @Override
    public String toString() {
        return "CP " + source.toString();
    }
}
