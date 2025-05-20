package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationA;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;

public class RotateLeftRegisterAThroughCarryInstruction extends RotateInstruction {

    public static final RotateLeftRegisterAThroughCarryInstruction INSTANCE = new RotateLeftRegisterAThroughCarryInstruction();

    private RotateLeftRegisterAThroughCarryInstruction() {
        super(RotateDirection.LEFT, SourceA.INSTANCE, DestinationA.INSTANCE, 4, false, false, true);
    }

    @Override
    public String toString() {
        return "RLA";
    }
}
