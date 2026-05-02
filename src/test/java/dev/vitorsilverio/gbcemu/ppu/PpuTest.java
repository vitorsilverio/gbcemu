package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PpuTest {

    @Test
    void statIncludesModeWhenLcdIsEnabled() {
        Ppu ppu = new Ppu(new Bus());

        ppu.write(0xFF40, (byte) 0x80);

        assertEquals(PpuMode.OAM_READ.getValue(), ppu.read(0xFF41) & 0x03);
    }

    @Test
    void scrollRegistersAreUnsigned() {
        Ppu ppu = new Ppu(new Bus());

        ppu.write(0xFF42, (byte) 0xFE);
        ppu.write(0xFF43, (byte) 0x80);

        assertEquals(0xFE, ppu.read(0xFF42) & 0xFF);
        assertEquals(0x80, ppu.read(0xFF43) & 0xFF);
    }
}
