package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
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

class DecrementInstructionDiffblueTest {
    /**
     * Test {@link DecrementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link Source} {@link Source#getValue(Cpu)} return forty-two.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@link Double#SIZE}.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given Source getValue(Cpu) return forty-two; then Cpu(Bus) with bus is Bus (default constructor) Af is SIZE")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecrementInstruction.execute(Cpu)"})
    void testExecute_givenSourceGetValueReturnFortyTwo_thenCpuWithBusIsBusAfIsSize() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        DecrementInstruction decrementInstruction = new DecrementInstruction(source, destination, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        decrementInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(41));
        verify(source).getValue(isA(Cpu.class));
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
        assertTrue(cpu.isNegativeFlag());
        assertEquals(Double.SIZE, cpu.getAf());
    }

    /**
     * Test {@link DecrementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is two hundred twenty-four.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is two hundred twenty-four")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecrementInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsTwoHundredTwentyFour() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(1);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        DecrementInstruction decrementInstruction = new DecrementInstruction(source, destination, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        decrementInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(0));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(224, cpu.getAf());
        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isNegativeFlag());
        assertTrue(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecrementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is zero.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is zero")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecrementInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsZero() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        DecrementInstruction decrementInstruction = new DecrementInstruction(source, destination, 1, false);
        Cpu cpu = new Cpu(new Bus());

        // Act
        decrementInstruction.execute(cpu);

        // Assert that nothing has changed
        verify(destination).setValue(isA(Cpu.class), eq(41));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(0, cpu.getAf());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isNegativeFlag());
        assertFalse(cpu.isZeroFlag());
    }
}
