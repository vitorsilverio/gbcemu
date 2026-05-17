package dev.vitorsilverio.gbcemu.cartridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

public class Huc3Cart extends Cart {

    private static final int MODE_RAM_READ_ONLY = 0x00;
    private static final int MODE_RAM_READ_WRITE = 0x0A;
    private static final int MODE_RTC_COMMAND_ARGUMENT = 0x0B;
    private static final int MODE_RTC_COMMAND_RESPONSE = 0x0C;
    private static final int MODE_RTC_SEMAPHORE = 0x0D;
    private static final int MODE_IR = 0x0E;

    private final int[] rtcNibbles = new int[0x100];

    private int romBank;
    private int ramBank;
    private int mode = 0x0F;
    private int rtcCommand;
    private int rtcArgument;
    private int rtcResponse = 1;
    private int rtcAddress;
    private boolean infraredTransmitterEnabled;
    private boolean rtcDirty;

    Huc3Cart(byte[] rom, File saveFile) {
        super(rom, saveFile);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            mode = unsigned & 0x0F;
        } else if (address < 0x4000) {
            romBank = unsigned & 0x7F;
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
        return switch (mode) {
            case MODE_RAM_READ_ONLY, MODE_RAM_READ_WRITE -> readRam(ramBank, address);
            case MODE_RTC_COMMAND_RESPONSE -> (byte) (0x80 | ((rtcCommand & 0x07) << 4) | (rtcResponse & 0x0F));
            case MODE_RTC_SEMAPHORE -> (byte) 0x81;
            case MODE_IR -> (byte) 0xC0;
            default -> (byte) 0xFF;
        };
    }

    @Override
    protected void writeExternal(int address, byte value) {
        int unsigned = value & 0xFF;
        switch (mode) {
            case MODE_RAM_READ_WRITE -> writeRam(ramBank, address, value);
            case MODE_RTC_COMMAND_ARGUMENT -> {
                rtcCommand = (unsigned >> 4) & 0x07;
                rtcArgument = unsigned & 0x0F;
            }
            case MODE_RTC_SEMAPHORE -> {
                if ((unsigned & 0x01) == 0) {
                    executeRtcCommand();
                    rtcDirty = true;
                    markSaveDirty();
                }
            }
            case MODE_IR -> infraredTransmitterEnabled = (unsigned & 0x01) != 0;
            default -> {
            }
        }
    }

    @Override
    protected void loadSave() {
        super.loadSave();
        loadRtcSidecar();
    }

    @Override
    void flushSave() {
        super.flushSave();
        flushRtcSidecar();
    }

    private void loadRtcSidecar() {
        File rtcFile = rtcFile();
        if (rtcFile == null || !rtcFile.isFile()) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new FileInputStream(rtcFile))) {
            rtcAddress = input.readUnsignedByte();
            rtcResponse = input.readUnsignedByte();
            for (int i = 0; i < rtcNibbles.length; i++) {
                rtcNibbles[i] = input.readUnsignedByte() & 0x0F;
            }
            rtcDirty = false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load HuC3 RTC file: " + rtcFile, e);
        }
    }

    private void flushRtcSidecar() {
        if (saveFile == null) {
            return;
        }
        File rtcFile = rtcFile();
        if (rtcFile == null) {
            return;
        }
        try {
            File parent = rtcFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            try (DataOutputStream output = new DataOutputStream(new FileOutputStream(rtcFile))) {
                output.writeByte(rtcAddress & 0xFF);
                output.writeByte(rtcResponse & 0x0F);
                for (int value : rtcNibbles) {
                    output.writeByte(value & 0x0F);
                }
            }
            rtcDirty = false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist HuC3 RTC file: " + rtcFile, e);
        }
    }

    private File rtcFile() {
        if (saveFile == null) {
            return null;
        }
        String path = saveFile.getAbsolutePath();
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            return new File(path.substring(0, dot) + ".huc3rtc");
        }
        return new File(path + ".huc3rtc");
    }

    private void executeRtcCommand() {
        switch (rtcCommand) {
            case 0x01 -> {
                rtcResponse = rtcNibbles[rtcAddress];
                rtcAddress = (rtcAddress + 1) & 0xFF;
            }
            case 0x03 -> {
                rtcNibbles[rtcAddress] = rtcArgument & 0x0F;
                rtcResponse = rtcNibbles[rtcAddress];
                rtcAddress = (rtcAddress + 1) & 0xFF;
            }
            case 0x04 -> {
                rtcAddress = (rtcAddress & 0xF0) | (rtcArgument & 0x0F);
                rtcResponse = rtcArgument & 0x0F;
            }
            case 0x05 -> {
                rtcAddress = ((rtcArgument & 0x0F) << 4) | (rtcAddress & 0x0F);
                rtcResponse = rtcArgument & 0x0F;
            }
            case 0x06 -> rtcResponse = rtcArgument == 0x02 ? 0x01 : 0x00;
            default -> rtcResponse = 0x00;
        }
    }

    @Override
    protected void putMapperState(Map<String, Object> state) {
        state.put("romBank", romBank);
        state.put("ramBank", ramBank);
        state.put("mode", mode);
        state.put("rtcCommand", rtcCommand);
        state.put("rtcArgument", rtcArgument);
        state.put("rtcResponse", rtcResponse);
        state.put("rtcAddress", rtcAddress);
        state.put("rtcNibbles", rtcNibbles.clone());
        state.put("infraredTransmitterEnabled", infraredTransmitterEnabled);
        state.put("rtcDirty", rtcDirty);
    }

    @Override
    protected void restoreMapperState(Map<String, Object> state) {
        romBank = (int) state.getOrDefault("romBank", 0);
        ramBank = (int) state.getOrDefault("ramBank", 0);
        mode = (int) state.getOrDefault("mode", 0x0F);
        rtcCommand = (int) state.getOrDefault("rtcCommand", 0);
        rtcArgument = (int) state.getOrDefault("rtcArgument", 0);
        rtcResponse = (int) state.getOrDefault("rtcResponse", 1);
        rtcAddress = (int) state.getOrDefault("rtcAddress", 0);
        Object savedNibbles = state.get("rtcNibbles");
        if (savedNibbles instanceof int[] values) {
            Arrays.fill(rtcNibbles, 0);
            System.arraycopy(values, 0, rtcNibbles, 0, Math.min(values.length, rtcNibbles.length));
        }
        infraredTransmitterEnabled = (boolean) state.getOrDefault("infraredTransmitterEnabled", false);
        rtcDirty = (boolean) state.getOrDefault("rtcDirty", false);
    }

    @Override
    protected void putDebugProperties(Map<String, String> properties) {
        properties.put("HuC3 ROM register", String.valueOf(romBank));
        properties.put("HuC3 RAM register", String.valueOf(ramBank));
        properties.put("HuC3 mode", String.format("%X", mode));
        properties.put("HuC3 RTC command", String.valueOf(rtcCommand));
        properties.put("HuC3 RTC argument", String.valueOf(rtcArgument));
        properties.put("HuC3 RTC response", String.valueOf(rtcResponse));
        properties.put("HuC3 RTC address", String.format("%02X", rtcAddress));
        properties.put("IR transmitter", String.valueOf(infraredTransmitterEnabled));
    }
}
