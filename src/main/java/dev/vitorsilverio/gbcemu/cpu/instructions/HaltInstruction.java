package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;

public class HaltInstruction implements Instruction {

    public static final HaltInstruction INSTANCE = new HaltInstruction();

    private HaltInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        cpu.setHalted(true);
        cpu.incrementProgramCounter(1);
        if (cpu.getBus().getPendingInterrupt().isPresent()) {
            cpu.setHalted(false);
            cpu.setHaltBug(true);
        }
        return 4;
    }

    @Override
    public String toString() {
        return "HALT";
    }
}
