package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.diffblue.cover.annotations.MethodsUnderTest;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DisableInterruptsInstructionDiffblueTest {
    /**
     * Test {@link DisableInterruptsInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor).</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Pc is one.</li>
     * </ul>
     * <p>
     * Method under test: {@link DisableInterruptsInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor); then Cpu(Bus) with bus is Bus (default constructor) Pc is one")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DisableInterruptsInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBus_thenCpuWithBusIsBusPcIsOne() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = DisableInterruptsInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals(1, cpu.getPc());
        assertEquals(4, actualExecuteResult);
    }
}
