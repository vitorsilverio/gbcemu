package dev.vitorsilverio.gbcemu.cpu.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.diffblue.cover.annotations.MethodsUnderTest;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ComplementCarryInstructionDiffblueTest {
    /**
     * Test {@link ComplementCarryInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given {@code true}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is zero.</li>
     * </ul>
     * <p>
     * Method under test: {@link ComplementCarryInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given 'true'; then Cpu(Bus) with bus is Bus (default constructor) Af is zero")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int ComplementCarryInstruction.execute(Cpu)"})
    void testExecute_givenTrue_thenCpuWithBusIsBusAfIsZero() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);

        // Act
        int actualExecuteResult = ComplementCarryInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals(0, cpu.getAf());
        assertEquals(1, cpu.getPc());
        assertEquals(4, actualExecuteResult);
        assertFalse(cpu.isCarryFlag());
    }

    /**
     * Test {@link ComplementCarryInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor).</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) CarryFlag.</li>
     * </ul>
     * <p>
     * Method under test: {@link ComplementCarryInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor); then Cpu(Bus) with bus is Bus (default constructor) CarryFlag")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int ComplementCarryInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBus_thenCpuWithBusIsBusCarryFlag() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());

        // Act
        int actualExecuteResult = ComplementCarryInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals(1, cpu.getPc());
        assertEquals(4, actualExecuteResult);
        assertTrue(cpu.isCarryFlag());
        assertEquals(Short.SIZE, cpu.getAf());
    }
}
