package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.misc.Key1;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimerTest {

    @Test
    void tacReadPreservesSelectedClockZero() {
        Timer timer = new Timer(new Bus());

        timer.write(0xFF07, (byte) 0x04);

        assertEquals(0xFC, timer.read(0xFF07) & 0xFF);
    }

    @Test
    void timerIncrementsOnSelectedCounterBitFallingEdge() {
        Timer timer = new Timer(new Bus());
        timer.write(0xFF07, (byte) 0x05);

        tick(timer, 16);

        assertEquals(1, timer.read(0xFF05) & 0xFF);
    }

    @Test
    void divResetCanTriggerTimerIncrement() {
        Timer timer = new Timer(new Bus());
        timer.write(0xFF07, (byte) 0x05);
        tick(timer, 8);

        timer.write(0xFF04, (byte) 0x00);

        assertEquals(1, timer.read(0xFF05) & 0xFF);
        assertEquals(0, timer.read(0xFF04) & 0xFF);
    }

    @Test
    void overflowReloadsModuloAndRequestsInterruptOneMachineCycleLater() {
        Bus bus = new Bus();
        Timer timer = new Timer(bus);
        timer.write(0xFF06, (byte) 0xAB);
        timer.write(0xFF05, (byte) 0xFF);
        timer.write(0xFF07, (byte) 0x05);

        tick(timer, 16);

        assertEquals(0x00, timer.read(0xFF05) & 0xFF);

        tick(timer, 3);

        assertEquals(0x00, timer.read(0xFF05) & 0xFF);

        timer.tick();

        assertEquals(0xAB, timer.read(0xFF05) & 0xFF);
        bus.write(0xFFFF, (byte) Interrupt.TIMER.getMask());
        assertEquals(Interrupt.TIMER, bus.getPendingInterrupt().orElseThrow());
    }

    @Test
    void doubleSpeedAdvancesSystemCounterTwicePerTick() {
        Bus bus = new Bus();
        Key1 key1 = new Key1();
        Timer timer = new Timer(bus);
        bus.addMemorySpace(key1);
        key1.write(0xFF4D, (byte) 0x01);
        key1.switchSpeedIfPrepared();
        timer.write(0xFF07, (byte) 0x05);

        tick(timer, 8);

        assertEquals(1, timer.read(0xFF05) & 0xFF);
    }

    private void tick(Timer timer, int ticks) {
        for (int i = 0; i < ticks; i++) {
            timer.tick();
        }
    }
}
