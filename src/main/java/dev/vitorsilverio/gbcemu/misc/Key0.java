package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;

/**
 * <p>This GBC-only register (which is not officially documented) is written only by the CGB boot ROM, as it gets locked after the bootrom finish execution (by a write to the BANK register).</p>
 * <p>Once it is locked, the behavior of the system can’t be changed without a reset (this behavior can be observed using this test ROM).</p>
 * <p>As a result of the above most of the behavior is not directly testable without hardware manipulation. Even though we can’t test its behavior directly we can inspect the disassembly of the CGB bootrom and infer the following:</p>
 */

public class Key0 implements MemorySpace {

    private byte key0 = 0;

    @Override
    public boolean contains(int address) {
        return address == 0xFF4C;
    }

    @Override
    public byte read(int address) {
        return key0;
    }

    @Override
    public void write(int address, byte value) {
        key0 = value;
    }
}
