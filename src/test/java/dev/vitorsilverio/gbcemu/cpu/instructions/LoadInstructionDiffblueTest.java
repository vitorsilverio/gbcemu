package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class LoadInstructionDiffblueTest {
    /**
     * Test {@link LoadInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor).</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Pc is one.</li>
     * </ul>
     * <p>
     * Method under test: {@link LoadInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor); then Cpu(Bus) with bus is Bus (default constructor) Pc is one")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int LoadInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBus_thenCpuWithBusIsBusPcIsOne() {
        // Arrange
        Source source = mock(Source.class);
        when(source.getValue(Mockito.<Cpu>any())).thenReturn(42);
        Destination destination = mock(Destination.class);
        doNothing().when(destination).setValue(Mockito.<Cpu>any(), anyInt());
        LoadInstruction loadInstruction = new LoadInstruction(source, destination, 1, 1);
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = loadInstruction.execute(cpu);

        // Assert
        verify(destination).setValue(isA(Cpu.class), eq(42));
        verify(source).getValue(isA(Cpu.class));
        assertEquals(1, cpu.getPc());
        assertEquals(1, actualExecuteResult);
    }
}
