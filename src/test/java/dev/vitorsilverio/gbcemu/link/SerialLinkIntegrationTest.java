package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialLinkIntegrationTest {

    @Test
    void masterAndSlaveExchangeByteThroughLinkCable() {
        Bus masterBus = new Bus();
        Bus slaveBus = new Bus();
        InMemoryLinkCablePair link = new InMemoryLinkCablePair(masterBus, slaveBus);
        Serial master = link.left();
        Serial slave = link.right();
        masterBus.addMemorySpace(master);
        slaveBus.addMemorySpace(slave);

        master.write(0xFF01, (byte) 0xAA);
        master.write(0xFF02, (byte) 0x81);
        slave.write(0xFF01, (byte) 0x55);
        slave.write(0xFF02, (byte) 0x80);

        masterBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());
        slaveBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());

        tickMaster(link, 4096);

        assertEquals(0x01, master.read(0xFF02) & 0xFF);
        assertEquals(0x55, master.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, masterBus.getPendingInterrupt().orElseThrow());

        assertEquals(0x00, slave.read(0xFF02) & 0xFF);
        assertEquals(0xAA, slave.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, slaveBus.getPendingInterrupt().orElseThrow());
    }

    @Test
    void externalClockSlaveDoesNotCompleteWithoutPartner() {
        Bus bus = new Bus();
        InMemoryLinkCablePair link = new InMemoryLinkCablePair(bus, new Bus());
        Serial slave = link.right();
        bus.addMemorySpace(slave);

        slave.write(0xFF01, (byte) 0xC7);
        slave.write(0xFF02, (byte) 0x80);

        for (int i = 0; i < 5000; i++) {
            slave.tick();
        }

        assertEquals(0x80, slave.read(0xFF02) & 0xFF);
        assertTrue(bus.getPendingInterrupt().isEmpty());
    }

    private void tickMaster(InMemoryLinkCablePair link, int cycles) {
        for (int i = 0; i < cycles; i++) {
            link.left().tick();
        }
    }
}
