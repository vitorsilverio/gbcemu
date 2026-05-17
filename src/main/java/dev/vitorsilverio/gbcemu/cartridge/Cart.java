package dev.vitorsilverio.gbcemu.cartridge;

import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.memory.MemoryBankProvider;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Cart implements MemorySpace, MemoryBankProvider, Stateful<CartState> {

    protected static final int ROM_BANK_SIZE = 0x4000;
    protected static final int RAM_BANK_SIZE = 0x2000;

    private static final Logger logger = LoggerFactory.getLogger(Cart.class);
    private static final int SAVE_MAGIC = 0x47424353;
    private static final int SAVE_VERSION = 1;

    protected final CartHeader header;
    protected final byte[] rom;
    protected final CartridgeRam ram;
    private final MemoryBank romMemoryBank = new RomMemoryBank();
    protected final int romBanks;
    protected final File saveFile;

    private boolean saveDirty;

    protected Cart(byte[] rom, File saveFile) {
        this.header = new CartHeader(rom);
        this.rom = rom;
        this.romBanks = Math.max(1, rom.length / ROM_BANK_SIZE);
        this.ram = new CartridgeRam(Math.max(header.getRamSizeBytes(), defaultRamSize(header.getCartridgeType())));
        this.saveFile = saveFile;
    }

    @Override
    public boolean contains(int address) {
        return address < 0x8000 || (address >= 0xA000 && address < 0xC000);
    }

    @Override
    public byte read(int address) {
        if (address < 0x4000) {
            return readFixedRom(address);
        }
        if (address < 0x8000) {
            return readSwitchableRom(address);
        }
        if (address >= 0xA000 && address < 0xC000) {
            return readExternal(address);
        }
        return (byte) 0xFF;
    }

    public CartHeader getHeader() {
        return header;
    }

    public Map<String, String> debugProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("Mapper", getClass().getSimpleName());
        properties.put("Title", header.getTitle());
        properties.put("Type", header.getCartridgeType().name());
        properties.put("CGB compatible", String.valueOf(header.isCgbCompatible()));
        properties.put("ROM size", String.format("%d bytes", rom.length));
        properties.put("ROM banks", String.valueOf(romBanks));
        properties.put("Current ROM bank", String.valueOf(currentRomBank()));
        properties.put("RAM size", String.format("%d bytes", ram.size()));
        properties.put("RAM banks", String.valueOf(ram.bankCount()));
        properties.put("Current RAM bank", String.valueOf(ram.currentBank()));
        properties.put("Rumble supported", String.valueOf(isRumbleSupported()));
        properties.put("Rumble active", String.valueOf(isRumbleActive()));
        properties.put("Save file", saveFile == null ? "(none)" : saveFile.getAbsolutePath());
        properties.put("Save dirty", String.valueOf(saveDirty));
        putDebugProperties(properties);
        return properties;
    }

    public boolean isRumbleSupported() {
        return header.getCartridgeType().hasRumble();
    }

    public boolean isRumbleActive() {
        return false;
    }

    protected byte readFixedRom(int address) {
        return readRom(address);
    }

    protected abstract byte readSwitchableRom(int address);

    protected abstract byte readExternal(int address);

    protected abstract void writeExternal(int address, byte value);

    protected byte readRomBank(int bank, int address) {
        int bankOffset = normalizeRomBank(bank) * ROM_BANK_SIZE;
        return readRom(bankOffset + (address - 0x4000));
    }

    protected byte readRomBankAllowZero(int bank, int address) {
        int normalized = romBanks == 0 ? 0 : bank % romBanks;
        int bankOffset = normalized * ROM_BANK_SIZE;
        return readRom(bankOffset + (address - 0x4000));
    }

    protected byte readRom(int address) {
        if (rom.length == 0) {
            return (byte) 0xFF;
        }
        return rom[address % rom.length];
    }

    protected byte readRam(int bank, int address) {
        return ram.read(bank, address);
    }

    protected void writeRam(int bank, int address, byte value) {
        if (ram.size() == 0) {
            return;
        }
        ram.write(bank, address, value);
        markSaveDirty();
    }

    @Override
    public List<MemoryBank> memoryBanks() {
        return List.of(romMemoryBank, ram);
    }

    protected int currentRomBank() {
        return romBanks > 1 ? 1 : 0;
    }

    protected int normalizeRomBank(int bank) {
        if (romBanks == 1) {
            return 0;
        }
        int normalized = bank % romBanks;
        return normalized == 0 ? 1 : normalized;
    }

    protected void markSaveDirty() {
        if (saveFile == null || !header.getCartridgeType().hasBattery()) {
            return;
        }
        saveDirty = true;
    }

    protected void loadSave() {
        if (saveFile == null || !saveFile.isFile()) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(saveFile.toPath());
            if (!hasLegacySaveHeader(bytes)) {
                ram.restoreData(bytes);
                return;
            }
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                input.readInt();
                int version = input.readInt();
                if (version != SAVE_VERSION) {
                    ram.restoreData(bytes);
                    return;
                }
                int ramLength = input.readInt();
                if (ramLength < 0 || ramLength > input.available()) {
                    ram.restoreData(bytes);
                    return;
                }
                if (ramLength > 0) {
                    byte[] savedRam = new byte[ramLength];
                    input.readFully(savedRam);
                    ram.restoreData(savedRam);
                }
                try {
                    loadExtraSaveData(input);
                } catch (IOException e) {
                    logger.warn("Failed to load legacy cartridge save extra data", e);
                }
                saveDirty = header.getCartridgeType().hasBattery();
            }
        } catch (IOException e) {
            logger.warn("Failed to load cartridge save", e);
        }
    }

    protected void loadExtraSaveData(DataInputStream input) throws IOException {
    }

    void flushSave() {
        if (!saveDirty) {
            return;
        }
        if (saveFile == null || !header.getCartridgeType().hasBattery()) {
            return;
        }
        try {
            File parent = saveFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            Files.write(saveFile.toPath(), ram.copyData());
            saveDirty = false;
        } catch (IOException e) {
            logger.warn("Failed to persist cartridge save", e);
        }
    }

    protected void installShutdownSaveHook() {
        if (saveFile == null || !header.getCartridgeType().hasBattery()) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::flushSave, "gbcemu-cart-save"));
    }

    protected void writeExtraSaveData(DataOutputStream output) throws IOException {
    }

    @Override
    public CartState saveState() {
        Map<String, Object> mapperState = new HashMap<>();
        putMapperState(mapperState);
        return new CartState(ram.saveState(), mapperState);
    }

    @Override
    public void loadState(CartState state) {
        ram.loadState(state.externalRam());
        restoreMapperState(state.mapperState());
    }

    protected void putMapperState(Map<String, Object> state) {
    }

    protected void restoreMapperState(Map<String, Object> state) {
    }

    protected void putDebugProperties(Map<String, String> properties) {
    }

    private int defaultRamSize(CartridgeType type) {
        return switch (type) {
            case MBC2, MBC2_BATTERY -> 512;
            case MBC1_RAM, MBC1_RAM_BATTERY, MMM01_RAM, MMM01_RAM_BATTERY, ROM_RAM, ROM_RAM_BATTERY,
                 MBC3_RAM, MBC3_RAM_BATTERY, MBC3_TIMER_RAM_BATTERY, MBC5_RAM, MBC5_RAM_BATTERY, MBC5_RUMBLE_RAM,
                 MBC5_RUMBLE_RAM_BATTERY -> RAM_BANK_SIZE;
            case HuC1_RAM_BATTERY, HuC3 -> 4 * RAM_BANK_SIZE;
            default -> 0;
        };
    }

    private boolean hasLegacySaveHeader(byte[] bytes) {
        if (bytes.length < Integer.BYTES * 2) {
            return false;
        }
        int magic = ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
        int version = ((bytes[4] & 0xFF) << 24)
                | ((bytes[5] & 0xFF) << 16)
                | ((bytes[6] & 0xFF) << 8)
                | (bytes[7] & 0xFF);
        return magic == SAVE_MAGIC && version == SAVE_VERSION;
    }

    private class RomMemoryBank implements MemoryBank {

        @Override
        public String bankName() {
            return "Cartridge ROM";
        }

        @Override
        public int bankCount() {
            return romBanks;
        }

        @Override
        public int bankSize() {
            return ROM_BANK_SIZE;
        }

        @Override
        public int currentBank() {
            return currentRomBank();
        }

        @Override
        public byte readBank(int bank, int offset) {
            return readRom(Math.floorMod(bank, romBanks) * ROM_BANK_SIZE + Math.floorMod(offset, ROM_BANK_SIZE));
        }

        @Override
        public void writeBank(int bank, int offset, byte value) {
        }
    }
}
