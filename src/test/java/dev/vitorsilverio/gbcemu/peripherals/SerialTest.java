package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.misc.Key1;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialTest {

    @Test
    void internalTransferCompletesAfterNormalSpeedSerialClock() {
        Bus bus = new Bus();
        Serial serial = new Serial(bus, new LinkCable(new Multiplayer()));
        bus.addMemorySpace(serial);

        serial.write(0xFF01, (byte) 0xC7);
        serial.write(0xFF02, (byte) 0x81);

        assertEquals(0xFD, serial.read(0xFF02) & 0xFF);
        assertEquals(0xC7, serial.read(0xFF01) & 0xFF);
        bus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());
        assertTrue(bus.getPendingInterrupt().isEmpty());

        tick(serial, 4095);

        assertEquals(0xFD, serial.read(0xFF02) & 0xFF);
        assertTrue(bus.getPendingInterrupt().isEmpty());

        serial.tick();

        assertEquals(0x7D, serial.read(0xFF02) & 0xFF);
        assertEquals(0xFF, serial.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, bus.getPendingInterrupt().orElseThrow());
    }

    @Test
    void cgbFastClockCompletesSooner() {
        Bus bus = new Bus();
        Serial serial = new Serial(bus, new LinkCable(new Multiplayer()));
        bus.addMemorySpace(serial);

        serial.write(0xFF01, (byte) 0x42);
        serial.write(0xFF02, (byte) 0x83);

        tick(serial, 127);

        assertEquals(0xFF, serial.read(0xFF02) & 0xFF);

        serial.tick();

        assertEquals(0x7F, serial.read(0xFF02) & 0xFF);
    }

    @Test
    void doubleSpeedHalvesInternalTransferTime() {
        Bus bus = new Bus();
        Key1 key1 = new Key1();
        key1.write(0xFF4D, (byte) 0x01);
        key1.switchSpeedIfPrepared();
        bus.addMemorySpace(key1);
        Serial serial = new Serial(bus, new LinkCable(new Multiplayer()));
        bus.addMemorySpace(serial);

        serial.write(0xFF01, (byte) 0x42);
        serial.write(0xFF02, (byte) 0x81);

        tick(serial, 2047);

        assertEquals(0xFD, serial.read(0xFF02) & 0xFF);

        serial.tick();

        assertEquals(0x7D, serial.read(0xFF02) & 0xFF);
    }

    @Test
    void externalClockTransferStaysPendingWithoutClock() {
        Bus bus = new Bus();
        Serial serial = new Serial(bus, new LinkCable(new Multiplayer()));
        bus.addMemorySpace(serial);

        serial.write(0xFF01, (byte) 0xC7);
        serial.write(0xFF02, (byte) 0x80);

        tick(serial, 5000);

        assertEquals(0xFC, serial.read(0xFF02) & 0xFF);
        assertEquals(0xC7, serial.read(0xFF01) & 0xFF);
        assertTrue(bus.getPendingInterrupt().isEmpty());
    }

    private void tick(Serial serial, int cycles) {
        for (int i = 0; i < cycles; i++) {
            serial.tick();
        }
    }
}
