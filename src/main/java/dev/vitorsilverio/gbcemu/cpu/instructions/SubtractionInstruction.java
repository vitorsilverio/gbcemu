package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class SubtractionInstruction implements Instruction {

    private final Source source;
    private final int cycles;
    private final boolean carryFlag;

    public SubtractionInstruction(Source source, int cycles, boolean carryFlag) {
        this.source = source;
        this.cycles = cycles;
        this.carryFlag = carryFlag;
    }

    @Override
    public int execute(Cpu cpu) {
        var value = source.getValue(cpu);
        var result = cpu.getA() - value - (cpu.isCarryFlag() ? 1 : 0);
        cpu.setCarryFlag(result < 0);
        cpu.setHalfCarryFlag(((cpu.getA() & 0x0F) - (value & 0x0F)) < 0);
        cpu.setNegativeFlag(true);
        if (carryFlag) {
            cpu.setZeroFlag(result == 0);
        }
        cpu.setA((byte) result);
        cpu.incrementProgramCounter(1);
        return cycles;
    }

    @Override
    public String toString() {
        return (carryFlag?"SBC ":"SUB") + source.toString();
    }


}
