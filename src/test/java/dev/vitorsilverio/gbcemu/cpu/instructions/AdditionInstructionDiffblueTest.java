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

class AdditionInstructionDiffblueTest {
    /**
     * Test {@link AdditionInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link Source} {@link Source#getValue(Cpu)} return {@code 65535}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is forty-eight.</li>
     * </ul>
     * <p>
     * Method under test: {@link AdditionInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given Source getValue(Cpu) return '65535'; then Cpu(Bus) with bus is Bus (default constructor) Af is forty-eight")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int AdditionInstruction.execute(Cpu)"})
    void testExecute_givenSourceGetValueReturn65535_thenCpuWithBusIsBusAfIsFortyEight() {
        // Arrange
        Source source1 = mock(Source.class);
        when(source1.getValue(Mockito.<Cpu>any())).thenReturn(65535);
        Source source2 = mock(Source.class);
        when(source2.getValue(Mockito.<Cpu>any())).thenReturn(42);
        AdditionInstruction additionInstruction = new AdditionInstruction(source1, source2, mock(Destination.class));
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = additionInstruction.execute(cpu);

        // Assert
        verify(source1).getValue(isA(Cpu.class));
        verify(source2).getValue(isA(Cpu.class));
        assertEquals(48, cpu.getAf());
        assertEquals(8, actualExecuteResult);
        assertTrue(cpu.isCarryFlag());
        assertTrue(cpu.isHalfCarryFlag());
    }

    /**
     * Test {@link AdditionInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@code true}.</li>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) CarryFlag is {@code true}.</li>
     * </ul>
     * <p>
     * Method under test: {@link AdditionInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given 'true'; when Cpu(Bus) with bus is Bus (default constructor) CarryFlag is 'true'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int AdditionInstruction.execute(Cpu)"})
    void testExecute_givenTrue_whenCpuWithBusIsBusCarryFlagIsTrue() {
        // Arrange
        Source source1 = mock(Source.class);
        when(source1.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Source source2 = mock(Source.class);
        when(source2.getValue(Mockito.<Cpu>any())).thenReturn(42);
        AdditionInstruction additionInstruction = new AdditionInstruction(source1, source2, mock(Destination.class));

        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);

        // Act
        int actualExecuteResult = additionInstruction.execute(cpu);

        // Assert
        verify(source1).getValue(isA(Cpu.class));
        verify(source2).getValue(isA(Cpu.class));
        assertEquals(8, actualExecuteResult);
        assertFalse(cpu.isCarryFlag());
        assertTrue(cpu.isHalfCarryFlag());
        assertEquals(Integer.SIZE, cpu.getAf());
    }

    /**
     * Test {@link AdditionInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred twenty-eight.</li>
     * </ul>
     * <p>
     * Method under test: {@link AdditionInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred twenty-eight")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int AdditionInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsOneHundredTwentyEight() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(0);
        AdditionInstruction additionInstruction = new AdditionInstruction(source, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = additionInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(128, cpu.getAf());
        assertFalse(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }

    /**
     * Test {@link AdditionInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@link Integer#SIZE}.</li>
     * </ul>
     * <p>
     * Method under test: {@link AdditionInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is SIZE")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int AdditionInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsSize() {
        // Arrange
        Source source1 = mock(Source.class);
        when(source1.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Source source2 = mock(Source.class);
        when(source2.getValue(Mockito.<Cpu>any())).thenReturn(42);
        AdditionInstruction additionInstruction = new AdditionInstruction(source1, source2, mock(Destination.class));
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = additionInstruction.execute(cpu);

        // Assert
        verify(source1).getValue(isA(Cpu.class));
        verify(source2).getValue(isA(Cpu.class));
        assertEquals(8, actualExecuteResult);
        assertFalse(cpu.isCarryFlag());
        assertTrue(cpu.isHalfCarryFlag());
        assertEquals(Integer.SIZE, cpu.getAf());
    }

    /**
     * Test {@link AdditionInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is zero.</li>
     * </ul>
     * <p>
     * Method under test: {@link AdditionInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is zero")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int AdditionInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsZero() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        AdditionInstruction additionInstruction = new AdditionInstruction(source, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = additionInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(0, cpu.getAf());
        assertEquals(1, actualExecuteResult);
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
    }
}
