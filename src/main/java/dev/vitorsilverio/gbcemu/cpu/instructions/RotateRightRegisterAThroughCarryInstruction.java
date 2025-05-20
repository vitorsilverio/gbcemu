package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationA;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;

public class RotateRightRegisterAThroughCarryInstruction extends RotateInstruction {

    public static final RotateRightRegisterAThroughCarryInstruction INSTANCE = new RotateRightRegisterAThroughCarryInstruction();

    private RotateRightRegisterAThroughCarryInstruction() {
        super(RotateDirection.RIGHT, SourceA.INSTANCE, DestinationA.INSTANCE, 4, false, false, true);
    }

    @Override
    public String toString() {
        return "RRA";
    }
}
