package dev.vitorsilverio.gbcemu.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnusedIoRegistersTest {

    @Test
    void ownsUnusedIoRegisterAtFf7f() {
        UnusedIoRegisters registers = new UnusedIoRegisters();

        assertTrue(registers.contains(0xFF7F));

        registers.write(0xFF7F, (byte) 0x42);

        assertEquals(0x42, registers.read(0xFF7F) & 0xFF);
    }

    @Test
    void doesNotOwnInterruptEnableOrHram() {
        UnusedIoRegisters registers = new UnusedIoRegisters();

        assertFalse(registers.contains(0xFFFF));
        assertFalse(registers.contains(0xFFFE));
    }
}
