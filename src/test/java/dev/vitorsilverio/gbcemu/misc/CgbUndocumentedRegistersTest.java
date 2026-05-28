package dev.vitorsilverio.gbcemu.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CgbUndocumentedRegistersTest {

    @Test
    void ownsCgbUndocumentedRegisterRange() {
        CgbUndocumentedRegisters registers = new CgbUndocumentedRegisters();

        assertTrue(registers.contains(0xFF72));
        assertTrue(registers.contains(0xFF75));
        assertFalse(registers.contains(0xFF71));
        assertFalse(registers.contains(0xFF76));
    }

    @Test
    void ff72ToFf74AreFullReadWriteLatches() {
        CgbUndocumentedRegisters registers = new CgbUndocumentedRegisters();

        registers.write(0xFF72, (byte) 0x12);
        registers.write(0xFF73, (byte) 0x34);
        registers.write(0xFF74, (byte) 0x56);

        assertEquals(0x12, registers.read(0xFF72) & 0xFF);
        assertEquals(0x34, registers.read(0xFF73) & 0xFF);
        assertEquals(0x56, registers.read(0xFF74) & 0xFF);
    }

    @Test
    void ff75OnlyStoresBitsFourToSixAndReadsOtherBitsAsOne() {
        CgbUndocumentedRegisters registers = new CgbUndocumentedRegisters();

        assertEquals(0x8F, registers.read(0xFF75) & 0xFF);

        registers.write(0xFF75, (byte) 0xFF);

        assertEquals(0xFF, registers.read(0xFF75) & 0xFF);

        registers.write(0xFF75, (byte) 0x00);

        assertEquals(0x8F, registers.read(0xFF75) & 0xFF);
    }

    @Test
    void stateRestoresRegisterValues() {
        CgbUndocumentedRegisters registers = new CgbUndocumentedRegisters();
        registers.write(0xFF72, (byte) 0x12);
        registers.write(0xFF75, (byte) 0x70);

        CgbUndocumentedRegisters restored = new CgbUndocumentedRegisters();
        restored.loadState(registers.saveState());

        assertEquals(0x12, restored.read(0xFF72) & 0xFF);
        assertEquals(0xFF, restored.read(0xFF75) & 0xFF);
    }
}
