package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.HashMap;
import java.util.Map;

public class InfraredPort implements MemorySpace, Snapshottable, Stateful<InfraredState> {

    private static final int RP_REGISTER = 0xFF56;

    @Savable private byte data = 0x02;

    @Override
    public InfraredState saveState() {
        return new InfraredState(data);
    }

    @Override
    public void loadState(InfraredState state) {
        data = state.data();
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
        if (state instanceof InfraredState infraredState) {
            loadState(infraredState);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
    }

    @Override
    public boolean contains(int address) {
        return address == RP_REGISTER;
    }

    @Override
    public byte read(int address) {
        return data;
    }

    @Override
    public void write(int address, byte value) {
        data = value;
    }
}
