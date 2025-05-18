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

class DecimalAdjustInstructionDiffblueTest {
    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given minus one hundred three.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) A is minus one hundred three.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given minus one hundred three; then Cpu(Bus) with bus is Bus (default constructor) A is minus one hundred three")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_givenMinusOneHundredThree_thenCpuWithBusIsBusAIsMinusOneHundredThree() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.setA((byte) -103);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert that nothing has changed
        assertEquals((byte) -103, cpu.getA());
        assertEquals(39168, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Given minus one hundred two.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@code 40960}.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); given minus one hundred two; then Cpu(Bus) with bus is Bus (default constructor) Af is '40960'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_givenMinusOneHundredTwo_thenCpuWithBusIsBusAfIs40960() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setNegativeFlag(false);
        cpu.setHalfCarryFlag(false);
        cpu.setCarryFlag(false);
        cpu.setA((byte) -102);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals((byte) -96, cpu.getA());
        assertEquals(40960, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) A is minus six.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) A is minus six")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAIsMinusSix() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setHalfCarryFlag(true);
        cpu.setNegativeFlag(true);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals((byte) -6, cpu.getA());
        assertEquals(64064, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred ninety-two.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred ninety-two")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_thenCpuWithBusIsBusAfIsOneHundredNinetyTwo() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setNegativeFlag(true);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals((byte) 0, cpu.getA());
        assertEquals(192, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) CarryFlag is {@code true}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@code 24576}.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor) CarryFlag is 'true'; then Cpu(Bus) with bus is Bus (default constructor) Af is '24576'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBusCarryFlagIsTrue_thenCpuWithBusIsBusAfIs24576() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals(24576, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
        assertEquals('`', cpu.getA());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) CarryFlag is {@code true}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@code 41024}.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor) CarryFlag is 'true'; then Cpu(Bus) with bus is Bus (default constructor) Af is '41024'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBusCarryFlagIsTrue_thenCpuWithBusIsBusAfIs41024() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setCarryFlag(true);
        cpu.setNegativeFlag(true);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals((byte) -96, cpu.getA());
        assertEquals(41024, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) HalfCarryFlag is {@code true}.</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is {@code 1536}.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor) HalfCarryFlag is 'true'; then Cpu(Bus) with bus is Bus (default constructor) Af is '1536'")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBusHalfCarryFlagIsTrue_thenCpuWithBusIsBusAfIs1536() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());
        cpu.setHalfCarryFlag(true);

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals(1536, cpu.getAf());
        assertEquals((byte) 6, cpu.getA());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    /**
     * Test {@link DecimalAdjustInstruction#execute(Cpu)}.
     * <ul>
     *   <li>When {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor).</li>
     *   <li>Then {@link Cpu#Cpu(Bus)} with bus is {@link Bus} (default constructor) Af is one hundred twenty-eight.</li>
     * </ul>
     * <p>
     * Method under test: {@link DecimalAdjustInstruction#execute(Cpu)}
     */
    @Test
    @DisplayName("Test execute(Cpu); when Cpu(Bus) with bus is Bus (default constructor); then Cpu(Bus) with bus is Bus (default constructor) Af is one hundred twenty-eight")
    @Tag("MaintainedByDiffblue")
    @MethodsUnderTest({"int DecimalAdjustInstruction.execute(Cpu)"})
    void testExecute_whenCpuWithBusIsBus_thenCpuWithBusIsBusAfIsOneHundredTwentyEight() {
        // Arrange
        Cpu cpu = new Cpu(new Bus());

        // Act
        DecimalAdjustInstruction.INSTANCE.execute(cpu);

        // Assert
        assertEquals((byte) 0, cpu.getA());
        assertEquals(128, cpu.getAf());
        assertFalse(cpu.isCarryFlag());
        assertFalse(cpu.isHalfCarryFlag());
        assertTrue(cpu.isZeroFlag());
    }
}
