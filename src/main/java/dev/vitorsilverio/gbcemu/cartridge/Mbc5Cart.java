package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class Mbc5Cart extends Cart {


    private int romBankLow;
    private int romBankHigh;
    private int ramBank;
    private boolean ramEnabled;
    private boolean rumbleEnabled;

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
    protected int currentRomBank() {
        return Math.floorMod(selectedRomBank(), romBanks);
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

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBankLow", romBankLow);
        state.put("romBankHigh", romBankHigh);
        state.put("ramBank", ramBank);
        state.put("ramEnabled", ramEnabled);
        state.put("rumbleEnabled", rumbleEnabled);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBankLow = (int) state.getOrDefault("romBankLow", 0);
        romBankHigh = (int) state.getOrDefault("romBankHigh", 0);
        ramBank = (int) state.getOrDefault("ramBank", 0);
        ramEnabled = (boolean) state.getOrDefault("ramEnabled", false);
        rumbleEnabled = (boolean) state.getOrDefault("rumbleEnabled", false);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("MBC5 ROM low", String.format("%02X", romBankLow));
        properties.put("MBC5 ROM high", String.valueOf(romBankHigh));
        properties.put("Selected ROM bank", String.valueOf(selectedRomBank()));
        properties.put("MBC5 RAM register", String.valueOf(ramBank));
        properties.put("RAM enabled", String.valueOf(ramEnabled));
        properties.put("Rumble enabled", String.valueOf(rumbleEnabled));
    }
}
