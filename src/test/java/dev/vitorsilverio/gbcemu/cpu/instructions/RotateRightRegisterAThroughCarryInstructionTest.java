package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class RotateRightRegisterAThroughCarryInstructionTest {

    @Test
    public void testRRA() {
        // Arrange
        var instruction = RotateRightRegisterAThroughCarryInstruction.INSTANCE;
        var bus = mock(Bus.class);
        var cpu = new Cpu(bus);
        cpu.setA((byte) 0b01010101);
        cpu.setCarryFlag(false);

        // Act
        var cycles = instruction.execute(cpu);

        // Assert
        assertEquals(0b00101010, cpu.getA());
        assertTrue(cpu.isCarryFlag());
        assertEquals(4, cycles);
        assertFalse(cpu.isZeroFlag());
    }
}
