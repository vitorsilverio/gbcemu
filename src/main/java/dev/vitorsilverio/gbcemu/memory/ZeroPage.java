package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.HashMap;
import java.util.Map;

public class ZeroPage implements MemorySpace, Snapshottable, Stateful<ZeroPageState> {

    @Savable private final byte[] memory = new byte[0x7F];

    @Override
    public ZeroPageState saveState() {
        return new ZeroPageState(memory.clone());
    }

    @Override
    public void loadState(ZeroPageState state) {
        System.arraycopy(state.memory(), 0, memory, 0, Math.min(memory.length, state.memory().length));
    }

    @Override
    public Snapshot createSnapshot(int version) {
        Map<String, Object> state = new HashMap<>();
        state.put("state", saveState());
        return new Snapshot(getClass().getName(), version, state);
    }

    @Override
    public void restoreSnapshot(Snapshot snapshot) {
        Object state = snapshot.state().get("state");
        if (state instanceof ZeroPageState zeroPageState) {
            loadState(zeroPageState);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
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
