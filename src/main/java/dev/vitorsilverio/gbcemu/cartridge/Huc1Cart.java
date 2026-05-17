package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.util.Map;

public class Huc1Cart extends Cart {

    private int romBank = 1;
    private int ramBank;
    private boolean irMode;
    private boolean infraredTransmitterEnabled;

    Huc1Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            irMode = unsigned == 0x0E;
        } else if (address < 0x4000) {
            romBank = unsigned & 0x3F;
        } else if (address < 0x6000) {
            ramBank = unsigned & 0x03;
        } else if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRomBankAllowZero(romBank, address);
    }

    @Override
    protected int currentRomBank() {
        return Math.floorMod(romBank, romBanks);
    }

    @Override
    protected byte readExternal(int address) {
        if (irMode) {
            return (byte) 0xC0;
        }
        return readRam(ramBank, address);
    }

    @Override
    protected void writeExternal(int address, byte value) {
        if (irMode) {
            infraredTransmitterEnabled = (value & 0x01) != 0;
            return;
        }
        writeRam(ramBank, address, value);
    }

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBank", romBank);
        state.put("ramBank", ramBank);
        state.put("irMode", irMode);
        state.put("infraredTransmitterEnabled", infraredTransmitterEnabled);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBank = (int) state.getOrDefault("romBank", 1);
        ramBank = (int) state.getOrDefault("ramBank", 0);
        irMode = (boolean) state.getOrDefault("irMode", false);
        infraredTransmitterEnabled = (boolean) state.getOrDefault("infraredTransmitterEnabled", false);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("HuC1 ROM register", String.valueOf(romBank));
        properties.put("HuC1 RAM register", String.valueOf(ramBank));
        properties.put("IR mode", String.valueOf(irMode));
        properties.put("IR transmitter", String.valueOf(infraredTransmitterEnabled));
    }
}
