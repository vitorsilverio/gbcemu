package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
