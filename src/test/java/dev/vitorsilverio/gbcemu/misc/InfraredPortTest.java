package dev.vitorsilverio.gbcemu.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraredPortTest {

    @Test
    void ownsCgbInfraredPortRegisterAsLatch() {
        InfraredPort port = new InfraredPort();

        assertTrue(port.contains(0xFF56));

        port.write(0xFF56, (byte) 0xC1);

        assertEquals(0xC1, port.read(0xFF56) & 0xFF);
    }
}
