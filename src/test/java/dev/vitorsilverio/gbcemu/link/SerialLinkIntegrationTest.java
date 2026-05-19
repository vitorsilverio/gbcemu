package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialLinkIntegrationTest {

    @Test
    void directLinkKeepsPlayer2TransferArmedWhileExposingExternalClock() {
        Bus player1Bus = new Bus();
        Bus player2Bus = new Bus();
        DirectLinkCable.Pair cables = DirectLinkCable.createPair();
        Serial player1 = new Serial(player1Bus, cables.player1());
        Serial player2 = new Serial(player2Bus, cables.player2());
        player1Bus.addMemorySpace(player1);
        player2Bus.addMemorySpace(player2);

        player1.write(0xFF01, (byte) 0xAA);
        player2.write(0xFF01, (byte) 0x55);
        player1.write(0xFF02, (byte) 0x81);
        player2.write(0xFF02, (byte) 0x81);

        assertEquals(0xFD, player1.read(0xFF02) & 0xFF);
        assertEquals(0xFC, player2.read(0xFF02) & 0xFF);
        assertTrue(player2.isTransferActive());

        for (int i = 0; i < 4096; i++) {
            player1.tick();
            player2.tick();
        }

        assertEquals(0x55, player1.read(0xFF01) & 0xFF);
        assertEquals(0xAA, player2.read(0xFF01) & 0xFF);
        assertEquals(0x7D, player1.read(0xFF02) & 0xFF);
        assertEquals(0x7C, player2.read(0xFF02) & 0xFF);
    }

    @Test
    void directLinkInternalClockCompletesWithOpenBusWhenPeerIsNotArmed() {
        Bus player1Bus = new Bus();
        Bus player2Bus = new Bus();
        DirectLinkCable.Pair cables = DirectLinkCable.createPair();
        Serial player1 = new Serial(player1Bus, cables.player1());
        Serial player2 = new Serial(player2Bus, cables.player2());
        player1Bus.addMemorySpace(player1);
        player2Bus.addMemorySpace(player2);

        player1.write(0xFF01, (byte) 0xAA);
        player2.write(0xFF01, (byte) 0x55);
        player1.write(0xFF02, (byte) 0x81);

        for (int i = 0; i < 4096; i++) {
            player1.tick();
            player2.tick();
        }

        assertEquals(0xFF, player1.read(0xFF01) & 0xFF);
        assertEquals(0x7D, player1.read(0xFF02) & 0xFF);
        assertEquals(0x55, player2.read(0xFF01) & 0xFF);
        assertEquals(0x7C, player2.read(0xFF02) & 0xFF);
    }

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

        assertEquals(0x7D, master.read(0xFF02) & 0xFF);
        assertEquals(0x55, master.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, masterBus.getPendingInterrupt().orElseThrow());

        assertEquals(0x7C, slave.read(0xFF02) & 0xFF);
        assertEquals(0xAA, slave.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, slaveBus.getPendingInterrupt().orElseThrow());
    }

    @Test
    void partnerClockBeforeSlaveTransferStartsIsHeldUntilSlaveArms() {
        Bus masterBus = new Bus();
        Bus slaveBus = new Bus();
        InMemoryLinkCablePair link = new InMemoryLinkCablePair(masterBus, slaveBus);
        Serial master = link.left();
        Serial slave = link.right();
        masterBus.addMemorySpace(master);
        slaveBus.addMemorySpace(slave);

        master.write(0xFF01, (byte) 0xAA);
        master.write(0xFF02, (byte) 0x81);
        masterBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());
        tickMaster(link, 4096);

        assertEquals(0xFD, master.read(0xFF02) & 0xFF);
        assertTrue(masterBus.getPendingInterrupt().isEmpty());

        slave.write(0xFF01, (byte) 0x55);
        slave.write(0xFF02, (byte) 0x80);

        slaveBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());

        assertEquals(0x7D, master.read(0xFF02) & 0xFF);
        assertEquals(0x55, master.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, masterBus.getPendingInterrupt().orElseThrow());

        assertEquals(0x7C, slave.read(0xFF02) & 0xFF);
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

        assertEquals(0xFC, slave.read(0xFF02) & 0xFF);
        assertTrue(bus.getPendingInterrupt().isEmpty());
    }

    @Test
    void internalClockCompletesWithOpenBusWhenPeerTransferDoesNotArmBeforeTimeout() {
        Bus masterBus = new Bus();
        Bus slaveBus = new Bus();
        InMemoryLinkCablePair link = new InMemoryLinkCablePair(masterBus, slaveBus);
        Serial master = link.left();
        masterBus.addMemorySpace(master);

        master.write(0xFF01, (byte) 0xAA);
        master.write(0xFF02, (byte) 0x81);
        masterBus.write(0xFFFF, (byte) Interrupt.SERIAL.getMask());

        tickMaster(link, 4096);
        assertEquals(0xFD, master.read(0xFF02) & 0xFF);
        assertTrue(masterBus.getPendingInterrupt().isEmpty());

        for (int tick = 0; tick < 8192; tick++) {
            link.tickRightCable();
        }

        assertEquals(0x7D, master.read(0xFF02) & 0xFF);
        assertEquals(0xFF, master.read(0xFF01) & 0xFF);
        assertEquals(Interrupt.SERIAL, masterBus.getPendingInterrupt().orElseThrow());
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
        assertEquals(0x7D, hostMaster.read(0xFF02) & 0xFF);
        assertEquals(0x55, hostMaster.read(0xFF01) & 0xFF);
        assertTrue(hostBus.getPendingInterrupt().isPresent());

        // Guest should also have completed through the host-driven transfer.
        // During dual-master arbitration, the effective slave exposes external clock to the game.
        assertEquals(0x7C, guestMaster.read(0xFF02) & 0xFF);
        assertEquals(0xAA, guestMaster.read(0xFF01) & 0xFF);
        assertTrue(guestBus.getPendingInterrupt().isPresent());
    }

    private void tickMaster(InMemoryLinkCablePair link, int cycles) {
        for (int i = 0; i < cycles; i++) {
            link.left().tick();
        }
    }
}
