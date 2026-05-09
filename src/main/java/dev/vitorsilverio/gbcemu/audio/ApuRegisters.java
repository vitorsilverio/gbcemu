package dev.vitorsilverio.gbcemu.audio;

final class ApuRegisters {
    private static final int REGISTER_START = 0xFF10;
    private static final int REGISTER_END = 0xFF3F;
    private static final int WAVE_RAM_START = 0xFF30;
    private static final int WAVE_RAM_END = 0xFF3F;

    private final byte[] registers = new byte[0x30];
    private final byte[] wavePatternRam = new byte[0x10];

    ApuRegisters() {
        setPowerOnDefaults();
    }

    boolean contains(int address) {
        return address >= REGISTER_START && address <= REGISTER_END;
    }

    boolean isUnusedRegister(int address) {
        return address >= 0xFF27 && address <= 0xFF2F;
    }

    boolean isWaveRam(int address) {
        return address >= WAVE_RAM_START && address <= WAVE_RAM_END;
    }

    byte read(int address) {
        return registers[index(address)];
    }

    void write(int address, byte value) {
        registers[index(address)] = value;
    }

    byte readWaveRamAddress(int address) {
        return wavePatternRam[address - WAVE_RAM_START];
    }

    void writeWaveRamAddress(int address, byte value) {
        wavePatternRam[address - WAVE_RAM_START] = value;
    }

    byte readWaveRamOffset(int offset) {
        return wavePatternRam[offset & 0x0F];
    }

    int period(int lowAddress, int highAddress) {
        return (read(lowAddress) & 0xFF) | ((read(highAddress) & 0x07) << 8);
    }

    byte[] copyRegisters() {
        return registers.clone();
    }

    byte[] copyWavePatternRam() {
        return wavePatternRam.clone();
    }

    void loadRegisters(byte[] source) {
        System.arraycopy(source, 0, registers, 0, Math.min(registers.length, source.length));
    }

    void loadWavePatternRam(byte[] source) {
        System.arraycopy(source, 0, wavePatternRam, 0, Math.min(wavePatternRam.length, source.length));
    }

    void clearRegisters() {
        java.util.Arrays.fill(registers, (byte) 0);
    }

    private void setPowerOnDefaults() {
        write(ApuAddress.NR50_MASTER_VOLUME, (byte) 0x77);
        write(ApuAddress.NR51_SOUND_PANNING, (byte) 0xFF);
        write(ApuAddress.NR52_AUDIO_MASTER_CONTROL, (byte) 0x80);
    }

    static int index(int address) {
        return address - REGISTER_START;
    }
}
