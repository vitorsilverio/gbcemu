package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DMATest {

    @Test
    void copiesAllOamBytesUsingUnsignedBaseAndOffset() {
        Bus bus = new Bus();
        Ppu ppu = new Ppu(bus);
        DMA dma = new DMA(bus);
        bus.addMemorySpace(new SourceRam(0xC000, 0xC0A0));
        bus.addMemorySpace(ppu);

        dma.write(0xFF46, (byte) 0xC0);
        for (int i = 0; i < 160; i++) {
            dma.tick();
        }
        ppu.write(0xFF40, (byte) 0x80);
        for (int i = 0; i < 252; i++) {
            ppu.tick();
        }

        assertFalse(dma.isActive());
        assertEquals(0x00, ppu.read(0xFE00) & 0xFF);
        assertEquals(0x7F, ppu.read(0xFE7F) & 0xFF);
        assertEquals(0x80, ppu.read(0xFE80) & 0xFF);
        assertEquals(0x9F, ppu.read(0xFE9F) & 0xFF);
    }

    @Test
    void dmaRegisterReadsLastWrittenSourceHighByte() {
        DMA dma = new DMA(new Bus());

        assertEquals(0x00, dma.read(0xFF46) & 0xFF);

        dma.write(0xFF46, (byte) 0xC0);

        assertEquals(0xC0, dma.read(0xFF46) & 0xFF);
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
            return (byte) (address - start);
        }

        @Override
        public void write(int address, byte value) {
        }
    }
}
