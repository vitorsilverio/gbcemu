package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class Mbc2Cart extends Cart {

    private int romBank = 1;
    private boolean ramEnabled;

    Mbc2Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x4000) {
            if ((address & 0x0100) == 0) {
                ramEnabled = (unsigned & 0x0F) == 0x0A;
                return;
            }
            romBank = unsigned & 0x0F;
            if (romBank == 0) {
                romBank = 1;
            }
            return;
        }
        if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRomBank(romBank, address);
    }

    @Override
    protected int currentRomBank() {
        return normalizeRomBank(romBank);
    }

    @Override
    protected byte readExternal(int address) {
        if (!ramEnabled) {
            return (byte) 0xFF;
        }
        return (byte) (0xF0 | (readRam(0, address) & 0x0F));
    }

    @Override
    protected void writeExternal(int address, byte value) {
        if (!ramEnabled) {
            return;
        }
        writeRam(0, address, (byte) (value & 0x0F));
    }

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBank", romBank);
        state.put("ramEnabled", ramEnabled);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBank = (int) state.getOrDefault("romBank", 1);
        ramEnabled = (boolean) state.getOrDefault("ramEnabled", false);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("MBC2 ROM register", String.valueOf(romBank));
        properties.put("RAM enabled", String.valueOf(ramEnabled));
        properties.put("Internal RAM", "512 x 4-bit");
    }
}
