package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtractionInstructionTest {

    @Test
    void subDoesNotIncludeCarryUnlessInstructionIsSbc() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x10);
        cpu.setCarryFlag(true);

        new SubtractionInstruction(cpu1 -> 0x01, 4, false).execute(cpu);

        assertEquals(0x0F, cpu.getA() & 0xFF);
        assertFalse(cpu.isCarryFlag());
    }

    @Test
    void sbcIncludesCarryInBorrowCalculation() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x10);
        cpu.setCarryFlag(true);

        new SubtractionInstruction(cpu1 -> 0x00, 4, true).execute(cpu);

        assertEquals(0x0F, cpu.getA() & 0xFF);
        assertTrue(cpu.isHalfCarryFlag());
        assertFalse(cpu.isCarryFlag());
    }

    @Test
    void immediateSubConsumesOpcodeAndOperand() {
        Cpu cpu = new Cpu(new Bus());

        new SubtractionInstruction(cpu1 -> 0x00, 8, false, 2).execute(cpu);

        assertEquals(2, cpu.getPc());
        assertTrue(cpu.isZeroFlag());
    }
}
