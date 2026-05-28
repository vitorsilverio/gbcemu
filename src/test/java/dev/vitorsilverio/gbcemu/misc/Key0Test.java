package dev.vitorsilverio.gbcemu.misc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void ignoresWritesAfterBootRomLocksModeSelect() {
        boolean[] cgbMode = {true};
        Key0 key0 = new Key0(value -> cgbMode[0] = value);

        key0.write(0xFF4C, (byte) 0x04);
        key0.lock();
        key0.write(0xFF4C, (byte) 0x00);

        assertEquals(0x04, key0.read(0xFF4C) & 0xFF);
        assertFalse(cgbMode[0]);
    }

    @Test
    void saveStatePreservesLockedModeSelect() {
        Key0 key0 = new Key0();
        key0.write(0xFF4C, (byte) 0x04);
        key0.lock();

        Key0 restored = new Key0();
        restored.loadState(key0.saveState());
        restored.write(0xFF4C, (byte) 0x00);

        assertEquals(0x04, restored.read(0xFF4C) & 0xFF);
    }
}
