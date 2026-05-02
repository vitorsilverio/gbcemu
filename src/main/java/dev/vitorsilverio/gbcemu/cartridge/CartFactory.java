package dev.vitorsilverio.gbcemu.cartridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.function.LongSupplier;

public final class CartFactory {

    private static final Logger logger = LoggerFactory.getLogger(CartFactory.class);

    private CartFactory() {
    }

    public static Cart fromFile(File romFile) {
        return fromFile(romFile, null);
    }

    public static Cart fromFile(File romFile, File saveFile) {
        return fromFile(romFile, saveFile, () -> Instant.now().getEpochSecond());
    }

    static Cart fromFile(File romFile, File saveFile, LongSupplier currentEpochSeconds) {
        byte[] rom = readRom(romFile);
        CartHeader header = new CartHeader(rom);
        Cart cart = switch (header.getCartridgeType()) {
            case ROM_ONLY -> new RomOnlyCart(rom, saveFile);
            case MBC1, MBC1_RAM, MBC1_RAM_BATTERY -> new Mbc1Cart(rom, saveFile);
            case MBC3_TIMER_BATTERY, MBC3_TIMER_RAM_BATTERY, MBC3, MBC3_RAM, MBC3_RAM_BATTERY ->
                    new Mbc3Cart(rom, saveFile, currentEpochSeconds);
            default -> throw new UnsupportedOperationException("Unsupported cartridge type: " + header.getCartridgeType());
        };
        cart.loadSave();
        cart.installShutdownSaveHook();
        logger.info("Cartridge: " + cart.getHeader());
        return cart;
    }

    private static byte[] readRom(File romFile) {
        try {
            return Files.readAllBytes(romFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
