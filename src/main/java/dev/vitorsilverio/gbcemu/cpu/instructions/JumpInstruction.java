package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class JumpInstruction implements Instruction {

    private final Condition condition;
    private final Source source;
    private final boolean relative;
    private final int bytes;
    private final int cyclesIfConditionMet;
    private final int cyclesIfConditionNotMet;

    public JumpInstruction(Condition condition, Source source, int bytes, int cyclesIfConditionMet, int cyclesIfConditionNotMet, boolean relative) {
        this.condition = condition;
        this.source = source;
        this.relative = relative;
        this.bytes = bytes;
        this.cyclesIfConditionMet = cyclesIfConditionMet;
        this.cyclesIfConditionNotMet = cyclesIfConditionNotMet;
    }

    public JumpInstruction(Source source, int bytes, int cyclesIfConditionMet, boolean relative) {
        this(null, source, bytes, cyclesIfConditionMet, 0, relative);
    }


    @Override
    public int execute(Cpu cpu) {
        int address = cpu.getPc();
        if (relative) {
            if (source.getValue(cpu) > 127) {
                address = (address + source.getValue(cpu) - 256) & 0xFFFF;
            } else {
                address += source.getValue(cpu);
            }
            address += bytes;
        } else {
            address = source.getValue(cpu);
        }
        if (condition != null) {
            switch (condition) {
                case CARRY -> {
                    if (cpu.isCarryFlag()) {
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(bytes);
                        return cyclesIfConditionNotMet;
                    }
                }
                case NOT_CARRY -> {
                    if (!cpu.isCarryFlag()) {
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(bytes);
                        return cyclesIfConditionNotMet;
                    }
                }
                case ZERO -> {
                    if (cpu.isZeroFlag()) {
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(bytes);
                        return cyclesIfConditionNotMet;
                    }
                }
                case NOT_ZERO -> {
                    if (!cpu.isZeroFlag()) {
                        cpu.setPc(address);
                        return cyclesIfConditionMet;
                    } else {
                        cpu.incrementProgramCounter(bytes);
                        return cyclesIfConditionNotMet;
                    }
                }
            }
        }
        cpu.setPc(address);
        return cyclesIfConditionMet;
    }

    @Override
    public String toString() {
        return "J" + (relative ? "R":"P") + (condition!= null? " "+condition: "") +" "+ source.toString();
    }
}
