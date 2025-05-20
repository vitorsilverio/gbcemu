package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationA;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class RotateInstructionTest {

    @Test
    public void testRLA() {
        // Arrange
        var instruction = new RotateInstruction(RotateDirection.LEFT, SourceA.INSTANCE, DestinationA.INSTANCE, 4, false, false, true);
        var bus = mock(Bus.class);
        var cpu = new Cpu(bus);
        cpu.setA((byte) 0b10101010);
        cpu.setCarryFlag(false);

        // Act
        var cycles = instruction.execute(cpu);

        // Assert
        assertEquals(0b01010100, cpu.getA());
        assertTrue(cpu.isCarryFlag());
        assertEquals(4, cycles);
        assertFalse(cpu.isZeroFlag());
    }
}
