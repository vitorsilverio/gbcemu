package dev.vitorsilverio.gbcemu.interrupt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterruptManagerTest {

    @Test
    void interruptFlagReadsUnusedBitsAsSet() {
        InterruptManager interrupts = new InterruptManager();

        interrupts.write(0xFF0F, (byte) 0x00);

        assertEquals(0xE0, interrupts.read(0xFF0F) & 0xFF);
    }

    @Test
    void interruptFlagStoresOnlyInterruptRequestBits() {
        InterruptManager interrupts = new InterruptManager();

        interrupts.write(0xFF0F, (byte) 0xFF);
        interrupts.clearInterrupt(Interrupt.TIMER);

        assertEquals(0xFB, interrupts.read(0xFF0F) & 0xFF);
    }

    @Test
    void pendingInterruptIgnoresUnusedInterruptFlagBits() {
        InterruptManager interrupts = new InterruptManager();

        interrupts.write(0xFFFF, (byte) 0xE0);
        interrupts.write(0xFF0F, (byte) 0xE0);

        assertTrue(interrupts.getPendingInterrupt().isEmpty());
    }
}
