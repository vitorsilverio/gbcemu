package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.WorkRam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResetInstructionTest {

    @Test
    void pushesAddressAfterRstOpcode() {
        Bus bus = new Bus();
        bus.addMemorySpace(new WorkRam());
        Cpu cpu = new Cpu(bus);
        cpu.setPc(0x1234);
        cpu.setSp(0xD010);

        new ResetInstruction(0x00).execute(cpu);

        assertEquals(0x0000, cpu.getPc());
        assertEquals(0xD00E, cpu.getSp());
        assertEquals(0x1235, cpu.popStack());
    }
}
