package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.misc.Key1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StopInstructionTest {

    @Test
    void advancesPastStopWithoutFreezingCpu() {
        Cpu cpu = new Cpu(new Bus());
        cpu.setPc(0xC34C);

        int cycles = StopInstruction.INSTANCE.execute(cpu);

        assertEquals(4, cycles);
        assertEquals(0xC34E, cpu.getPc());
        assertFalse(cpu.isStopped());
    }

    @Test
    void switchesSpeedWhenKey1IsPrepared() {
        Bus bus = new Bus();
        Key1 key1 = new Key1();
        bus.addMemorySpace(key1);
        Cpu cpu = new Cpu(bus);
        cpu.setPc(0x0100);
        key1.write(0xFF4D, (byte) 0x01);

        StopInstruction.INSTANCE.execute(cpu);

        assertEquals(0x0102, cpu.getPc());
        assertEquals(2, cpu.getSpeedRate());
        assertEquals(0xFE, key1.read(0xFF4D) & 0xFF);
        assertFalse(cpu.isStopped());
    }

    @Test
    void secondPreparedStopReturnsToNormalSpeed() {
        Bus bus = new Bus();
        Key1 key1 = new Key1();
        bus.addMemorySpace(key1);
        Cpu cpu = new Cpu(bus);

        key1.write(0xFF4D, (byte) 0x01);
        StopInstruction.INSTANCE.execute(cpu);
        key1.write(0xFF4D, (byte) 0x01);
        StopInstruction.INSTANCE.execute(cpu);

        assertEquals(1, cpu.getSpeedRate());
        assertEquals(0x7E, key1.read(0xFF4D) & 0xFF);
    }
}
