package dev.vitorsilverio.gbcemu.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeroPageTest {

    @Test
    void includesLastHramByteBeforeInterruptEnableRegister() {
        ZeroPage zeroPage = new ZeroPage();

        assertTrue(zeroPage.contains(0xFFFE));

        zeroPage.write(0xFFFE, (byte) 0x42);

        assertEquals(0x42, zeroPage.read(0xFFFE) & 0xFF);
    }
}
