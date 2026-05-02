package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class SubtractionInstruction implements Instruction {

    private final Source source;
    private final int cycles;
    private final boolean carryFlag;
    private final int bytes;

    public SubtractionInstruction(Source source, int cycles, boolean carryFlag) {
        this(source, cycles, carryFlag, 1);
    }

    public SubtractionInstruction(Source source, int cycles, boolean carryFlag, int bytes) {
        this.source = source;
        this.cycles = cycles;
        this.carryFlag = carryFlag;
        this.bytes = bytes;
    }

    @Override
    public int execute(Cpu cpu) {
        var value = source.getValue(cpu);
        int a = cpu.getA() & 0xFF;
        int carry = carryFlag && cpu.isCarryFlag() ? 1 : 0;
        var result = a - value - carry;
        cpu.setCarryFlag(result < 0);
        cpu.setHalfCarryFlag(((a & 0x0F) - (value & 0x0F) - carry) < 0);
        cpu.setNegativeFlag(true);
        cpu.setZeroFlag((result & 0xFF) == 0);
        cpu.setA((byte) result);
        cpu.incrementProgramCounter(bytes);
        return cycles;
    }

    @Override
    public String toString() {
        return (carryFlag?"SBC ":"SUB") + source.toString();
    }


}
