package dev.vitorsilverio.gbcemu.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkRamTest {

    @Test
    void defaultSwitchableBankUsesBankOne() {
        WorkRam workRam = new WorkRam();

        workRam.write(0xD000, (byte) 0x42);

        assertEquals(0x42, workRam.read(0xD000) & 0xFF);
        assertEquals(0xF8, workRam.read(0xFF70) & 0xFF);
    }

    @Test
    void svbkZeroSelectsBankOneWithoutChangingRegisterValue() {
        WorkRam workRam = new WorkRam();

        workRam.write(0xFF70, (byte) 0);
        workRam.write(0xD000, (byte) 0x24);

        assertEquals(0x24, workRam.read(0xD000) & 0xFF);
        assertEquals(1, workRam.getBank());
        assertEquals(0xF8, workRam.read(0xFF70) & 0xFF);
    }

    @Test
    void selectedBanksAreIndependent() {
        WorkRam workRam = new WorkRam();

        workRam.write(0xFF70, (byte) 1);
        workRam.write(0xD000, (byte) 0x11);
        workRam.write(0xFF70, (byte) 2);
        workRam.write(0xD000, (byte) 0x22);

        assertEquals(0x22, workRam.read(0xD000) & 0xFF);
        workRam.write(0xFF70, (byte) 1);
        assertEquals(0x11, workRam.read(0xD000) & 0xFF);
    }

    @Test
    void svbkReadsUnusedBitsAsOneAndSelectedBankInLowBits() {
        WorkRam workRam = new WorkRam();

        workRam.write(0xFF70, (byte) 0xFF);

        assertEquals(0xFF, workRam.read(0xFF70) & 0xFF);
        assertEquals(7, workRam.getBank());
    }
}
