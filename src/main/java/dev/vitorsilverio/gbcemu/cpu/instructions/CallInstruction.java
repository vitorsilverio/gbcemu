package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class CallInstruction implements Instruction {

    private final Condition condition;
    private final Source source;
    private final int cyclesIfConditionMet;
    private final int cyclesIfConditionNotMet;

    public CallInstruction(Condition condition, Source source, int cyclesIfConditionMet, int cyclesIfConditionNotMet) {
        this.condition = condition;
        this.source = source;
        this.cyclesIfConditionMet = cyclesIfConditionMet;
        this.cyclesIfConditionNotMet = cyclesIfConditionNotMet;
    }

    public CallInstruction(Source source, int cyclesIfConditionMet) {
        this(null, source, cyclesIfConditionMet, 0);
    }

    @Override
    public int execute(Cpu cpu) {
        var address = source.getValue(cpu);
        cpu.incrementProgramCounter(3);
        if (condition != null) {
            switch (condition) {
                case ZERO -> {
                    if (cpu.isZeroFlag()) {
                        cpu.pushStack(cpu.getPc());
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        return cyclesIfConditionNotMet;
                    }
                }
                case NOT_ZERO -> {
                    if (!cpu.isZeroFlag()) {
                        cpu.pushStack(cpu.getPc());
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        return cyclesIfConditionNotMet;
                    }
                }
                case CARRY -> {
                    if (cpu.isCarryFlag()) {
                        cpu.pushStack(cpu.getPc());
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        return cyclesIfConditionNotMet;
                    }
                }
                case NOT_CARRY -> {
                    if (!cpu.isCarryFlag()) {
                        cpu.pushStack(cpu.getPc());
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        return cyclesIfConditionNotMet;
                    }
                }
            }
        } else {
            cpu.pushStack(cpu.getPc());
            cpu.setPc(address);
            return cyclesIfConditionMet;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "CALL " + (condition!=null? condition + " ": "") + source;
    }
}
