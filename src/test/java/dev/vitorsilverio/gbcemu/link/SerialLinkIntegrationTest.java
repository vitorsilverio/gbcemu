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
    void partnerByteBufferedUntilSlaveTransferStarts() {
        Bus masterBus = new Bus();
        Bus slaveBus = new Bus();
        InMemoryLinkCablePair link = new InMemoryLinkCablePair(masterBus, slaveBus);
        Serial master = link.left();
        Serial slave = link.right();
        masterBus.addMemorySpace(master);
        slaveBus.addMemorySpace(slave);

        master.write(0xFF01, (byte) 0xAA);
        master.write(0xFF02, (byte) 0x81);
        tickMaster(link, 4096);

        assertEquals(0x81, master.read(0xFF02) & 0xFF);
        assertTrue(masterBus.getPendingInterrupt().isEmpty());

        slave.write(0xFF01, (byte) 0x55);
        slave.write(0xFF02, (byte) 0x80);

        masterBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());
        slaveBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());

        assertEquals(0x01, master.read(0xFF02) & 0xFF);
        assertEquals(0x55, master.read(0xFF01) & 0xFF);
        assertEquals(0x00, slave.read(0xFF02) & 0xFF);
        assertEquals(0xAA, slave.read(0xFF01) & 0xFF);
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

    @Test
    void dualMasterArbitration() {
        Bus hostBus = new Bus();
        Bus guestBus = new Bus();
        InMemoryLinkCablePair link = new InMemoryLinkCablePair(hostBus, guestBus);
        Serial hostMaster = link.left(); // host is the effective master
        Serial guestMaster = link.right(); // guest is the effective slave
        hostBus.addMemorySpace(hostMaster);
        guestBus.addMemorySpace(guestMaster);

        // Both want to be master
        hostMaster.write(0xFF01, (byte) 0xAA);
        hostMaster.write(0xFF02, (byte) 0x81);
        guestMaster.write(0xFF01, (byte) 0x55);
        guestMaster.write(0xFF02, (byte) 0x81);

        hostBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());
        guestBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());

        // Tick both. If arbitration works, hostMaster will drive the clock.
        // guestMaster should also complete when hostMaster finishes.
        for (int i = 0; i < 4100; i++) {
            hostMaster.tick();
            guestMaster.tick();
        }

        // Host should have completed
        // Host is master, so it should see 1 in bit 0.
        assertEquals(0x01, hostMaster.read(0xFF02) & 0xFF);
        assertEquals(0x55, hostMaster.read(0xFF01) & 0xFF);
        assertTrue(hostBus.getPendingInterrupt().isPresent());

        // Guest should also have completed as a slave.
        // Bit 0 was forced to 0 during write() by the Guest arbitration logic.
        assertEquals(0x00, guestMaster.read(0xFF02) & 0xFF);
        assertEquals(0xAA, guestMaster.read(0xFF01) & 0xFF);
        assertTrue(guestBus.getPendingInterrupt().isPresent());
    }

    private void tickMaster(InMemoryLinkCablePair link, int cycles) {
        for (int i = 0; i < cycles; i++) {
            link.left().tick();
        }
    }
}
