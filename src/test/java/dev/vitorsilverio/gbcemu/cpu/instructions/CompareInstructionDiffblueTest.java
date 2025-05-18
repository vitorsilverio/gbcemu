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

class CompareInstructionDiffblueTest {
    /**
     * Test {@link CompareInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is ninety-six.</li>
     * </ul>
     * <p>
     * Method under test: {@link CompareInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is ninety-six")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CompareInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsNinetySix() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(-42);
        CompareInstruction compareInstruction = new CompareInstruction(source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        compareInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(96, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isZeroFlag());
        assertTrue(cpu.isHalfCarryFlag());
    }

    /**
     * Test {@link CompareInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred ninety-two.</li>
     * </ul>
     * <p>
     * Method under test: {@link CompareInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred ninety-two")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CompareInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsOneHundredNinetyTwo() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(0);
        CompareInstruction compareInstruction = new CompareInstruction(source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        compareInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(192, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }

    /**
     * Test {@link CompareInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred twelve.</li>
     * </ul>
     * <p>
     * Method under test: {@link CompareInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred twelve")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CompareInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsOneHundredTwelve() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        CompareInstruction compareInstruction = new CompareInstruction(source, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        compareInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(112, cpu.getAf());
        assertFalse(cpu.isZeroFlag());
        assertTrue(cpu.isCarryFlag());
        assertTrue(cpu.isHalfCarryFlag());
    }
}
