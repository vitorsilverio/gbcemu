package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerialTest {

    @Test
    void transferClearsStartBitAndRequestsInterrupt() {
        Bus bus = new Bus();
        Serial serial = new Serial(bus);
        bus.addMemorySpace(serial);

        serial.write(0xFF01, (byte) 0xC7);
        serial.write(0xFF02, (byte) 0x81);

        assertEquals(0x01, serial.read(0xFF02) & 0xFF);
        assertEquals(0xC7, serial.read(0xFF01) & 0xFF);
        bus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());
        assertEquals(Interrupt.SERIAL, bus.getPendingInterrupt().orElseThrow());
    }
}
