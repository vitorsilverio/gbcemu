package dev.vitorsilverio.gbcemu.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraredPortTest {

    @Test
    void ownsCgbInfraredPortRegister() {
        InfraredPort port = new InfraredPort();

        assertTrue(port.contains(0xFF56));
        assertEquals(0x3E, port.read(0xFF56) & 0xFF);
    }

    @Test
    void readKeepsUnusedBitsHighAndExposesWritableControlBits() {
        InfraredPort port = new InfraredPort();

        port.write(0xFF56, (byte) 0xC1);

        assertEquals(0xFF, port.read(0xFF56) & 0xFF);
    }

    @Test
    void writeMasksToEmittingAndReadEnableBits() {
        InfraredPort port = new InfraredPort();

        port.write(0xFF56, (byte) 0xFF);
        port.write(0xFF56, (byte) 0x3E);

        assertEquals(0x3E, port.read(0xFF56) & 0xFF);
    }
}
