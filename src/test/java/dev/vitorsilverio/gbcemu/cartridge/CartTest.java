package dev.vitorsilverio.gbcemu.cartridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
    void mbc1UsesSecondaryRegisterAsUpperRomBankBits() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC1, 0x40, 0x00));

        cart.write(0x2000, (byte) 0x02);
        cart.write(0x4000, (byte) 0x01);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mbc1AdvancedModeMapsSecondaryRegisterIntoFixedRomArea() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC1, 0x40, 0x00));

        cart.write(0x4000, (byte) 0x01);
        cart.write(0x6000, (byte) 0x01);

        assertEquals(0x20, cart.read(0x0000) & 0xFF);
    }

    @Test
    void mbc1RamBankingUsesSecondaryRegisterOnlyInAdvancedMode() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC1_RAM, 0x02, 0x03));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x01);
        cart.write(0xA123, (byte) 0x44);

        assertEquals(0x44, cart.read(0xA123) & 0xFF);

        cart.write(0x6000, (byte) 0x01);

        assertEquals(0x00, cart.read(0xA123) & 0xFF);
    }

    @Test
    void romOnlyIgnoresBankSwitchWrites() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.ROM_ONLY, (byte) 0x11, (byte) 0x22));

        cart.write(0x2000, (byte) 0x01);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
    }

    @Test
    void debugPropertiesExposeHeaderSizeAndChecksumValidation() throws IOException {
        File rom = writeRom(CartridgeType.ROM_ONLY, (byte) 0x11, (byte) 0x22);
        Cart cart = CartFactory.fromFile(rom);

        Map<String, String> properties = cart.debugProperties();

        assertEquals("32768 bytes", properties.get("Header ROM size"));
        assertEquals("0 bytes", properties.get("Header RAM size"));
        assertEquals("false", properties.get("Header checksum valid"));
        assertEquals("false", properties.get("Global checksum valid"));
    }

    @Test
    void romRamMapsExternalRamWithoutBankController() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.ROM_RAM, 0x02, 0x02));

        cart.write(0x0000, (byte) 0x00);
        cart.write(0xA123, (byte) 0x5A);

        assertEquals(0x5A, cart.read(0xA123) & 0xFF);
        assertEquals(0x01, cart.read(0x4000) & 0xFF);
    }

    @Test
    void romRamBatteryPersistsRawRam() throws IOException {
        File rom = writeRom(CartridgeType.ROM_RAM_BATTERY, 0x02, 0x02);
        File save = tempDir.resolve("rom-ram.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0xA123, (byte) 0x44);
        cart.flushSave();

        assertEquals(0x2000, Files.size(save.toPath()));

        Cart loaded = CartFactory.fromFile(rom, save);

        assertEquals(0x44, loaded.read(0xA123) & 0xFF);
    }

    @Test
    void mmm01StartsUnmappedToLastThirtyTwoKiBMenu() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MMM01, 0x08, 0x00));

        assertEquals(0x06, cart.read(0x0000) & 0xFF);
        assertEquals(0x07, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mmm01CanEnterMappedModeAndUseMbc1StyleBanking() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MMM01, 0x08, 0x00));

        cart.write(0x0000, (byte) 0x40);
        cart.write(0x2000, (byte) 0x03);

        assertEquals(0x00, cart.read(0x0000) & 0xFF);
        assertEquals(0x03, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mmm01RamPersistsForBatteryType() throws IOException {
        File rom = writeRom(CartridgeType.MMM01_RAM_BATTERY, 0x02, 0x02);
        File save = tempDir.resolve("mmm01.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x4A);
        cart.write(0xA123, (byte) 0x5E);
        cart.flushSave();

        assertEquals(0x2000, Files.size(save.toPath()));

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x4A);

        assertEquals(0x5E, loaded.read(0xA123) & 0xFF);
    }

    @Test
    void mmm01RomMaskLocksSelectedLowRomBankBits() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MMM01, 0x20, 0x00));

        cart.write(0x2000, (byte) 0x10);
        cart.write(0x6000, (byte) 0x20);
        cart.write(0x0000, (byte) 0x40);
        cart.write(0x2000, (byte) 0x01);

        assertEquals(0x11, cart.read(0x4000) & 0xFF);
    }

    @Test
    void mmm01RamMaskLocksSelectedRamBankBits() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MMM01_RAM, 0x02, 0x03));

        cart.write(0x4000, (byte) 0x02);
        cart.write(0x0000, (byte) 0x6A);
        cart.write(0x6000, (byte) 0x01);
        cart.write(0x4000, (byte) 0x00);
        cart.write(0xA123, (byte) 0x37);
        cart.write(0x4000, (byte) 0x02);

        assertEquals(0x37, cart.read(0xA123) & 0xFF);
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
    void mbc3InvalidRamOrRtcSelectDoesNotAccessPreviousRamBank() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC3_RAM, 0x02, 0x02));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x00);
        cart.write(0xA000, (byte) 0x44);
        cart.write(0x4000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0x66);

        assertEquals(0xFF, cart.read(0xA000) & 0xFF);

        cart.write(0x4000, (byte) 0x00);

        assertEquals(0x44, cart.read(0xA000) & 0xFF);
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

        assertEquals(0x8000, Files.size(save.toPath()));

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);
        loaded.write(0x4000, (byte) 0x01);

        assertEquals(0x66, loaded.read(0xA123) & 0xFF);
    }

    @Test
    void mbc3RtcPersistsToSidecarWithoutChangingRawSaveRam() throws IOException {
        AtomicLong now = new AtomicLong(1_000);
        File rom = writeRom(CartridgeType.MBC3_TIMER_RAM_BATTERY, 0x02, 0x02);
        File save = tempDir.resolve("rtc-game.sav").toFile();
        File rtc = tempDir.resolve("rtc-game.rtc").toFile();
        Cart cart = CartFactory.fromFile(rom, save, now::get);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x08);
        cart.write(0xA000, (byte) 0x2A);
        cart.write(0x4000, (byte) 0x00);
        cart.write(0xA000, (byte) 0x55);
        cart.flushSave();

        assertEquals(0x2000, Files.size(save.toPath()));
        assertEquals(true, rtc.isFile());

        now.set(2_000);
        Cart loaded = CartFactory.fromFile(rom, save, now::get);
        loaded.write(0x0000, (byte) 0x0A);
        loaded.write(0x4000, (byte) 0x08);

        assertEquals(0x2A, loaded.read(0xA000) & 0xFF);

        loaded.write(0x4000, (byte) 0x00);

        assertEquals(0x55, loaded.read(0xA000) & 0xFF);
    }

    @Test
    void mbc3TimerBatteryWithoutRamOnlyPersistsRtcSidecar() throws IOException {
        File rom = writeRom(CartridgeType.MBC3_TIMER_BATTERY, 0x02, 0x00);
        File save = tempDir.resolve("rtc-only.sav").toFile();
        File rtc = tempDir.resolve("rtc-only.rtc").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x4000, (byte) 0x08);
        cart.write(0xA000, (byte) 0x11);
        cart.flushSave();

        assertEquals(false, save.exists());
        assertEquals(true, rtc.isFile());
    }

    @Test
    void mbc2UsesAddressBitEightToSelectRamEnableOrRomBank() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC2, 0x04, 0x00));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x0100, (byte) 0x03);
        cart.write(0xA000, (byte) 0x5A);

        assertEquals(0x03, cart.read(0x4000) & 0xFF);
        assertEquals(0xFA, cart.read(0xA000) & 0xFF);
    }

    @Test
    void mbc2RamEchoesEveryFiveHundredTwelveBytesAndStoresLowerNibble() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.MBC2, 0x02, 0x00));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0xA000, (byte) 0x2B);

        assertEquals(0xFB, cart.read(0xA200) & 0xFF);
    }

    @Test
    void mbc2BatteryRamPersistsRawFiveHundredTwelveBytes() throws IOException {
        File rom = writeRom(CartridgeType.MBC2_BATTERY, 0x02, 0x00);
        File save = tempDir.resolve("mbc2.sav").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0xA123, (byte) 0x6C);
        cart.flushSave();

        assertEquals(512, Files.size(save.toPath()));

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);

        assertEquals(0xFC, loaded.read(0xA123) & 0xFF);
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
    void legacyHeaderedSaveStillLoadsAndMigratesToRawRamOnFlush() throws IOException {
        File rom = writeRom(CartridgeType.MBC3_RAM_BATTERY, 0x02, 0x03);
        File save = tempDir.resolve("legacy.sav").toFile();
        byte[] ram = new byte[0x8000];
        ram[0x2123] = 0x66;
        Files.write(save.toPath(), legacySaveBytes(ram));

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);
        loaded.write(0x4000, (byte) 0x01);

        assertEquals(0x66, loaded.read(0xA123) & 0xFF);

        loaded.flushSave();

        assertEquals(0x8000, Files.size(save.toPath()));
        assertEquals(0x66, Files.readAllBytes(save.toPath())[0x2123] & 0xFF);
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
        assertEquals(true, cart.isRumbleSupported());
        assertEquals(true, cart.isRumbleActive());
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

        assertEquals(0x8000, Files.size(save.toPath()));

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0A);
        loaded.write(0x4000, (byte) 0x01);

        assertEquals(0x66, loaded.read(0xA123) & 0xFF);
    }

    @Test
    void huc1MapsRomAndRamBanksWithoutRamDisable() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC1_RAM_BATTERY, 0x40, 0x03));

        cart.write(0x0000, (byte) 0x00);
        cart.write(0x2000, (byte) 0x22);
        cart.write(0x4000, (byte) 0x02);
        cart.write(0xA123, (byte) 0x55);
        cart.write(0x4000, (byte) 0x00);

        assertEquals(0x22, cart.read(0x4000) & 0xFF);
        assertEquals(0x00, cart.read(0xA123) & 0xFF);

        cart.write(0x4000, (byte) 0x02);

        assertEquals(0x55, cart.read(0xA123) & 0xFF);
    }

    @Test
    void huc1IrModeReturnsNoLightValueAndDoesNotWriteRam() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC1_RAM_BATTERY, 0x02, 0x02));

        cart.write(0xA000, (byte) 0x66);
        cart.write(0x0000, (byte) 0x0E);
        cart.write(0xA000, (byte) 0x01);

        assertEquals(0xC0, cart.read(0xA000) & 0xFF);

        cart.write(0x0000, (byte) 0x00);

        assertEquals(0x66, cart.read(0xA000) & 0xFF);
    }

    @Test
    void huc3MapsRomAndRamBanksWithModeSelection() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC3, 0x80, 0x00));

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0x2000, (byte) 0x45);
        cart.write(0x4000, (byte) 0x03);
        cart.write(0xA123, (byte) 0x5C);
        cart.write(0x4000, (byte) 0x00);

        assertEquals(0x45, cart.read(0x4000) & 0xFF);
        assertEquals(0x00, cart.read(0xA123) & 0xFF);

        cart.write(0x4000, (byte) 0x03);

        assertEquals(0x5C, cart.read(0xA123) & 0xFF);
    }

    @Test
    void huc3RamReadOnlyModeIgnoresRamWrites() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC3, 0x02, 0x02));

        cart.write(0x0000, (byte) 0x00);
        cart.write(0xA000, (byte) 0x77);

        assertEquals(0x00, cart.read(0xA000) & 0xFF);
    }

    @Test
    void huc3SupportsMinimalRtcMailboxProtocol() throws IOException {
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC3, 0x02, 0x00));

        cart.write(0x0000, (byte) 0x0D);
        assertEquals(0x81, cart.read(0xA000) & 0xFF);

        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x42);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x53);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x3A);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x42);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x53);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x10);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0C);

        assertEquals(0x9A, cart.read(0xA000) & 0xFF);
    }

    @Test
    void huc3PersistsRtcSidecarSeparatelyFromRawRam() throws IOException {
        File rom = writeRom(CartridgeType.HuC3, 0x02, 0x02);
        File save = tempDir.resolve("huc3.sav").toFile();
        File rtc = tempDir.resolve("huc3.huc3rtc").toFile();
        Cart cart = CartFactory.fromFile(rom, save);

        cart.write(0x0000, (byte) 0x0A);
        cart.write(0xA000, (byte) 0x77);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x42);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x53);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) 0x3B);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
        cart.flushSave();

        assertEquals(0x8000, Files.size(save.toPath()));
        assertEquals(true, rtc.isFile());

        Cart loaded = CartFactory.fromFile(rom, save);
        loaded.write(0x0000, (byte) 0x0B);
        loaded.write(0xA000, (byte) 0x42);
        loaded.write(0x0000, (byte) 0x0D);
        loaded.write(0xA000, (byte) 0xFE);
        loaded.write(0x0000, (byte) 0x0B);
        loaded.write(0xA000, (byte) 0x53);
        loaded.write(0x0000, (byte) 0x0D);
        loaded.write(0xA000, (byte) 0xFE);
        loaded.write(0x0000, (byte) 0x0B);
        loaded.write(0xA000, (byte) 0x10);
        loaded.write(0x0000, (byte) 0x0D);
        loaded.write(0xA000, (byte) 0xFE);
        loaded.write(0x0000, (byte) 0x0C);

        assertEquals(0x9B, loaded.read(0xA000) & 0xFF);

        loaded.write(0x0000, (byte) 0x0A);

        assertEquals(0x77, loaded.read(0xA000) & 0xFF);
    }

    @Test
    void huc3LatchCommandCopiesElapsedClockIntoReadableRegisters() throws IOException {
        AtomicLong now = new AtomicLong(1_000);
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC3, 0x02, 0x00), null, now::get);

        now.addAndGet(65 * 60);
        huc3Command(cart, 0x60);
        huc3SetIndex(cart, 0x00);

        assertEquals(0x91, huc3ReadNext(cart));
        assertEquals(0x94, huc3ReadNext(cart));
        assertEquals(0x90, huc3ReadNext(cart));
    }

    @Test
    void huc3SetRtcCommandCopiesWritableRegistersBackToLiveClock() throws IOException {
        AtomicLong now = new AtomicLong(1_000);
        Cart cart = CartFactory.fromFile(writeRom(CartridgeType.HuC3, 0x02, 0x00), null, now::get);

        huc3SetIndex(cart, 0x00);
        huc3Command(cart, 0x35);
        huc3Command(cart, 0x30);
        huc3Command(cart, 0x30);
        huc3Command(cart, 0x30);
        huc3Command(cart, 0x30);
        huc3Command(cart, 0x30);
        huc3Command(cart, 0x61);
        now.addAndGet(60);
        huc3SetIndex(cart, 0x10);

        assertEquals(0x96, huc3ReadNext(cart));
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

    private byte[] legacySaveBytes(byte[] ram) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(0x47424353);
            output.writeInt(1);
            output.writeInt(ram.length);
            output.write(ram);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeBoolean(false);
            output.writeBoolean(false);
            output.writeLong(0L);
            for (int i = 0; i < 5; i++) {
                output.writeInt(0);
            }
        }
        return bytes.toByteArray();
    }

    private void huc3SetIndex(Cart cart, int index) {
        huc3Command(cart, 0x40 | (index & 0x0F));
        huc3Command(cart, 0x50 | ((index >> 4) & 0x0F));
    }

    private void huc3Command(Cart cart, int value) {
        cart.write(0x0000, (byte) 0x0B);
        cart.write(0xA000, (byte) value);
        cart.write(0x0000, (byte) 0x0D);
        cart.write(0xA000, (byte) 0xFE);
    }

    private int huc3ReadNext(Cart cart) {
        huc3Command(cart, 0x10);
        cart.write(0x0000, (byte) 0x0C);
        return cart.read(0xA000) & 0xFF;
    }
}
