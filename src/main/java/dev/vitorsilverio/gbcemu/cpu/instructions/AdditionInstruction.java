package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationA;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;

public class AdditionInstruction implements Instruction {


    private final Source source1;
    private final Source source2;
    private final Destination destination;
    private final int bytes;
    private final int cycles;
    private final boolean carryFlag;
    private final boolean zeroFlag;
    private final boolean wordOperation;

    public AdditionInstruction(Source source1, Source source2, Destination destination, int bytes, int cycles, boolean carryFlag, boolean zeroFlag, boolean wordOperation) {
        this.source1 = source1;
        this.source2 = source2;
        this.destination = destination;
        this.bytes = bytes;
        this.cycles = cycles;
        this.carryFlag = carryFlag;
        this.zeroFlag = zeroFlag;
        this.wordOperation = wordOperation;
    }

    /**
     * Constructor for AdditionInstruction for sum of 2 16bit registers
     * @param source1
     * @param source2
     * @param destination
     */
    public AdditionInstruction(Source source1, Source source2, Destination destination) {
        this(source1, source2, destination, 1, 8, false, false, true);
    }

    public AdditionInstruction(Source source, int cycles, boolean carryFlag) {
        this(source, cycles, carryFlag, 1);
    }

    public AdditionInstruction(Source source, int cycles, boolean carryFlag, int bytes) {
        this(source, SourceA.INSTANCE, DestinationA.INSTANCE, bytes, cycles, carryFlag, true, false);
    }

    @Override
    public int execute(Cpu cpu) {
        var value1 = source1.getValue(cpu);
        var value2 = source2.getValue(cpu);
        int carry = carryFlag && cpu.isCarryFlag() ? 1 : 0;
        var sum = value1 + value2 + carry;
        if (wordOperation) {
            cpu.setCarryFlag(sum > 0xFFFF);
            cpu.setHalfCarryFlag(((value1 & 0x0FFF) + (value2 & 0x0FFF)) > 0x0FFF);
        } else {
            cpu.setCarryFlag(sum > 0xFF);
            cpu.setHalfCarryFlag(((value1 & 0x0F) + (value2 & 0x0F) + carry) > 0x0F);
        }
        cpu.setNegativeFlag(false);
        if (zeroFlag) {
            cpu.setZeroFlag((sum & 0xFF) == 0);
        }
        destination.setValue(cpu, sum);
        cpu.incrementProgramCounter(bytes);
        return cycles;
    }

    @Override
    public String toString() {
        return String.format("AD%s %s, %s", (carryFlag?"C":"D"), destination, source1);
    }
}
