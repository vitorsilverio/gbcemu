package dev.vitorsilverio.gbcemu.cartridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartTest {

    @TempDir
    Path tempDir;

    @Test
    void mbc1BankSelectionWrapsToAvailableRomBanks() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC1, (byte) 0x11, (byte) 0x22));

        cart.write(0x2000, (byte) 25);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mbc1RamEnableAreaDoesNotSelectRomBank() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC1, (byte) 0x11, (byte) 0x22));

        cart.write(0x0000, (byte) 0x0A);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void romOnlyIgnoresBankSwitchWrites() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.ROM_ONLY, (byte) 0x11, (byte) 0x22));

        cart.write(0x2000, (byte) 0x01);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mbc3CanSelectRomBankTwenty() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC3, 0x40, 0x00));

        cart.write(0x2000, (byte) 0x20);

        assertEquals(0x20, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mbc3MapsExternalRamBanksWhenEnabled() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC3_RAM, 0x04, 0x03));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x01);
        cart.write(0xA000, (byte) 0x5A);
        cart.write(0x4000, (byte) 0x00);

        assertEquals(0x00, cart.read(0xA000) & 0xFF);

        cart.write(0x4000, (byte) 0x01);

        assertEquals(0x5A, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc3RamAndRtcReadsAsFfWhenDisabled() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC3_RAM, 0x02, 0x02));

        cart.write(0xA000, (byte) 0x5A);

        assertEquals(0xFF, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc3RtcLatchesCurrentTimeOnZeroToOneTransition() throws IOException {
        AtomicLong now = new AtomicLong(1_000);
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC3_TIMER_BATTERY, 0x02, 0x00), null, now::get);

        cart.write(0x0000, (byte) 0x0A);
        now.addAndGet(65);
        cart.write(0x6000, (byte) 0x00);
        cart.write(0x6000, (byte) 0x01);
        cart.write(0x4000, (byte) 0x08);

        assertEquals(5, cart.read(0xA000) & 0xFF);

        cart.write(0x4000, (byte) 0x09);

        assertEquals(1, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc3RtcCanBeHaltedAndWritten() throws IOException {
        AtomicLong now = new AtomicLong(1_000);
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC3_TIMER_BATTERY, 0x02, 0x00), null, now::get);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x0C);
        cart.write(0xA000, (byte) 0x40);
        cart.write(0x4000, (byte) 0x08);
        cart.write(0xA000, (byte) 0x30);
        now.addAndGet(20);
        cart.write(0x6000, (byte) 0x00);
        cart.write(0x6000, (byte) 0x01);

        assertEquals(0x30, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc3BatteryRamPersistsToSaveFile() throws IOException {
        File rom = writeRom(CartridgeType.MBC3_RAM_BATTERY, 0x02, 0x03);
        File save = tempDir.resolve("test.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x01);
        cart.write(0xA123, (byte) 0x66);
        cart.flushSave();

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);
        loaded.write(0x4000, (byte) 0x01);

        assertEquals(0x66, loaded.read(0xA123) & 0xFF);
    }

    @Test
    void mbc3BatteryRamStaysInMemoryWhenExternalRamIsDisabled() throws IOException {
        File rom = writeRom(CartridgeType.MBC3_RAM_BATTERY, 0x02, 0x03);
        File save = tempDir.resolve("disable-does-not-flush.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0xA000, (byte) 0x77);
        cart.write(0x0000, (byte) 0x00);

        assertEquals(false, save.exists());

        cart.write(0x0000, (byte) 0x0A);

        assertEquals(0x77, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc3BatteryRamFlushSavePersistsExplicitly() throws IOException {
        File rom = writeRom(CartridgeType.MBC3_RAM_BATTERY, 0x02, 0x03);
        File save = tempDir.resolve("explicit-flush.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0xA000, (byte) 0x77);
        cart.flushSave();

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);

        assertEquals(0x77, loaded.read(0xA000) & 0xFF);
    }

    @Test
    void mbc5AllowsSelectingRomBankZeroInSwitchableArea() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC5, 0x02, 0x00));

        cart.write(0x2000, (byte) 0x00);

        assertEquals(0x00, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mbc5UsesNineBitRomBankNumber() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC5, 0x101, 0x00));

        cart.write(0x2000, (byte) 0x00);
        cart.write(0x3000, (byte) 0x01);

        assertEquals(0x00, cart.read(0x4000) & 0xFF);
        assertEquals(0x01, cart.read(0x4001) & 0xFF);
    }

    @Test
    void mbc5MapsSixteenExternalRamBanksWhenEnabled() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC5_RAM, 0x02, 0x04));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x0F);
        cart.write(0xA000, (byte) 0x5A);
        cart.write(0x4000, (byte) 0x00);

        assertEquals(0x00, cart.read(0xA000) & 0xFF);

        cart.write(0x4000, (byte) 0x0F);

        assertEquals(0x5A, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc5RamReadsAsFfWhenDisabled() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC5_RAM, 0x02, 0x02));

        cart.write(0xA000, (byte) 0x5A);

        assertEquals(0xFF, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc5RumbleUsesBitThreeWithoutSelectingRamBankEight() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC5_RUMBLE_RAM, 0x02, 0x04));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x01);
        cart.write(0xA000, (byte) 0x11);
        cart.write(0x4000, (byte) 0x09);
        cart.write(0xA000, (byte) 0x99);

        assertEquals(0x99, cart.read(0xA000) & 0xFF);
        assertEquals(true, ((Mbc5Cart) cart).isRumbleEnabled());
    }

    @Test
    void mbc5BatteryRamPersistsToSaveFile() throws IOException {
        File rom = writeRom(CartridgeType.MBC5_RAM_BATTERY, 0x02, 0x03);
        File save = tempDir.resolve("mbc5.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x01);
        cart.write(0xA123, (byte) 0x66);
        cart.flushSave();

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);
        loaded.write(0x4000, (byte) 0x01);

        assertEquals(0x66, loaded.read(0xA123) & 0xFF);
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

    private File writeRom(CartridgeType cartridgeType, int romBanks, int ramSizeCode) throws IOException {
        byte[] rom = new byte[0x4000 * romBanks];
        for (int bank = 0; bank < romBanks; bank++) {
            rom[bank * 0x4000] = (byte) bank;
            rom[bank * 0x4000 + 1] = (byte) (bank >> 8);
        }
        rom[0x0147] = (byte) cartridgeType.getCode();
        rom[0x0148] = 0;
        rom[0x0149] = (byte) ramSizeCode;

        Path romFile = tempDir.resolve(cartridgeType.name() + romBanks + ".gb");
        Files.write(romFile, rom);
        return romFile.toFile();
    }
}
