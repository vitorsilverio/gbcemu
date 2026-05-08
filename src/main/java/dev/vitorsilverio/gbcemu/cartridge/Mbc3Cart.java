package dev.vitorsilverio.gbcemu.cartridge;

import dev.vitorsilverio.gbcemu.snapshot.Savable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.LongSupplier;

public class Mbc3Cart extends Cart {

    private final Mbc3Rtc rtc;

    @Savable private int romBank = 1;
    @Savable private int ramBank;
    @Savable private int ramOrRtcSelect;
    @Savable private int latchValue = 0xFF;
    @Savable private boolean ramAndTimerEnabled;

    Mbc3Cart(byte[] rom, File saveFile, LongSupplier currentEpochSeconds) {
        super(rom, saveFile);
        this.rtc = new Mbc3Rtc(currentEpochSeconds);
    }

    @Override
    public void write(int address, byte value) {
        int unsigned = value & 0xFF;
        if (address < 0x2000) {
            ramAndTimerEnabled = (unsigned & 0x0F) == 0x0A;
        } else if (address < 0x4000) {
            romBank = unsigned & 0x7F;
            if (romBank == 0) {
                romBank = 1;
            }
        } else if (address < 0x6000) {
            ramOrRtcSelect = unsigned;
            if (unsigned <= 0x07) {
                ramBank = unsigned;
            }
        } else if (address < 0x8000) {
            latchRtc(unsigned);
        } else if (address >= 0xA000 && address < 0xC000) {
            writeExternal(address, value);
        }
    }

    @Override
    protected byte readSwitchableRom(int address) {
        return readRomBank(romBank, address);
    }

    @Override
    protected byte readExternal(int address) {
        if (!ramAndTimerEnabled) {
            return (byte) 0xFF;
        }
        if (ramOrRtcSelect >= 0x08 && ramOrRtcSelect <= 0x0C) {
            return rtc.read(ramOrRtcSelect);
        }
        return readRam(ramBank, address);
    }

    @Override
    protected void writeExternal(int address, byte value) {
        if (!ramAndTimerEnabled) {
            return;
        }
        if (ramOrRtcSelect >= 0x08 && ramOrRtcSelect <= 0x0C) {
            rtc.write(ramOrRtcSelect, value);
            markSaveDirty();
            return;
        }
        writeRam(ramBank, address, value);
    }

    @Override
    protected void loadExtraSaveData(DataInputStream input) throws IOException {
        rtc.load(input);
    }

    @Override
    protected void writeExtraSaveData(DataOutputStream output) throws IOException {
        rtc.save(output);
    }

    private void latchRtc(int value) {
        if (latchValue == 0x00 && value == 0x01) {
            rtc.latch();
        }
        latchValue = value;
    }

    private static class Mbc3Rtc {
        private final LongSupplier currentEpochSeconds;
        private final int[] latched = new int[5];

        private long baseEpochSeconds;
        private int seconds;
        private int minutes;
        private int hours;
        private int days;
        private boolean halted;
        private boolean carry;

        private Mbc3Rtc(LongSupplier currentEpochSeconds) {
            this.currentEpochSeconds = currentEpochSeconds;
            this.baseEpochSeconds = currentEpochSeconds.getAsLong();
        }

        private byte read(int register) {
            return (byte) (latched[register - 0x08] & 0xFF);
        }

        private void write(int register, byte value) {
            updateLiveRegisters();
            int unsigned = value & 0xFF;
            switch (register) {
                case 0x08 -> seconds = unsigned % 60;
                case 0x09 -> minutes = unsigned % 60;
                case 0x0A -> hours = unsigned % 24;
                case 0x0B -> days = (days & 0x100) | unsigned;
                case 0x0C -> {
                    days = (days & 0xFF) | ((unsigned & 0x01) << 8);
                    halted = (unsigned & 0x40) != 0;
                    carry = (unsigned & 0x80) != 0;
                }
                default -> {
                }
            }
            baseEpochSeconds = currentEpochSeconds.getAsLong();
            latch();
        }

        private void latch() {
            updateLiveRegisters();
            latched[0] = seconds;
            latched[1] = minutes;
            latched[2] = hours;
            latched[3] = days & 0xFF;
            latched[4] = ((days >> 8) & 0x01) | (halted ? 0x40 : 0) | (carry ? 0x80 : 0);
        }

        private void updateLiveRegisters() {
            if (halted) {
                return;
            }
            long now = currentEpochSeconds.getAsLong();
            long elapsed = Math.max(0, now - baseEpochSeconds);
            if (elapsed == 0) {
                return;
            }

            long totalSeconds = seconds + (minutes * 60L) + (hours * 3600L) + (days * 86_400L) + elapsed;
            long totalDays = totalSeconds / 86_400L;
            if (totalDays > 511) {
                carry = true;
            }
            days = (int) (totalDays & 0x1FF);
            int daySeconds = (int) (totalSeconds % 86_400L);
            hours = daySeconds / 3600;
            daySeconds %= 3600;
            minutes = daySeconds / 60;
            seconds = daySeconds % 60;
            baseEpochSeconds = now;
        }

        private void save(DataOutputStream output) throws IOException {
            updateLiveRegisters();
            output.writeInt(seconds);
            output.writeInt(minutes);
            output.writeInt(hours);
            output.writeInt(days);
            output.writeBoolean(halted);
            output.writeBoolean(carry);
            output.writeLong(baseEpochSeconds);
            for (int value : latched) {
                output.writeInt(value);
            }
        }

        private void load(DataInputStream input) throws IOException {
            seconds = input.readInt();
            minutes = input.readInt();
            hours = input.readInt();
            days = input.readInt();
            halted = input.readBoolean();
            carry = input.readBoolean();
            baseEpochSeconds = input.readLong();
            Arrays.setAll(latched, i -> {
                try {
                    return input.readInt();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
