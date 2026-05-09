package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class Mbc1Cart extends Cart {

    private int romBank = 1;
    private int ramBank;
    private boolean ramEnabled;

    Mbc1Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        if (address < 0x2000) {
            ramEnabled = (value & 0x0F) == 0x0A;
        } else if (address < 0x4000) {
            romBank = value & 0x1F;
            if (romBank == 0) {
                romBank = 1;
            }
        } else if (address < 0x6000) {
            ramBank = value & 0x03;
        } else if (address >= 0xA000 && address < 0xC000) {
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
        return readRam(ramBank, address);
    }

    @Override
    protected void writeExternal(int address, byte value) {
        if (!ramEnabled) {
            return;
        }
        writeRam(ramBank, address, value);
    }

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBank", romBank);
        state.put("ramBank", ramBank);
        state.put("ramEnabled", ramEnabled);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBank = (int) state.getOrDefault("romBank", 1);
        ramBank = (int) state.getOrDefault("ramBank", 0);
        ramEnabled = (boolean) state.getOrDefault("ramEnabled", false);
    }
}
