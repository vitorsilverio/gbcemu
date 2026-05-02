package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationHL;
import dev.vitorsilverio.gbcemu.cpu.instructions.destinations.DestinationSP;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddSignedImmediateToStackPointerInstructionTest {

    @Test
    void addSpUsesSignedImmediate() {
        Cpu cpu = cpuWithImmediate(0xFE);
        cpu.setSp(0x1000);

        int cycles = new AddSignedImmediateToStackPointerInstruction(DestinationSP.INSTANCE, 16).execute(cpu);

        assertEquals(0x0FFE, cpu.getSp());
        assertEquals(2, cpu.getPc());
        assertEquals(16, cycles);
        assertFalse(cpu.isZeroFlag());
        assertFalse(cpu.isNegativeFlag());
    }

    @Test
    void ldHlSpPlusImmediateDoesNotIncludeCarryFlag() {
        Cpu cpu = cpuWithImmediate(0x01);
        cpu.setSp(0x1000);
        cpu.setCarryFlag(true);

        new AddSignedImmediateToStackPointerInstruction(DestinationHL.INSTANCE, 12).execute(cpu);

        assertEquals(0x1001, cpu.getHl());
        assertFalse(cpu.isCarryFlag());
    }

    @Test
    void flagsUseLowByteUnsignedAddition() {
        Cpu cpu = cpuWithImmediate(0x01);
        cpu.setSp(0x00FF);

        new AddSignedImmediateToStackPointerInstruction(DestinationSP.INSTANCE, 16).execute(cpu);

        assertEquals(0x0100, cpu.getSp());
        assertTrue(cpu.isHalfCarryFlag());
        assertTrue(cpu.isCarryFlag());
    }

    private Cpu cpuWithImmediate(int immediate) {
        Bus bus = new Bus();
        bus.addMemorySpace(new MemorySpace() {
            @Override
            public boolean contains(int address) {
                return address == 1;
            }

            @Override
            public byte read(int address) {
                return (byte) immediate;
            }

            @Override
            public void write(int address, byte value) {
            }
        });
        return new Cpu(bus);
    }
}
