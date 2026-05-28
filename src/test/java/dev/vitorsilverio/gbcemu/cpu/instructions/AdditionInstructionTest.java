package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdditionInstructionTest {

    @Test
    void addWritesTheResultBackToA() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x19);

        new AdditionInstruction(cpu1 -> 0x66, 4, false).execute(cpu);

        assertEquals(0x7F, cpu.getA() & 0xFF);
    }

    @Test
    void addDoesNotIncludeCarryUnlessInstructionIsAdc() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x01);
        cpu.setCarryFlag(true);

        new AdditionInstruction(cpu1 -> 0x01, 4, false).execute(cpu);

        assertEquals(0x02, cpu.getA() & 0xFF);
        assertFalse(cpu.isCarryFlag());
    }

    @Test
    void adcIncludesCarryInHalfCarryCalculation() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x0F);
        cpu.setCarryFlag(true);

        new AdditionInstruction(cpu1 -> 0x00, 4, true).execute(cpu);

        assertEquals(0x10, cpu.getA() & 0xFF);
        assertTrue(cpu.isHalfCarryFlag());
        assertFalse(cpu.isCarryFlag());
    }

    @Test
    void immediateAddConsumesOpcodeAndOperand() {
        Cpu cpu = new Cpu(new Bus());

        new AdditionInstruction(cpu1 -> 0x10, 8, false, 2).execute(cpu);

        assertEquals(2, cpu.getPc());
    }

    @Test
    void sixteenBitAddUsesBitElevenForHalfCarry() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setHl(0x0FFF);
        cpu.setBc(0x0001);

        new AdditionInstruction(cpu1 -> cpu1.getBc(), cpu1 -> cpu1.getHl(), (cpu1, value) -> cpu1.setHl(value)).execute(cpu);

        assertEquals(0x1000, cpu.getHl());
        assertEquals(1, cpu.getPc());
        assertTrue(cpu.isHalfCarryFlag());
        assertFalse(cpu.isCarryFlag());
    }
}
