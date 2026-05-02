package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;

public class Key1 implements MemorySpace {

    private static final int KEY1_REGISTER = 0xFF4D;

    private boolean prepareSpeedSwitch;
    private boolean doubleSpeed;

    @Override
    public boolean contains(int address) {
        return address == KEY1_REGISTER;
    }

    @Override
    public byte read(int address) {
        return (byte) (0x7E | (doubleSpeed ? 0x80 : 0) | (prepareSpeedSwitch ? 0x01 : 0));
    }

    @Override
    public void write(int address, byte value) {
        prepareSpeedSwitch = (value & 0x01) != 0;
    }

    public boolean isPrepareSpeedSwitch() {
        return prepareSpeedSwitch;
    }

    public boolean isDoubleSpeed() {
        return doubleSpeed;
    }

    public boolean switchSpeedIfPrepared() {
        if (!prepareSpeedSwitch) {
            return false;
        }
        doubleSpeed = !doubleSpeed;
        prepareSpeedSwitch = false;
        return true;
    }
}
