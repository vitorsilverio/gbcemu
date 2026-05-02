package dev.vitorsilverio.gbcemu.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Key1Test {

    @Test
    void exposesSpeedSwitchRegisterAtFf4d() {
        Key1 key1 = new Key1();

        assertTrue(key1.contains(0xFF4D));
        assertEquals(0x7E, key1.read(0xFF4D) & 0xFF);
    }

    @Test
    void storesPrepareSpeedSwitchBit() {
        Key1 key1 = new Key1();

        key1.write(0xFF4D, (byte) 0x01);

        assertEquals(0x7F, key1.read(0xFF4D) & 0xFF);
    }

    @Test
    void switchesSpeedOnlyWhenPreparedAndClearsPrepareBit() {
        Key1 key1 = new Key1();

        org.junit.jupiter.api.Assertions.assertFalse(key1.switchSpeedIfPrepared());

        key1.write(0xFF4D, (byte) 0x01);

        assertTrue(key1.switchSpeedIfPrepared());
        assertEquals(0xFE, key1.read(0xFF4D) & 0xFF);
        org.junit.jupiter.api.Assertions.assertFalse(key1.isPrepareSpeedSwitch());
        assertTrue(key1.isDoubleSpeed());
    }
}
