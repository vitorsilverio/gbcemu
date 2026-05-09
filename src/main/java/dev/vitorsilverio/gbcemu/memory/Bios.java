package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Bios implements MemorySpace, Snapshottable, Stateful<BiosState> {

    @Savable private boolean enabled = true;

    private final byte[] bios;

    public Bios(File biosFile) {
        try(var inputStream = biosFile.toURI().toURL().openStream()) {
            this.bios = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BIOS", e);
        }
    }

    @Override
    public BiosState saveState() {
        return new BiosState(enabled);
    }

    @Override
    public void loadState(BiosState state) {
        enabled = state.enabled();
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
        if (state instanceof BiosState biosState) {
            loadState(biosState);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
    }

    @Override
    public boolean contains(int address) {
        if (address == 0xFF50) {
            return true;
        }
        if (address < 0x900 && !(address >= 0x100 && address < 0x200)) {
            return enabled;
        }
        return false;
    }

    @Override
    public byte read(int address) {
        if (address == 0xFF50) {
            return (byte) (enabled ? 0 : 1);
        }
        return bios[address];
    }

    @Override
    public void write(int address, byte value) {
        if (address == 0xFF50) {
            enabled = false;
        }
    }
}
