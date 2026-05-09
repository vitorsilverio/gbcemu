package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class ZeroPage implements MemorySpace, Stateful<ZeroPageState> {

    private final byte[] memory = new byte[0x7F];

    @Override
    public ZeroPageState saveState() {
        return new ZeroPageState(memory.clone());
    }

    @Override
    public void loadState(ZeroPageState state) {
        System.arraycopy(state.memory(), 0, memory, 0, Math.min(memory.length, state.memory().length));
    }

    @Override
    public boolean contains(int address) {
        return 0xff80 <= address && address <= 0xfffe;
    }

    @Override
    public byte read(int address) {
        return memory[address - 0xff80];
    }

    @Override
    public void write(int address, byte value) {
        memory[address - 0xff80] = value;
    }
}
