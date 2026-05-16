package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class CgbUndocumentedRegisters implements MemorySpace, Stateful<CgbUndocumentedRegistersState> {

    private static final int FF72 = 0xFF72;
    private static final int FF73 = 0xFF73;
    private static final int FF74 = 0xFF74;
    private static final int FF75 = 0xFF75;

    private byte ff72;
    private byte ff73;
    private byte ff74;
    private byte ff75;

    @Override
    public boolean contains(int address) {
        return address >= FF72 && address <= FF75;
    }

    @Override
    public byte read(int address) {
        return switch (address) {
            case FF72 -> ff72;
            case FF73 -> ff73;
            case FF74 -> ff74;
            case FF75 -> (byte) (0x8F | (ff75 & 0x70));
            default -> (byte) 0xFF;
        };
    }

    @Override
    public void write(int address, byte value) {
        switch (address) {
            case FF72 -> ff72 = value;
            case FF73 -> ff73 = value;
            case FF74 -> ff74 = value;
            case FF75 -> ff75 = (byte) (value & 0x70);
            default -> {
            }
        }
    }

    @Override
    public CgbUndocumentedRegistersState saveState() {
        return new CgbUndocumentedRegistersState(ff72, ff73, ff74, ff75);
    }

    @Override
    public void loadState(CgbUndocumentedRegistersState state) {
        ff72 = state.ff72();
        ff73 = state.ff73();
        ff74 = state.ff74();
        ff75 = (byte) (state.ff75() & 0x70);
    }
}
