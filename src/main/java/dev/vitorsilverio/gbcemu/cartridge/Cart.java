package dev.vitorsilverio.gbcemu.cartridge;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Cart implements MemorySpace {

    private static final Logger logger = LoggerFactory.getLogger(Cart.class);

    private final CartHeader header;
    private final byte[] rom;
    private final byte[] ram;
    private final int romBanks;
    private int romBank = 1;

    public Cart(File romFile) {
        try(var inputStream = romFile.toURI().toURL().openStream()) {
            this.rom = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.ram = new byte[0x2000];
        this.romBanks = Math.max(1, rom.length / 0x4000);
        this.header = new CartHeader(rom);
        logger.info("Cartridge: " + header);
    }

    @Override
    public boolean contains(int address) {
        return address < 0x8000 || (address >= 0xA000 && address < 0xC000);
    }

    @Override
    public byte read(int address) {
        if (address < 0x4000) {
            return rom[address];
        } else if (address < 0x8000) {
            int bankOffset = normalizeRomBank(romBank) * 0x4000;
            return rom[bankOffset + (address - 0x4000)];
        } else if (address >= 0xA000 && address < 0xC000) {
            return ram[address - 0xA000];
        }
        return (byte) 0xff;
    }

    @Override
    public void write(int address, byte value) {
        if (header.getCartridgeType() == CartridgeType.ROM_ONLY) {
            return;
        }

        if (address >= 0x2000 && address < 0x4000) {
            romBank = value & 0x1F;
            if (romBank == 0) {
                romBank = 1;
            }
        }
    }

    public CartHeader getHeader() {
        return header;
    }

    private int normalizeRomBank(int bank) {
        if (romBanks == 1) {
            return 0;
        }
        int normalized = bank % romBanks;
        return normalized == 0 ? 1 : normalized;
    }
}
