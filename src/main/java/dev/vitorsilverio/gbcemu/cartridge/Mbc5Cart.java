package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.function.LongSupplier;

public class Mbc5Cart extends Cart {


    private int romBank = 1;
    private int ramBank;
    private int ramSelect;
    private boolean ramEnabled;

    Mbc5Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            ramEnabled = (unsigned & 0x0F) == 0x0A;
        } else if (address < 0x4000) {
            romBank = unsigned & 0x7F;
            if (romBank == 0) {
                romBank = 1;
            }
        } else if (address < 0x6000) {
            ramSelect = unsigned;
            if (unsigned <= 0x07) {
                ramBank = unsigned;
            }
        } else if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRomBank(romBank, address);
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
        if (ramSelect >= 0x08 && ramSelect <= 0x0C) {
            markSaveDirty();
            return;
        }
        writeRam(ramBank, address, value);
    }
}
