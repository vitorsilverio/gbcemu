package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementDecrementInstructionTest {

    @Test
    void decrementSetsHalfCarryWhenBorrowingFromBitFour() {
        Cpu cpu = new Cpu(new Bus());

        new DecrementInstruction(cpu1 -> 0x10, (cpu1, value) -> {
        }, 4, true).execute(cpu);

        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isNegativeFlag());
        assertFalse(cpu.isZeroFlag());
    }

    @Test
    void decrementToZeroDoesNotSetHalfCarryWithoutNibbleBorrow() {
        Cpu cpu = new Cpu(new Bus());

        new DecrementInstruction(cpu1 -> 0x01, (cpu1, value) -> {
        }, 4, true).execute(cpu);

        assertFalse(cpu.isHalfCarryFlag());
        assertTrue(cpu.isNegativeFlag());
        assertTrue(cpu.isZeroFlag());
    }

    @Test
    void incrementSetsZeroFromEightBitResult() {
        Cpu cpu = new Cpu(new Bus());

        new IncrementInstruction(cpu1 -> 0xFF, (cpu1, value) -> {
        }, 4, true).execute(cpu);

        assertTrue(cpu.isZeroFlag());
        assertTrue(cpu.isHalfCarryFlag());
        assertFalse(cpu.isNegativeFlag());
    }
}
