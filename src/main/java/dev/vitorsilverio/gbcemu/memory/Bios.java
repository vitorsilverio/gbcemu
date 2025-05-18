package dev.vitorsilverio.gbcemu.memory;

import java.io.File;

public class Bios implements MemorySpace {

    private boolean enabled = true;

    private final byte[] bios;

    public Bios(File biosFile) {
        try(var inputStream = biosFile.toURI().toURL().openStream()) {
            this.bios = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BIOS", e);
        }
    }

    @Override
    public boolean contains(int address) {
        if ((address < 0x900 && !(address >= 0x100 && address < 0x200)) || address == 0xFF50) {
            return enabled;
        }
        return false;
    }

    @Override
    public byte read(int address) {
        return bios[address];
    }

    @Override
    public void write(int address, byte value) {
        if (address == 0xFF50) {
            enabled = false;
        }
    }
}
