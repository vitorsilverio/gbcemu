package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class RotateInstruction implements Instruction {

    private final RotateDirection direction;
    private final Source source;
    private final Destination destination;
    private final int cycles;
    private final boolean carryFlag;
    private final boolean zeroFlag;
    private final boolean throughCarry;

    public RotateInstruction(RotateDirection direction, Source source, Destination destination, int cycles, boolean carryFlag, boolean zeroFlag, boolean throughCarry) {
        this.direction = direction;
        this.source = source;
        this.destination = destination;
        this.cycles = cycles;
        this.carryFlag = carryFlag;
        this.zeroFlag = zeroFlag;
        this.throughCarry = throughCarry;
    }

    @Override
    public int execute(Cpu cpu) {
        var value = source.getValue(cpu);
        int bitout;
        if (RotateDirection.LEFT.equals(direction)) {
            bitout = (value & 0x80) != 0 ? 1 : 0;
            value = (value << 1) | (carryFlag && cpu.isCarryFlag() ? 1 : 0);
        } else {
            bitout = (value & 0x01) != 0 ? 1 : 0;
            value = (value >> 1) | (carryFlag && cpu.isCarryFlag() ? 0x80 : 0);
        }
        if (carryFlag) {
            cpu.setCarryFlag(bitout != 0);
        }
        if(throughCarry) {
            cpu.setCarryFlag(bitout != 0);
        }
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(false);
        if(zeroFlag){
            cpu.setZeroFlag(value == 0);
        } else {
            cpu.setZeroFlag(false);
        }
        destination.setValue(cpu, value);
        cpu.incrementProgramCounter(1);
        return cycles;
    }

    @Override
    public String toString() {
        return "R" + direction.getDirection() + (carryFlag ? "C" : "") + (zeroFlag?" ":"") + source;
    }
}
