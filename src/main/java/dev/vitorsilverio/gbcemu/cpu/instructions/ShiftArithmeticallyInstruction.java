package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class ShiftArithmeticallyInstruction implements Instruction {

    private final RotateDirection direction;
    private final Source source;
    private final Destination destination;
    private final int cycles;
    private final boolean keepMostSignificantBit;

    public ShiftArithmeticallyInstruction(RotateDirection direction, Source source, Destination destination, int cycles, boolean keepMostSignificantBit) {
        this.direction = direction;
        this.source = source;
        this.destination = destination;
        this.cycles = cycles;
        this.keepMostSignificantBit = keepMostSignificantBit;
    }

    public ShiftArithmeticallyInstruction(RotateDirection direction, Source source, Destination destination, int cycles) {
        this(direction, source, destination, cycles, false);
    }

    @Override
    public int execute(Cpu cpu) {
        int value = source.getValue(cpu);
        int result;
        if (direction == RotateDirection.LEFT) {
            result = value << 1;
            cpu.setCarryFlag((value & 0x80) != 0);
        } else {
            result = value >> 1;
            if (keepMostSignificantBit) {
                result |= (value & 0x80);
            }
            cpu.setCarryFlag((value & 0x01) != 0);
        }
        cpu.setZeroFlag(result == 0);
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(false);
        destination.setValue(cpu, (byte) result);
        cpu.incrementProgramCounter(1);
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("S%sA %s", direction, destination);
    }
}
