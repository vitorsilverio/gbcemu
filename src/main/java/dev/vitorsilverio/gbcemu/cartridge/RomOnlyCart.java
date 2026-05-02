package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;

public class RomOnlyCart extends Cart {

    RomOnlyCart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRom(address);
    }

    @Override
    protected byte readExternal(int address) {
        return (byte) 0xFF;
    }

    @Override
    protected void writeExternal(int address, byte value) {
    }
}
