package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class Mbc1Cart extends Cart {

    private int romBankLow;
    private int secondaryBank;
    private boolean advancedBankingMode;
    private boolean ramEnabled;

    Mbc1Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            ramEnabled = (value & 0x0F) == 0x0A;
        } else if (address < 0x4000) {
            romBankLow = unsigned & 0x1F;
        } else if (address < 0x6000) {
            secondaryBank = unsigned & 0x03;
        } else if (address < 0x8000) {
            advancedBankingMode = (unsigned & 0x01) != 0;
        } else if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readFixedRom(int address) {
        if (!advancedBankingMode) {
            return super.readFixedRom(address);
        }
        int bank = secondaryBank << 5;
        return readRom((Math.floorMod(bank, romBanks) * ROM_BANK_SIZE) + address);
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRomBankAllowZero(selectedRomBank(), address);
    }

    @Override
    protected int currentRomBank() {
        return Math.floorMod(selectedRomBank(), romBanks);
    }

    @Override
    protected byte readExternal(int address) {
        if (!ramEnabled) {
            return (byte) 0xFF;
        }
        return readRam(selectedRamBank(), address);
    }

    @Override
    protected void writeExternal(int address, byte value) {
        if (!ramEnabled) {
            return;
        }
        writeRam(selectedRamBank(), address, value);
    }

    private int selectedRomBank() {
        int low = romBankLow == 0 ? 1 : romBankLow;
        return (secondaryBank << 5) | low;
    }

    private int selectedRamBank() {
        return advancedBankingMode ? secondaryBank : 0;
    }

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBankLow", romBankLow);
        state.put("secondaryBank", secondaryBank);
        state.put("advancedBankingMode", advancedBankingMode);
        state.put("ramEnabled", ramEnabled);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBankLow = (int) state.getOrDefault("romBankLow", state.getOrDefault("romBank", 1));
        secondaryBank = (int) state.getOrDefault("secondaryBank", state.getOrDefault("ramBank", 0));
        advancedBankingMode = (boolean) state.getOrDefault("advancedBankingMode", false);
        ramEnabled = (boolean) state.getOrDefault("ramEnabled", false);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("MBC1 ROM low register", String.valueOf(romBankLow));
        properties.put("MBC1 secondary register", String.valueOf(secondaryBank));
        properties.put("MBC1 banking mode", advancedBankingMode ? "advanced" : "simple");
        properties.put("Selected ROM bank", String.valueOf(selectedRomBank()));
        properties.put("Selected RAM bank", String.valueOf(selectedRamBank()));
        properties.put("RAM enabled", String.valueOf(ramEnabled));
    }
}
