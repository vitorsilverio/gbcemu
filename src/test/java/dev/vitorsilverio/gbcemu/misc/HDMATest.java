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
        bus.addMemorySpace(new SourceRam(0x4000, 0x4020));
        bus.addMemorySpace(new Ppu(bus));
        HDMA hdma = new HDMA(bus);

        hdma.write(0xFF55, (byte) 0x81);

        assertTrue(hdma.isActive());

        hdma.write(0xFF55, (byte) 0x00);

        assertFalse(hdma.isActive());
        assertEquals(0x81, hdma.read(0xFF55) & 0xFF);
    }

    @Test
    void stoppedHblankTransferReportsRemainingBlocksAfterPartialCopy() {
        Bus bus = new Bus();
        bus.addMemorySpace(new SourceRam(0x4000, 0x4040));
        bus.addMemorySpace(new Ppu(bus));
        HDMA hdma = new HDMA(bus);

        hdma.write(0xFF51, (byte) 0x40);
        hdma.write(0xFF55, (byte) 0x83);
        tickEightTimes(hdma);

        assertEquals(0x02, hdma.read(0xFF55) & 0xFF);

        hdma.write(0xFF55, (byte) 0x00);

        assertFalse(hdma.isActive());
        assertEquals(0x82, hdma.read(0xFF55) & 0xFF);
    }

    @Test
    void hblankTransferCopiesOnlyOneBlockPerHBlank() {
        Bus bus = new Bus();
        bus.addMemorySpace(new SourceRam(0x4000, 0x4020));
        Ppu ppu = new Ppu(bus);
        bus.addMemorySpace(ppu);
        HDMA hdma = new HDMA(bus);

        hdma.write(0xFF51, (byte) 0x40);
        hdma.write(0xFF53, (byte) 0x00);
        hdma.write(0xFF55, (byte) 0x81);
        tickEightTimes(hdma);
        tickEightTimes(hdma);

        assertTrue(hdma.isActive());
        assertEquals(0x00, hdma.read(0xFF55) & 0xFF);
        assertEquals(0x80, ppu.read(0x8000) & 0xFF);
        assertEquals(0x8F, ppu.read(0x800F) & 0xFF);
        assertEquals(0x00, ppu.read(0x8010) & 0xFF);

        hdma.leaveHBlank();
        tickEightTimes(hdma);

        assertFalse(hdma.isActive());
        assertEquals(0xFF, hdma.read(0xFF55) & 0xFF);
        assertEquals(0x90, ppu.read(0x8010) & 0xFF);
        assertEquals(0x9F, ppu.read(0x801F) & 0xFF);
    }

    @Test
    void hdmaAddressRegistersAreWriteOnlyAndUseMaskedAddressesForTransfer() {
        Bus bus = new Bus();
        bus.addMemorySpace(new SourceRam(0x4010, 0x4020));
        Ppu ppu = new Ppu(bus);
        bus.addMemorySpace(ppu);
        HDMA hdma = new HDMA(bus);

        hdma.write(0xFF51, (byte) 0x40);
        hdma.write(0xFF52, (byte) 0x1F);
        hdma.write(0xFF53, (byte) 0xFF);
        hdma.write(0xFF54, (byte) 0xFF);
        hdma.write(0xFF55, (byte) 0x00);
        tickEightTimes(hdma);

        assertEquals(0xFF, hdma.read(0xFF51) & 0xFF);
        assertEquals(0xFF, hdma.read(0xFF52) & 0xFF);
        assertEquals(0xFF, hdma.read(0xFF53) & 0xFF);
        assertEquals(0xFF, hdma.read(0xFF54) & 0xFF);
        assertEquals(0x80, ppu.read(0x9FF0) & 0xFF);
        assertEquals(0x8F, ppu.read(0x9FFF) & 0xFF);
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
