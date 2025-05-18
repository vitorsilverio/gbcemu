package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diffblue.cover.annotations.MethodsUnderTest;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LogicalInstructionDiffblueTest {
    /**
     * Test {@link LogicalInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link LogicalInstruction#LogicalInstruction(LogicalOperation, Source, int)} with operation is {@code OR} and {@link Source} and cycles is one.</li>
     * </ul>
     * <p>
     * Method under test: {@link LogicalInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given LogicalInstruction(LogicalOperation, Source, int) with operation is 'OR' and Source and cycles is one")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int LogicalInstruction.execute(Cpu)"})
    void testExecute_givenLogicalInstructionWithOperationIsOrAndSourceAndCyclesIsOne() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        LogicalInstruction logicalInstruction = new LogicalInstruction(LogicalOperation.OR, source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        logicalInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(10752, cpu.getAf());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
        assertEquals('*', cpu.getA());
    }

    /**
     * Test {@link LogicalInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link LogicalInstruction#LogicalInstruction(LogicalOperation, Source, int)} with operation is {@code XOR} and {@link Source} and cycles is one.</li>
     * </ul>
     * <p>
     * Method under test: {@link LogicalInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given LogicalInstruction(LogicalOperation, Source, int) with operation is 'XOR' and Source and cycles is one")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int LogicalInstruction.execute(Cpu)"})
    void testExecute_givenLogicalInstructionWithOperationIsXorAndSourceAndCyclesIsOne() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        LogicalInstruction logicalInstruction = new LogicalInstruction(LogicalOperation.XOR, source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        logicalInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(10752, cpu.getAf());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
        assertEquals('*', cpu.getA());
    }

    /**
     * Test {@link LogicalInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) A is zero.</li>
     * </ul>
     * <p>
     * Method under test: {@link LogicalInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) A is zero")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int LogicalInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAIsZero() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        LogicalInstruction logicalInstruction = new LogicalInstruction(LogicalOperation.AND, source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        logicalInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals((byte) 0, cpu.getA());
        assertEquals(160, cpu.getAf());
        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }
}
