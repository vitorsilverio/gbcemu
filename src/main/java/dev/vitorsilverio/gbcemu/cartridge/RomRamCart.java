package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class RomRamCart extends Cart {

    RomRamCart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRom(address);
    }

    @Override
    protected byte readExternal(int address) {
        return readRam(0, address);
    }

    @Override
    protected void writeExternal(int address, byte value) {
        writeRam(0, address, value);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("RAM enabled", "always");
    }
}
