package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.HashMap;
import java.util.Map;

public class DMA implements MemorySpace, MachineCycle, Snapshottable, Stateful<DmaState> {

    private static final int DMA_REQUEST_REGISTER = 0xff46;

    @Savable private int cycles = 0;
    @Savable private int baseAddress = 0;
    @Savable private boolean active;
    private final Bus bus;

    public DMA(Bus bus) {
        this.bus = bus;
    }

    @Override
    public DmaState saveState() {
        return new DmaState(cycles, baseAddress, active);
    }

    @Override
    public void loadState(DmaState state) {
        cycles = Math.max(0, state.cycles());
        baseAddress = state.baseAddress() & 0xFF;
        active = state.active();
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
        if (state instanceof DmaState dmaState) {
            loadState(dmaState);
            return;
        }
        Snapshottable.super.restoreSnapshot(snapshot);
    }

    @Override
    public void tick() {
        int offset = 0xa0 - cycles;
        cycles--;
        var value = bus.read((baseAddress << 8) | offset);
        bus.write(0xfe00 | offset, value);
        if (cycles == 0) {
            active = false;
        }
    }

    @Override
    public boolean contains(int address) {
        return address == DMA_REQUEST_REGISTER;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        baseAddress = value & 0xFF;
        active = true;
        cycles = 160;
    }

    public boolean isActive() {
        return active;
    }
}
