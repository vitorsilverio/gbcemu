package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class LogicalInstruction implements Instruction {

    private final LogicalOperation operation;
    private final Source source;
    private final int bytes;
    private final int cycles;

    public LogicalInstruction(LogicalOperation operation, Source source, int bytes, int cycles) {
        this.operation = operation;
        this.source = source;
        this.bytes = bytes;
        this.cycles = cycles;
    }

    public LogicalInstruction(LogicalOperation operation, Source source, int cycles) {
        this(operation, source, 1, cycles);
    }

    @Override
    public int execute(Cpu cpu) {
        var a = cpu.getA();
        var b = source.getValue(cpu);
        var result = operation.apply(a, b);
        cpu.setA(result);
        cpu.setZeroFlag(result == 0);
        cpu.setNegativeFlag(false);
        cpu.setCarryFlag(false);
        cpu.setHalfCarryFlag(LogicalOperation.AND.equals(operation));
        cpu.incrementProgramCounter(bytes);
        return cycles;
    }

    @Override
    public String toString() {
        return operation.name() + " " + source;
    }
}
