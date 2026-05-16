package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.io.File;

public class Bios implements MemorySpace, Stateful<BiosState> {

    private boolean enabled = true;

    private final byte[] bios;
    private final Runnable onDisabled;

    public Bios(File biosFile) {
        this(biosFile, () -> {
        });
    }

    public Bios(File biosFile, Runnable onDisabled) {
        this.onDisabled = onDisabled;
        try(var inputStream = biosFile.toURI().toURL().openStream()) {
            this.bios = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BIOS", e);
        }
    }

    @Override
    public BiosState saveState() {
        return new BiosState(enabled);
    }

    @Override
    public void loadState(BiosState state) {
        enabled = state.enabled();
    }

    @Override
    public boolean contains(int address) {
        if (address == 0xFF50) {
            return true;
        }
        if (address < 0x900 && !(address >= 0x100 && address < 0x200)) {
            return enabled;
        }
        return false;
    }

    @Override
    public byte read(int address) {
        if (address == 0xFF50) {
            return (byte) (enabled ? 0 : 1);
        }
        return bios[address];
    }

    @Override
    public void write(int address, byte value) {
        if (address == 0xFF50 && enabled && value != 0) {
            enabled = false;
            onDisabled.run();
        }
    }
}
