package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompareInstructionTest {

    @Test
    void comparesUsingEightBitResultForZeroFlag() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x00);

        new CompareInstruction(cpu1 -> 0x100, 4).execute(cpu);

        assertTrue(cpu.isZeroFlag());
        assertTrue(cpu.isNegativeFlag());
        assertTrue(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
    }

    @Test
    void comparesUnsignedAccumulatorAgainstSource() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x80);

        new CompareInstruction(cpu1 -> 0x7F, 4).execute(cpu);

        assertFalse(cpu.isZeroFlag());
        assertFalse(cpu.isCarryFlag());
        assertTrue(cpu.isHalfCarryFlag());
    }
}
