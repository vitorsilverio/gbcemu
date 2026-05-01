package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationHighReference8bits;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceImmediate8Bits;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UnsignedByteSourceTest {

    @Test
    void compareImmediateTreatsByteAsUnsigned() {
        Bus bus = new Bus();
        bus.addMemorySpace(new MemorySpace() {
            @Override
            public boolean contains(int address) {
                return address == 1;
            }

            @Override
            public byte read(int address) {
                return (byte) 0x90;
            }

            @Override
            public void write(int address, byte value) {
            }
        });
        Cpu cpu = new Cpu(bus);
        cpu.setA((byte) 0x90);

        new CompareInstruction(SourceImmediate8Bits.INSTANCE, 2, 8).execute(cpu);

        assertTrue(cpu.isZeroFlag());
    }

    @Test
    void highImmediateAddressTreatsOffsetAsUnsigned() {
        RecordingBus bus = new RecordingBus();
        Cpu cpu = new Cpu(bus);

        DestinationHighReference8bits.INSTANCE.setValue(cpu, 0x12);

        assertEquals(0xFF90, bus.writtenAddress);
        assertEquals((byte) 0x12, bus.writtenValue);
    }

    private static class RecordingBus extends Bus {
        private int writtenAddress;
        private byte writtenValue;

        @Override
        public byte read(int address) {
            return (byte) 0x90;
        }

        @Override
        public void write(int address, byte value) {
            writtenAddress = address;
            writtenValue = value;
        }
    }
}
