package dev.vitorsilverio.gbcemu.cartridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartTest {

    @TempDir
    Path tempDir;

    @Test
    void mbc1BankSelectionWrapsToAvailableRomBanks() throws IOException {
        Cart cart = new Cart(writeRom(CartridgeType.MBC1, (byte) 0x11, (byte) 0x22));

        cart.write(0x2000, (byte) 25);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mbc1RamEnableAreaDoesNotSelectRomBank() throws IOException {
        Cart cart = new Cart(writeRom(CartridgeType.MBC1, (byte) 0x11, (byte) 0x22));

        cart.write(0x0000, (byte) 0x0A);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void romOnlyIgnoresBankSwitchWrites() throws IOException {
        Cart cart = new Cart(writeRom(CartridgeType.ROM_ONLY, (byte) 0x11, (byte) 0x22));

        cart.write(0x2000, (byte) 0x01);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    private File writeRom(CartridgeType cartridgeType, byte bank0Value, byte bank1Value) throws IOException {
        byte[] rom = new byte[0x8000];
        rom[0] = bank0Value;
        rom[0x4000] = bank1Value;
        rom[0x0147] = (byte) cartridgeType.getCode();
        rom[0x0148] = 0;

        Path romFile = tempDir.resolve("test.gb");
        Files.write(romFile, rom);
        return romFile.toFile();
    }
}
