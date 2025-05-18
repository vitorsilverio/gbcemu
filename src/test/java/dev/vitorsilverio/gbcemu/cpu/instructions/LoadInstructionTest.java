package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationReferenceHL;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.ZeroPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadInstructionTest {

    @Test
    public void testIniatilizeStack() {
        // Arrange
        var instruction = new LoadInstruction(
                (cpu) -> 0xfffe,
                Cpu::setSp,
                1,
                4
        );
        Bus bus = new Bus();
        bus.addMemorySpace(new ZeroPage());
        var cpu = new Cpu(bus);

        // Act
        // Simulate the instruction execution
        var cycles = instruction.execute(cpu);

        // Assert
        // Add assertions to verify the expected behavior
        assertEquals(0xfffe, cpu.getSp());
        assertEquals(4, cycles);

    }

    @Test
    public void testLoadHlWith9fff(){
        // Arrange
        var instruction = new LoadInstruction(
                (cpu) -> 0x9fff,
                Cpu::setHl,
                1,
                4
        );
        Bus bus = new Bus();
        bus.addMemorySpace(new ZeroPage());
        var cpu = new Cpu(bus);

        // Act
        // Simulate the instruction execution
        var cycles = instruction.execute(cpu);

        // Assert
        // Add assertions to verify the expected behavior
        assertEquals(0x9fff, cpu.getHl());
        assertEquals(4, cycles);
    }

    @Test
    public void testLoadDecrementHLWithA(){
        // Arrange
        Bus bus = mock(Bus.class);
        when(bus.read(0x9fff)).thenReturn((byte) 0xff);
        var cpu = new Cpu(bus);
        cpu.setA((byte) 0xff);
        cpu.setHl(0x9fff);
        var instruction = new LoadInstruction(
                SourceA.INSTANCE,
                DestinationReferenceHL.INSTANCE_DECREMENT,
                1,
                4
        );

        // Act
        var cycles = instruction.execute(cpu);

        // Assert
        assertEquals(0x9ffe, cpu.getHl());
        assertEquals((byte)0xff, cpu.getBus().read(0x9fff));
        assertEquals(4, cycles);
    }
}
