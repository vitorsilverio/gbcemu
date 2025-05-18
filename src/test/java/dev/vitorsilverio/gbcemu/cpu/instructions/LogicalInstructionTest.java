package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogicalInstructionTest {

    @Test
    public void testXorA(){
        //Arrange
        Bus bus = new Bus();
        var cpu = new Cpu(bus);
        var instruction = new LogicalInstruction(
                LogicalOperation.XOR,
                Cpu::getA,
                4
        );

        //Act
        var cycles = instruction.execute(cpu);


        //Assert
        assertEquals(0, cpu.getA());
        assertEquals(4, cycles);
        assertTrue(cpu.isZeroFlag());
        assertFalse(cpu.isNegativeFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isCarryFlag());

    }
}
