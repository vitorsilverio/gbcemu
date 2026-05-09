package dev.vitorsilverio.gbcemu.cartridge;

import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.memory.MemoryBankProvider;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Snapshot;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Cart implements MemorySpace, MemoryBankProvider, Snapshottable {

    protected static final int ROM_BANK_SIZE = 0x4000;
    protected static final int RAM_BANK_SIZE = 0x2000;

    private static final Logger logger = LoggerFactory.getLogger(Cart.class);
    private static final int SAVE_MAGIC = 0x47424353;
    private static final int SAVE_VERSION = 1;

    protected final CartHeader header;
    protected final byte[] rom;
    protected final CartridgeRam ram;
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
        return List.of(ram);
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
        try (DataInputStream input = new DataInputStream(Files.newInputStream(saveFile.toPath()))) {
            int magic = input.readInt();
            if (magic != SAVE_MAGIC) {
                loadRawRamSave();
                return;
            }
            int version = input.readInt();
            if (version != SAVE_VERSION) {
                return;
            }
            int ramLength = input.readInt();
            if (ramLength > 0) {
                byte[] savedRam = new byte[ramLength];
                input.readFully(savedRam);
                ram.restoreData(savedRam);
            }
            loadExtraSaveData(input);
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
            Files.write(saveFile.toPath(), saveBytes());
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
    public Snapshot createSnapshot(int version) {
        Map<String, Object> state = new HashMap<>();
        state.put("externalRam", ram.saveState());
        putMapperState(state);
        return new Snapshot(getClass().getName(), version, state);
    }

    @Override
    public void restoreSnapshot(Snapshot snapshot) {
        Object externalRamState = snapshot.state().get("externalRam");
        if (externalRamState instanceof CartridgeRamState cartridgeRamState) {
            ram.loadState(cartridgeRamState);
        } else if (snapshot.state().get("ram") instanceof byte[] savedRam) {
            ram.restoreData(savedRam);
        }
        restoreMapperState(snapshot.state());
    }

    protected void putMapperState(Map<String, Object> state) {
    }

    protected void restoreMapperState(Map<String, Object> state) {
    }

    private int defaultRamSize(CartridgeType type) {
        return switch (type) {
            case MBC1_RAM, MBC1_RAM_BATTERY, ROM_RAM, ROM_RAM_BATTERY, MBC3_RAM, MBC3_RAM_BATTERY,
                 MBC3_TIMER_RAM_BATTERY, MBC5_RAM, MBC5_RAM_BATTERY, MBC5_RUMBLE_RAM,
                 MBC5_RUMBLE_RAM_BATTERY -> RAM_BANK_SIZE;
            default -> 0;
        };
    }

    private void loadRawRamSave() throws IOException {
        byte[] bytes = Files.readAllBytes(saveFile.toPath());
        ram.restoreData(bytes);
    }

    private byte[] saveBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(SAVE_MAGIC);
            output.writeInt(SAVE_VERSION);
            byte[] ramData = ram.copyData();
            output.writeInt(ramData.length);
            output.write(ramData);
            writeExtraSaveData(output);
        }
        return bytes.toByteArray();
    }
}
