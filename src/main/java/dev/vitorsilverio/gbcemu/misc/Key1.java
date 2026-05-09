package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.HashMap;
import java.util.Map;

public class Key1 implements MemorySpace, Snapshottable, Stateful<Key1State> {

    private static final int KEY1_REGISTER = 0xFF4D;

    @Savable private boolean prepareSpeedSwitch;
    @Savable private boolean doubleSpeed;

    @Override
    public Key1State saveState() {
        return new Key1State(prepareSpeedSwitch, doubleSpeed);
    }

    @Override
    public void loadState(Key1State state) {
        prepareSpeedSwitch = state.prepareSpeedSwitch();
        doubleSpeed = state.doubleSpeed();
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
        if (state instanceof Key1State key1State) {
            loadState(key1State);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
    }

    @Override
    public boolean contains(int address) {
        return address == KEY1_REGISTER;
    }

    @Override
    public byte read(int address) {
        return (byte) (0x7E | (doubleSpeed ? 0x80 : 0) | (prepareSpeedSwitch ? 0x01 : 0));
    }

    @Override
    public void write(int address, byte value) {
        prepareSpeedSwitch = (value & 0x01) != 0;
    }

    public boolean isPrepareSpeedSwitch() {
        return prepareSpeedSwitch;
    }

    public boolean isDoubleSpeed() {
        return doubleSpeed;
    }

    public boolean switchSpeedIfPrepared() {
        if (!prepareSpeedSwitch) {
            return false;
        }
        doubleSpeed = !doubleSpeed;
        prepareSpeedSwitch = false;
        return true;
    }
}
