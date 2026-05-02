package dev.vitorsilverio.gbcemu.cpu.instructions;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.instructions.sources.SourceImmediate8Bits;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JumpInstructionTest {

    @Test
    void unconditionalRelativeJumpUsesTwoByteInstructionLength() {
        Bus bus = new Bus();
        bus.addMemorySpace(new MemorySpace() {
            @Override
            public boolean contains(int address) {
                return address == 1;
            }

            @Override
            public byte read(int address) {
                return 5;
            }

            @Override
            public void write(int address, byte value) {
            }
        });
        Cpu cpu = new Cpu(bus);

        new JumpInstruction(SourceImmediate8Bits.INSTANCE, 2, 12, true).execute(cpu);

        assertEquals(7, cpu.getPc());
    }
}
