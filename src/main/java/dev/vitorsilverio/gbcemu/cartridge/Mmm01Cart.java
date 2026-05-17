package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class Mmm01Cart extends Cart {

    private int romBankLow;
    private int secondaryBank;
    private int ramBank;
    private boolean ramEnabled;
    private boolean mapped;
    private boolean advancedBankingMode;

    Mmm01Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            ramEnabled = (unsigned & 0x0F) == 0x0A;
            if (!mapped && (unsigned & 0x40) != 0) {
                mapped = true;
            }
        } else if (address < 0x4000) {
            romBankLow = unsigned & 0x1F;
            if (!mapped) {
                secondaryBank = (unsigned >> 5) & 0x03;
            }
        } else if (address < 0x6000) {
            ramBank = unsigned & 0x03;
            if (!mapped) {
                secondaryBank = (unsigned >> 4) & 0x03;
            }
        } else if (address < 0x8000) {
            advancedBankingMode = (unsigned & 0x01) != 0;
        } else if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readFixedRom(int address) {
        if (!mapped) {
            return readRom(unmappedMenuOffset() + address);
        }
        if (!advancedBankingMode) {
            return super.readFixedRom(address);
        }
        int bank = secondaryBank << 5;
        return readRom((Math.floorMod(bank, romBanks) * ROM_BANK_SIZE) + address);
    }

    @Override
    protected byte readSwitchableRom(int address) {
        if (!mapped) {
            return readRom(unmappedMenuOffset() + ROM_BANK_SIZE + (address - 0x4000));
        }
        return readRomBankAllowZero(selectedRomBank(), address);
    }

    @Override
    protected int currentRomBank() {
        if (!mapped) {
            return Math.max(0, romBanks - 1);
        }
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

    private int unmappedMenuOffset() {
        return Math.max(0, rom.length - 0x8000);
    }

    private int selectedRomBank() {
        int low = romBankLow == 0 ? 1 : romBankLow;
        return (secondaryBank << 5) | low;
    }

    private int selectedRamBank() {
        return advancedBankingMode ? ramBank : 0;
    }

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBankLow", romBankLow);
        state.put("secondaryBank", secondaryBank);
        state.put("ramBank", ramBank);
        state.put("ramEnabled", ramEnabled);
        state.put("mapped", mapped);
        state.put("advancedBankingMode", advancedBankingMode);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBankLow = (int) state.getOrDefault("romBankLow", 0);
        secondaryBank = (int) state.getOrDefault("secondaryBank", 0);
        ramBank = (int) state.getOrDefault("ramBank", 0);
        ramEnabled = (boolean) state.getOrDefault("ramEnabled", false);
        mapped = (boolean) state.getOrDefault("mapped", false);
        advancedBankingMode = (boolean) state.getOrDefault("advancedBankingMode", false);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("MMM01 mapped", String.valueOf(mapped));
        properties.put("MMM01 ROM low register", String.valueOf(romBankLow));
        properties.put("MMM01 secondary register", String.valueOf(secondaryBank));
        properties.put("MMM01 RAM register", String.valueOf(ramBank));
        properties.put("MMM01 banking mode", advancedBankingMode ? "advanced" : "simple");
        properties.put("RAM enabled", String.valueOf(ramEnabled));
    }
}
