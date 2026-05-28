package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import java.util.ArrayList;
import java.util.List;

public class BusDebugMemoryInterface implements DebugMemoryInterface {
    private final Bus bus;

    public BusDebugMemoryInterface(Bus bus) {
        this.bus = bus;
    }

    @Override
    public byte read(int address) {
        return bus.read(address);
    }

    @Override
    public void writeHardware(int address, byte value) {
        bus.write(address, value);
    }

    @Override
    public List<MemoryMapEntry> memoryMap() {
        List<MemoryMapEntry> entries = new ArrayList<>();
        for (Bus.MemoryMapEntry entry : bus.memoryMap()) {
            entries.add(new MemoryMapEntry(entry.start(), entry.end(), entry.owner()));
        }
        return entries;
    }

    @Override
    public List<MemoryBank> memoryBanks() {
        return bus.memoryBanks();
    }
}
