package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import java.util.List;

public interface DebugMemoryInterface {
    byte read(int address);

    void writeHardware(int address, byte value);

    List<MemoryMapEntry> memoryMap();

    List<MemoryBank> memoryBanks();

    default boolean writeRawBank(String bankName, int bankIndex, int offset, byte value) {
        for (MemoryBank bank : memoryBanks()) {
            if (!bank.bankName().equals(bankName)
                    || bank.bankCount() <= 0
                    || bank.bankSize() <= 0
                    || bankIndex < 0
                    || bankIndex >= bank.bankCount()) {
                continue;
            }
            bank.writeBank(bankIndex, Math.floorMod(offset, bank.bankSize()), value);
            return true;
        }
        return false;
    }

    record MemoryMapEntry(int start, int end, String owner) {
    }
}
