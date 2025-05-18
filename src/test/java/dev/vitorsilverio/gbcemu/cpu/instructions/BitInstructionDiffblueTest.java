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

class BitInstructionDiffblueTest {
    /**
     * Test {@link BitInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred sixty.</li>
     * </ul>
     * <p>
     * Method under test: {@link BitInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred sixty")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int BitInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsOneHundredSixty() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        BitInstruction bitInstruction = new BitInstruction(2, source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = bitInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
        assertEquals(160, cpu.getAf());
        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }

    /**
     * Test {@link BitInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then not {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) ZeroFlag.</li>
     * </ul>
     * <p>
     * Method under test: {@link BitInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then not Cpu(Bus) with bus is Bus (default constructor) ZeroFlag")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int BitInstruction.execute(Cpu)"})
    void testExecute_thenNotCpuWithBusIsBusZeroFlag() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        BitInstruction bitInstruction = new BitInstruction(1, source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = bitInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
        assertFalse(cpu.isZeroFlag());
        assertTrue(cpu.isHalfCarryFlag());
        assertEquals(Integer.SIZE, cpu.getAf());
    }
}
