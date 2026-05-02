package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.Instruction;
import dev.vitorsilverio.gbcemu.misc.Key1;

public class StopInstruction implements Instruction {

    public static final StopInstruction INSTANCE = new StopInstruction();

    private StopInstruction() {
        // Private constructor to prevent instantiation
    }

    @Override
    public int execute(Cpu cpu) {
        cpu.getBus().findMemorySpace(Key1.class)
                .filter(Key1::switchSpeedIfPrepared)
                .ifPresent(key1 -> cpu.setSpeedRate(key1.isDoubleSpeed() ? 2 : 1));
        cpu.incrementProgramCounter(2);
        return 4;
    }

    @Override
    public String toString() {
        return "STOP n8";
    }
}
