package dev.vitorsilverio.gbcemu.interrupt;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;

import java.util.List;
import java.util.Optional;

public class InterruptManager implements MemorySpace, Snapshottable {

    private static final int IF_REG = 0xFF0F;
    private static final int IE_REG = 0xFFFF;
    private static final int INTERRUPT_BITS = 0x1F;
    private static final int IF_UNUSED_BITS = 0xE0;
    private static final List<Integer> REGISTERS = List.of(IE_REG, IF_REG);

    @Savable private byte ieReg;
    @Savable private byte ifReg;

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address);
    }

    @Override
    public byte read(int address) {
        if (address == IF_REG) {
            return (byte) ((ifReg & INTERRUPT_BITS) | IF_UNUSED_BITS);
        } else if (address == IE_REG) {
            return ieReg;
        }
        throw new IllegalArgumentException("Address " + address + " not found in any memory space");
    }

    @Override
    public void write(int address, byte value) {
        if (address == IF_REG) {
            ifReg = (byte) (value & INTERRUPT_BITS);
        } else if (address == IE_REG) {
            ieReg = value;
        } else {
            throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }
    }

    public void requestInterrupt(Interrupt interrupt) {
        // Set the corresponding bit in the IF register
        ifReg = (byte) ((ifReg | interrupt.getMask()) & INTERRUPT_BITS);
    }

    public void clearInterrupt(Interrupt interrupt) {
        // Clear the corresponding bit in the IF register
        ifReg = (byte) (ifReg & ~interrupt.getMask() & INTERRUPT_BITS);

    }

    public Optional<Interrupt> getPendingInterrupt() {
        for (Interrupt interrupt : Interrupt.values()) {
            if ((ifReg & ieReg & interrupt.getMask() & INTERRUPT_BITS) != 0) {
                return Optional.of(interrupt);
            }
        }
        return Optional.empty();
    }
}
