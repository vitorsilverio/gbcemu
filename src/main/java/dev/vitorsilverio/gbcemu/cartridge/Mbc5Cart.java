package dev.vitorsilverio.gbcemu.cartridge;

import dev.vitorsilverio.gbcemu.snapshot.Savable;

import java.io.File;

public class Mbc5Cart extends Cart {


    @Savable private int romBankLow;
    @Savable private int romBankHigh;
    @Savable private int ramBank;
    @Savable private boolean ramEnabled;
    @Savable private boolean rumbleEnabled;

    Mbc5Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            ramEnabled = (unsigned & 0x0F) == 0x0A;
        } else if (address < 0x3000) {
            romBankLow = unsigned;
        } else if (address < 0x4000) {
            romBankHigh = unsigned & 0x01;
        } else if (address < 0x6000) {
            selectRamBank(unsigned);
        } else if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRomBankAllowZero(selectedRomBank(), address);
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

    boolean isRumbleEnabled() {
        return rumbleEnabled;
    }

    private int selectedRomBank() {
        return ((romBankHigh & 0x01) << 8) | romBankLow;
    }

    private void selectRamBank(int value) {
        if (header.getCartridgeType().hasRumble()) {
            rumbleEnabled = (value & 0x08) != 0;
            ramBank = value & 0x07;
            return;
        }
        ramBank = value & 0x0F;
    }
}
