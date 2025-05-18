package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
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

class JumpInstructionDiffblueTest {
    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(source, 1, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(43, cpu.getPc());
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute2() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.CARRY, source, 1, 1, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute3() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.NOT_CARRY, source, 1, 1, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(43, cpu.getPc());
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute4() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.ZERO, source, 1, 1, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute5() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.NOT_ZERO, source, 1, 1, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(43, cpu.getPc());
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute6() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.NOT_CARRY, source, 1, 1, 1, true);

        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute7() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.ZERO, source, 1, 1, 1, true);

        Cpu cpu = new Cpu(new Bus());
        cpu.setZeroFlag(true);

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(43, cpu.getPc());
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute8() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.NOT_ZERO, source, 1, 1, 1, true);

        Cpu cpu = new Cpu(new Bus());
        cpu.setZeroFlag(true);

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@link Source} {@link Source#getValue(Cpu)} return {@code 65535}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Pc is {@code 65280}.</li>
     * </ul>
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given Source getValue(Cpu) return '65535'; then Cpu(Bus) with bus is Bus (default constructor) Pc is '65280'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute_givenSourceGetValueReturn65535_thenCpuWithBusIsBusPcIs65280() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(65535);
        JumpInstruction jumpInstruction = new JumpInstruction(source, 1, 1, true);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(65280, cpu.getPc());
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Pc is forty-two.</li>
     * </ul>
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Pc is forty-two")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusPcIsFortyTwo() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(source, 1, 1, false);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(42, cpu.getPc());
    }

    /**
     * Test {@link JumpInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) CarryFlag is {@code true}.</li>
     * </ul>
     * <p>
     * Method under test: {@link JumpInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor) CarryFlag is 'true'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int JumpInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBusCarryFlagIsTrue() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        JumpInstruction jumpInstruction = new JumpInstruction(Condition.CARRY, source, 1, 1, 1, true);

        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);

        // Act
        int actualExecuteResult = jumpInstruction.execute(cpu);

        // Assert
        verify(source, atLeast(1)).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(43, cpu.getPc());
    }
}
