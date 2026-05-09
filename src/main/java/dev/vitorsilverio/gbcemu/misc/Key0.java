package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>This GBC-only register (which is not officially documented) is written only by the CGB boot ROM, as it gets locked after the bootrom finish execution (by a write to the BANK register).</p>
 * <p>Once it is locked, the behavior of the system can’t be changed without a reset (this behavior can be observed using this test ROM).</p>
 * <p>As a result of the above most of the behavior is not directly testable without hardware manipulation. Even though we can’t test its behavior directly we can inspect the disassembly of the CGB bootrom and infer the following:</p>
 */

public class Key0 implements MemorySpace, Snapshottable, Stateful<Key0State> {

    @Savable private byte key0 = 0;
    private final Consumer<Boolean> onCgbModeChange;

    public Key0() {
        this(cgbMode -> {
        });
    }

    public Key0(Consumer<Boolean> onCgbModeChange) {
        this.onCgbModeChange = onCgbModeChange;
    }

    @Override
    public Key0State saveState() {
        return new Key0State(key0);
    }

    @Override
    public void loadState(Key0State state) {
        key0 = state.key0();
        onCgbModeChange.accept((key0 & 0x04) == 0);
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
        if (state instanceof Key0State key0State) {
            loadState(key0State);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
        onCgbModeChange.accept((key0 & 0x04) == 0);
    }

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
        onCgbModeChange.accept((value & 0x04) == 0);
    }
}
