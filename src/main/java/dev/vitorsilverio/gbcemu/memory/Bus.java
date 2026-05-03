package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.interrupt.InterruptManager;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bus {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Bus.class);

    private final List<MemorySpace> memorySpaces;
    private final InterruptManager interruptManager;

    public Bus() {
        this.memorySpaces = new ArrayList<>();
        this.interruptManager = new InterruptManager();
        this.memorySpaces.add(interruptManager);
    }

    public void addMemorySpace(MemorySpace memorySpace) {
        memorySpaces.add(memorySpace);

    }

    public List<MemoryMapEntry> memoryMap() {
        List<MemoryMapEntry> entries = new ArrayList<>();
        String currentOwner = null;
        int rangeStart = 0;
        for (int address = 0; address <= 0xFFFF; address++) {
            String owner = ownerName(address);
            if (address == 0) {
                currentOwner = owner;
                continue;
            }
            if (!owner.equals(currentOwner)) {
                entries.add(new MemoryMapEntry(rangeStart, address - 1, currentOwner));
                rangeStart = address;
                currentOwner = owner;
            }
        }
        entries.add(new MemoryMapEntry(rangeStart, 0xFFFF, currentOwner));
        return entries;
    }

    private String ownerName(int address) {
        for (MemorySpace memorySpace : memorySpaces) {
            if (memorySpace.contains(address)) {
                return memorySpace.getClass().getSimpleName();
            }
        }
        return "Unmapped";
    }

    public <T extends MemorySpace> Optional<T> findMemorySpace(Class<T> type) {
        return memorySpaces.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    public byte read(int address) {
        address = address & 0xFFFF; // Ensure address is within 16-bit range
        for (MemorySpace memorySpace : memorySpaces) {
            if (memorySpace.contains(address)) {
                try {
                    return memorySpace.read(address);
                } catch (Exception e) {
                    logger.error(String.format("""
                                Error reading from address %s
                                Memory space: %s
                                Contains?: %s
                            """, Integer.toHexString(address), memorySpace.getClass().getName(), memorySpace.contains(address)), e);
                    throw new RuntimeException("Failed to read from address " + Integer.toHexString(address), e);
                }
            }
        }
        logger.warn("Address " + Integer.toHexString(address) + " not found in any memory space");
        return 0;
    }

    public void write(int address, byte value) {
        address = address & 0xFFFF;
        for (MemorySpace memorySpace : memorySpaces) {
            if (memorySpace.contains(address)) {
                memorySpace.write(address, value);
                return;
            }
        }
        logger.warn("Address " + Integer.toHexString(address) + " not found in any memory space");
    }

    public void requestInterrupt(Interrupt interrupt) {
        interruptManager.requestInterrupt(interrupt);
    }

    public void clearInterrupt(Interrupt interrupt) {
        interruptManager.clearInterrupt(interrupt);
    }

    public Optional<Interrupt> getPendingInterrupt() {
        return interruptManager.getPendingInterrupt();
    }

    public int readWord(int address) {
        int lowByte = read(address);
        int highByte = read(address + 1);
        return ((highByte << 8) | (lowByte & 0xFF)) & 0xFFFF; // Ensure value is within 16-bit range
    }

    public void writeWord(int address, int value) {
        value = value & 0xFFFF; // Ensure value is within 16-bit range
        write(address, (byte) (value & 0xFF));
        write(address + 1, (byte) ((value >> 8) & 0xFF));
    }

    public record MemoryMapEntry(int start, int end, String owner) {
    }
}
