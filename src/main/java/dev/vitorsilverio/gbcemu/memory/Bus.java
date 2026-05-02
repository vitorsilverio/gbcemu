package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.interrupt.InterruptManager;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bus {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Bus.class);

    private final List<MemorySpace> memorySpaces;
    private final InterruptManager interruptManager;
    private final List<DeferredWrite> deferredWrites = new ArrayList<>();
    private boolean deferringCpuWrites;

    public Bus() {
        this.memorySpaces = new ArrayList<>();
        this.interruptManager = new InterruptManager();
        this.memorySpaces.add(interruptManager);
    }

    public void addMemorySpace(MemorySpace memorySpace) {
        memorySpaces.add(memorySpace);

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

    public byte readAfterCpuCycles(int address, int cyclesAhead) {
        address = address & 0xFFFF;
        Optional<Timer> timer = findMemorySpace(Timer.class);
        if (timer.isPresent() && timer.get().contains(address)) {
            return timer.get().readAfterTicks(address, cyclesAhead);
        }
        if (address == 0xFF0F && timer.map(value -> value.requestsInterruptAfterTicks(cyclesAhead)).orElse(false)) {
            return (byte) ((read(address) & 0xFF) | Interrupt.TIMER.getMask());
        }
        return read(address);
    }

    public void write(int address, byte value) {
        address = address & 0xFFFF; // Ensure address is within 16-bit range
        if (deferringCpuWrites) {
            deferredWrites.add(new DeferredWrite(address, value, 0));
            return;
        }
        writeNow(address, value);
    }

    public void beginCpuInstructionWrites() {
        deferringCpuWrites = true;
    }

    public void endCpuInstructionWrites(int remainingCycles) {
        deferringCpuWrites = false;
        for (int i = 0; i < deferredWrites.size(); i++) {
            DeferredWrite write = deferredWrites.get(i);
            deferredWrites.set(i, new DeferredWrite(write.address(), write.value(), Math.max(remainingCycles - 3, 0)));
        }
        applyDueDeferredWrites();
    }

    public void tickDeferredCpuWrites() {
        for (int i = 0; i < deferredWrites.size(); i++) {
            DeferredWrite write = deferredWrites.get(i);
            deferredWrites.set(i, new DeferredWrite(write.address(), write.value(), write.remainingCycles() - 1));
        }
        applyDueDeferredWrites();
    }

    private void writeNow(int address, byte value) {
        for (MemorySpace memorySpace : memorySpaces) {
            if (memorySpace.contains(address)) {
                memorySpace.write(address, value);
                return;
            }
        }
        logger.warn("Address " + Integer.toHexString(address) + " not found in any memory space");
    }

    private void applyDueDeferredWrites() {
        for (int i = 0; i < deferredWrites.size(); ) {
            DeferredWrite write = deferredWrites.get(i);
            if (write.remainingCycles() <= 0) {
                deferredWrites.remove(i);
                writeNow(write.address(), write.value());
            } else {
                i++;
            }
        }
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

    private record DeferredWrite(int address, byte value, int remainingCycles) {
    }
}
