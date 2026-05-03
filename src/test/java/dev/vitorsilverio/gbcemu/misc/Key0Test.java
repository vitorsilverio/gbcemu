package dev.vitorsilverio.gbcemu.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Key0Test {

    @Test
    void storesModeSelectRegisterAtFf4c() {
        Key0 key0 = new Key0();

        key0.write(0xFF4C, (byte) 0x04);

        assertTrue(key0.contains(0xFF4C));
        assertEquals(0x04, key0.read(0xFF4C) & 0xFF);
    }

    @Test
    void notifiesWhenBootRomSelectsDmgCompatibilityMode() {
        boolean[] cgbMode = {true};
        Key0 key0 = new Key0(value -> cgbMode[0] = value);

        key0.write(0xFF4C, (byte) 0x04);

        assertFalse(cgbMode[0]);
    }

    @Test
    void notifiesWhenBootRomKeepsCgbMode() {
        boolean[] cgbMode = {false};
        Key0 key0 = new Key0(value -> cgbMode[0] = value);

        key0.write(0xFF4C, (byte) 0x00);

        assertTrue(cgbMode[0]);
    }
}
