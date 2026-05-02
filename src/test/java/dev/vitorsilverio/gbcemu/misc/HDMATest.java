package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HDMATest {

    @Test
    void staysIdleUntilHdma5StartsTransfer() {
        Bus bus = new Bus();
        SourceRam sourceRam = new SourceRam(0x4000, 0x4010);
        bus.addMemorySpace(sourceRam);
        bus.addMemorySpace(new Ppu(bus));
        HDMA hdma = new HDMA(bus);

        hdma.tick();

        assertFalse(hdma.isActive());
    }

    @Test
    void generalPurposeTransferCopiesOneSixteenByteBlockToVram() {
        Bus bus = new Bus();
        SourceRam sourceRam = new SourceRam(0x4000, 0x4010);
        bus.addMemorySpace(sourceRam);
        Ppu ppu = new Ppu(bus);
        bus.addMemorySpace(ppu);
        HDMA hdma = new HDMA(bus);

        hdma.write(0xFF51, (byte) 0x40);
        hdma.write(0xFF52, (byte) 0x0F);
        hdma.write(0xFF53, (byte) 0x10);
        hdma.write(0xFF54, (byte) 0x0F);
        hdma.write(0xFF55, (byte) 0x00);
        tickEightTimes(hdma);

        assertFalse(hdma.isActive());
        assertEquals(0xFF, hdma.read(0xFF55) & 0xFF);
        assertEquals(0x80, ppu.read(0x9000) & 0xFF);
        assertEquals(0x8F, ppu.read(0x900F) & 0xFF);
    }

    @Test
    void hblankTransferCanBeStoppedWithoutMarkingCompleted() {
        Bus bus = new Bus();
        bus.addMemorySpace(new SourceRam(0x4000, 0x4010));
        bus.addMemorySpace(new Ppu(bus));
        HDMA hdma = new HDMA(bus);

        hdma.write(0xFF55, (byte) 0x81);

        assertTrue(hdma.isActive());

        hdma.write(0xFF55, (byte) 0x00);

        assertFalse(hdma.isActive());
        assertEquals(0x81, hdma.read(0xFF55) & 0xFF);
    }

    private void tickEightTimes(HDMA hdma) {
        for (int i = 0; i < 8; i++) {
            hdma.tick();
        }
    }

    private static class SourceRam implements MemorySpace {
        private final int start;
        private final int end;

        private SourceRam(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean contains(int address) {
            return start <= address && address < end;
        }

        @Override
        public byte read(int address) {
            return (byte) (0x80 + (address - start));
        }

        @Override
        public void write(int address, byte value) {
        }
    }
}
