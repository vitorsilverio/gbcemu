package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationA;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationReference16bits;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceReference16bits;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceSP;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Reference16BitsInstructionTest {

    @Test
    void loadAFromA16ReadsSingleByteFromImmediateAddress() {
        TestMemory memory = new TestMemory();
        memory.write(1, (byte) 0x34);
        memory.write(2, (byte) 0x12);
        memory.write(0x1234, (byte) 0xAB);
        memory.write(0x1235, (byte) 0xCD);
        Cpu cpu = cpu(memory);

        new LoadInstruction(SourceReference16bits.INSTANCE, DestinationA.INSTANCE, 3, 16).execute(cpu);

        assertEquals(0xAB, cpu.getA() & 0xFF);
    }

    @Test
    void loadA16FromAWritesToImmediateAddress() {
        TestMemory memory = new TestMemory();
        memory.write(1, (byte) 0x34);
        memory.write(2, (byte) 0x12);
        Cpu cpu = cpu(memory);
        cpu.setA((byte) 0xAB);

        new LoadInstruction(cpu1 -> cpu1.getA() & 0xFF, DestinationReference16bits.INSTANCE, 3, 16).execute(cpu);

        assertEquals(0xAB, memory.read(0x1234) & 0xFF);
    }

    @Test
    void loadA16FromSpWritesWordToImmediateAddress() {
        TestMemory memory = new TestMemory();
        memory.write(1, (byte) 0x34);
        memory.write(2, (byte) 0x12);
        Cpu cpu = cpu(memory);
        cpu.setSp(0xFED2);

        new LoadInstruction(SourceSP.INSTANCE, DestinationReference16bits.INSTANCE_WORD, 3, 20).execute(cpu);

        assertEquals(0xD2, memory.read(0x1234) & 0xFF);
        assertEquals(0xFE, memory.read(0x1235) & 0xFF);
    }

    private Cpu cpu(TestMemory memory) {
        Bus bus = new Bus();
        bus.addMemorySpace(memory);
        return new Cpu(bus);
    }

    private static class TestMemory implements MemorySpace {
        private final Map<Integer, Byte> bytes = new HashMap<>();

        @Override
        public boolean contains(int address) {
            return true;
        }

        @Override
        public byte read(int address) {
            return bytes.getOrDefault(address & 0xFFFF, (byte) 0);
        }

        @Override
        public void write(int address, byte value) {
            bytes.put(address & 0xFFFF, value);
        }
    }
}
