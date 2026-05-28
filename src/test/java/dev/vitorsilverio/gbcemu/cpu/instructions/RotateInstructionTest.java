package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationA;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationB;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceA;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceB;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RotateInstructionTest {

    @Test
    void rlcaRotatesBitSevenIntoBitZero() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x80);

        new RotateInstruction(RotateDirection.LEFT, SourceA.INSTANCE, DestinationA.INSTANCE, 4, true, false, false).execute(cpu);

        assertEquals(0x01, cpu.getA() & 0xFF);
        assertTrue(cpu.isCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }

    @Test
    void rrcaRotatesBitZeroIntoBitSeven() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setA((byte) 0x01);

        new RotateInstruction(RotateDirection.RIGHT, SourceA.INSTANCE, DestinationA.INSTANCE, 4, true, false, false).execute(cpu);

        assertEquals(0x80, cpu.getA() & 0xFF);
        assertTrue(cpu.isCarryFlag());
    }

    @Test
    void rlUsesCarryAsInputAndBitSevenAsOutput() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setB((byte) 0x80);
        cpu.setCarryFlag(true);

        new RotateInstruction(RotateDirection.LEFT, SourceB.INSTANCE, DestinationB.INSTANCE, 8, false, true, true).execute(cpu);

        assertEquals(0x01, cpu.getB() & 0xFF);
        assertTrue(cpu.isCarryFlag());
        assertFalse(cpu.isZeroFlag());
    }
}
