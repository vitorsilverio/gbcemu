package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BitInstructionTest {

    @Test
    public void testBit7OfAwithValue0x00() {
        // Arrange
        var instruction = new BitInstruction(7, SourceA.INSTANCE, 8);
        var bus = new Bus();
        var cpu = new Cpu(bus);
        cpu.setA((byte) 0x00);

        // Act
        var cycles = instruction.execute(cpu);

        // Assert
        assertEquals(8, cycles);
        assertEquals(0x00, cpu.getA());
        assertTrue(cpu.isZeroFlag());
    }

    @Test
    public void testBit7OfAwithValue0x80() {
        // Arrange
        var instruction = new BitInstruction(7, SourceA.INSTANCE, 8);
        var bus = new Bus();
        var cpu = new Cpu(bus);
        cpu.setA((byte) 0x80);

        // Act
        var cycles = instruction.execute(cpu);

        // Assert
        assertEquals(8, cycles);
        assertEquals((byte)0x80, cpu.getA());
        assertFalse(cpu.isZeroFlag());
    }
}
