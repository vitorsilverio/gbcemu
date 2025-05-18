package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.diffblue.cover.annotations.MethodsUnderTest;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ComplementInstructionDiffblueTest {
    /**
     * Test {@link ComplementInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor).</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) A is minus one.</li>
     * </ul>
     * <p>
     * Method under test: {@link ComplementInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor); then Cpu(Bus) with bus is Bus (default constructor) A is minus one")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int ComplementInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBus_thenCpuWithBusIsBusAIsMinusOne() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = ComplementInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals((byte) -1, cpu.getA());
        assertEquals(1, cpu.getPc());
        assertEquals(4, actualExecuteResult);
        assertEquals(65376, cpu.getAf());
        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isNegativeFlag());
    }
}
