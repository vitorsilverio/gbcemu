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

class IncrementInstructionDiffblueTest {
    /**
     * Test {@link IncrementInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link IncrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int IncrementInstruction.execute(Cpu)"})
    void testExecute() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        IncrementInstruction incrementInstruction = new IncrementInstruction(source, destination, 1, false);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = incrementInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(43));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(0, cpu.getAf());
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link IncrementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link Source} {@link Source#getValue(Cpu)} return {@code 65535}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@link Integer#SIZE}.</li>
     * </ul>
     * <p>
     * Method under test: {@link IncrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given Source getValue(Cpu) return '65535'; then Cpu(Bus) with bus is Bus (default constructor) Af is SIZE")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int IncrementInstruction.execute(Cpu)"})
    void testExecute_givenSourceGetValueReturn65535_thenCpuWithBusIsBusAfIsSize() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(65535);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        IncrementInstruction incrementInstruction = new IncrementInstruction(source, destination, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = incrementInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(65536));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
        assertFalse(cpu.isZeroFlag());
        assertTrue(cpu.isHalfCarryFlag());
        assertEquals(Integer.SIZE, cpu.getAf());
    }

    /**
     * Test {@link IncrementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link Source} {@link Source#getValue(Cpu)} return forty-two.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is zero.</li>
     * </ul>
     * <p>
     * Method under test: {@link IncrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given Source getValue(Cpu) return forty-two; then Cpu(Bus) with bus is Bus (default constructor) Af is zero")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int IncrementInstruction.execute(Cpu)"})
    void testExecute_givenSourceGetValueReturnFortyTwo_thenCpuWithBusIsBusAfIsZero() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        IncrementInstruction incrementInstruction = new IncrementInstruction(source, destination, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = incrementInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(43));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(0, cpu.getAf());
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link IncrementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred sixty.</li>
     * </ul>
     * <p>
     * Method under test: {@link IncrementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred sixty")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int IncrementInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsOneHundredSixty() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(255);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        IncrementInstruction incrementInstruction = new IncrementInstruction(source, destination, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = incrementInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(256));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
        assertEquals(160, cpu.getAf());
        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }
}
