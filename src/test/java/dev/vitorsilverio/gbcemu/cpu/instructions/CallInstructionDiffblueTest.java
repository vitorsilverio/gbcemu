package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class CallInstructionDiffblueTest {
    /**
     * Test {@link CallInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link CallInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CallInstruction.execute(Cpu)"})
    void testExecute() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        CallInstruction callInstruction = new CallInstruction(Condition.CARRY, source, 1, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = callInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(3, cpu.getPc());
    }

    /**
     * Test {@link CallInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link CallInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CallInstruction.execute(Cpu)"})
    void testExecute2() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        CallInstruction callInstruction = new CallInstruction(Condition.ZERO, source, 1, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = callInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(3, cpu.getPc());
    }

    /**
     * Test {@link CallInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link CallInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CallInstruction.execute(Cpu)"})
    void testExecute3() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        CallInstruction callInstruction = new CallInstruction(Condition.NOT_CARRY, source, 1, 1);

        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);

        // Act
        int actualExecuteResult = callInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(3, cpu.getPc());
    }

    /**
     * Test {@link CallInstruction#execute(Cpu)}.
     * <p>
     * Method under test: {@link CallInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu)")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int CallInstruction.execute(Cpu)"})
    void testExecute4() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        CallInstruction callInstruction = new CallInstruction(Condition.NOT_ZERO, source, 1, 1);

        Cpu cpu = new Cpu(new Bus());
        cpu.setZeroFlag(true);

        // Act
        int actualExecuteResult = callInstruction.execute(cpu);

        // Assert
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, actualExecuteResult);
        assertEquals(3, cpu.getPc());
    }
}
