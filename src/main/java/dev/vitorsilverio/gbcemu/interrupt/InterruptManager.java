package dev.vitorsilverio.gbcemu.interrupt;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;

import java.util.List;
import java.util.Optional;

public class InterruptManager implements MemorySpace {

    private static final int IF_REG = 0xFF0F;
    private static final int IE_REG = 0xFFFF;
    private static final List<Integer> REGISTERS = List.of(IE_REG, IF_REG);

    private byte ieReg;
    private byte ifReg;

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address);
    }

    @Override
    public byte read(int address) {
        if (address == IF_REG) {
            return ifReg;
        } else if (address == IE_REG) {
            return ieReg;
        }
        throw new IllegalArgumentException("Address " + address + " not found in any memory space");
    }

    @Override
    public void write(int address, byte value) {
        if (address == IF_REG) {
            ifReg = value;
        } else if (address == IE_REG) {
            ieReg = value;
        } else {
            throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }
    }

    public void requestInterrupt(Interrupt interrupt) {
        // Set the corresponding bit in the IF register
        ifReg |= (byte) interrupt.getMask();
    }

    public void clearInterrupt(Interrupt interrupt) {
        // Clear the corresponding bit in the IF register
        ifReg &= (byte) ~interrupt.getMask();

    }

    public Optional<Interrupt> getPendingInterrupt() {
        for (Interrupt interrupt : Interrupt.values()) {
            if ((ifReg & interrupt.getMask()) != 0 && (ieReg & interrupt.getMask()) != 0) {
                return Optional.of(interrupt);
            }
        }
        return Optional.empty();
    }
}
