package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class BitInstruction implements Instruction {

    private final int bit;
    private final Source source;
    private final int cycles;

    public BitInstruction(int bit, Source source, int cycles) {
        this.bit = bit;
        this.source = source;
        this.cycles = cycles;
    }


    @Override
    public int execute(Cpu cpu) {
        var value = source.getValue(cpu);
        var result = value & (1 << bit);
        cpu.setZeroFlag(result == 0);
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(true);
        cpu.incrementProgramCounter(1);
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("BIT %d, %s", bit, source);
    }
}
