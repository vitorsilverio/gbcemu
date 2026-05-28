package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecialFlagInstructionTest {

    @Test
    void scfPreservesZeroFlag() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setZeroFlag(true);
        cpu.setHalfCarryFlag(true);
        cpu.setNegativeFlag(true);

        CarryFlagInstruction.INSTANCE.execute(cpu);

        assertTrue(cpu.isZeroFlag());
        assertTrue(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isNegativeFlag());
    }

    @Test
    void ccfPreservesZeroFlag() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setZeroFlag(true);
        cpu.setCarryFlag(true);
        cpu.setHalfCarryFlag(true);
        cpu.setNegativeFlag(true);

        ComplementCarryInstruction.INSTANCE.execute(cpu);

        assertTrue(cpu.isZeroFlag());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isNegativeFlag());
    }

    @Test
    void daaUsesUnsignedAccumulatorAndPreservesCarryAfterSubtraction() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x9A);
        cpu.setCarryFlag(true);
        cpu.setNegativeFlag(true);

        int cycles = DecimalAdjustInstruction.INSTANCE.execute(cpu);

        assertTrue(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
        assertEquals(4, cycles);
        assertEquals(0x3A, cpu.getA() & 0xFF);
    }

    @Test
    void daaUsesHighNibbleCarryBeforeLowNibbleAdjustment() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x96);

        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        assertEquals(0x96, cpu.getA() & 0xFF);
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }
}
