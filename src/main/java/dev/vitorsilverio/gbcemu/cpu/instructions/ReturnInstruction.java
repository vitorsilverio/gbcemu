package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class ReturnInstruction implements Instruction {

    private final Condition condition;
    private final int cyclesIfConditionMet;
    private final int cyclesIfConditionNotMet;
    private final boolean enableInterrupts;

    public ReturnInstruction(Condition condition, int cyclesIfConditionMet, int cyclesIfConditionNotMet, boolean enableInterrupts) {
        this.condition = condition;
        this.cyclesIfConditionMet = cyclesIfConditionMet;
        this.cyclesIfConditionNotMet = cyclesIfConditionNotMet;
        this.enableInterrupts = enableInterrupts;
    }

    public ReturnInstruction(int cyclesIfConditionMet, boolean enableInterrupts) {
        this(null, cyclesIfConditionMet, 0, enableInterrupts);
    }

    public ReturnInstruction(Condition condition, int cyclesIfConditionMet, int cyclesIfConditionNotMet) {
        this(condition, cyclesIfConditionMet, cyclesIfConditionNotMet, false);
    }

    @Override
    public int execute(Cpu cpu) {
        if (condition != null) {
            switch (condition) {
                case ZERO -> {
                    if (cpu.isZeroFlag()) {
                        cpu.setPc(cpu.popStack());
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(1);
                        return cyclesIfConditionNotMet;
                    }
                }
                case NOT_ZERO -> {
                    if (!cpu.isZeroFlag()) {
                        cpu.setPc(cpu.popStack());
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(1);
                        return cyclesIfConditionNotMet;
                    }
                }
                case CARRY -> {
                    if (cpu.isCarryFlag()) {
                        cpu.setPc(cpu.popStack());
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(1);
                        return cyclesIfConditionNotMet;
                    }
                }
                case NOT_CARRY -> {
                    if (!cpu.isCarryFlag()) {
                        cpu.setPc(cpu.popStack());
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(1);
                        return cyclesIfConditionNotMet;
                    }
                }
            }
        }
        if (enableInterrupts) {
            cpu.setIme(true);
        }
        cpu.setPc(cpu.popStack());
        return cyclesIfConditionMet;
    }

    @Override
    public String toString() {
        if (condition != null) {
            return "RET " + condition;
        }
        return "RET";
    }
}
