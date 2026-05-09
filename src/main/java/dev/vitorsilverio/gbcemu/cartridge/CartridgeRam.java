package dev.vitorsilverio.gbcemu.cartridge;

import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.Arrays;

public class CartridgeRam implements MemorySpace, MemoryBank, Stateful<CartridgeRamState> {

    private static final int BASE_ADDRESS = 0xA000;
    private static final int END_ADDRESS = 0xC000;
    private static final int BANK_SIZE = 0x2000;

    private final byte[] data;
    private int currentBank;

    public CartridgeRam(int size) {
        this.data = new byte[Math.max(0, size)];
    }

    @Override
    public boolean contains(int address) {
        return address >= BASE_ADDRESS && address < END_ADDRESS;
    }

    @Override
    public byte read(int address) {
        return read(currentBank, address);
    }

    public byte read(int bank, int address) {
        if (data.length == 0) {
            return (byte) 0xFF;
        }
        currentBank = normalizeBank(bank);
        return data[ramAddress(currentBank, address)];
    }

    @Override
    public void write(int address, byte value) {
        write(currentBank, address, value);
    }

    public void write(int bank, int address, byte value) {
        if (data.length == 0) {
            return;
        }
        currentBank = normalizeBank(bank);
        data[ramAddress(currentBank, address)] = value;
    }

    public byte[] copyData() {
        return data.clone();
    }

    public void restoreData(byte[] source) {
        Arrays.fill(data, (byte) 0);
        System.arraycopy(source, 0, data, 0, Math.min(source.length, data.length));
    }

    public int size() {
        return data.length;
    }

    @Override
    public CartridgeRamState saveState() {
        return new CartridgeRamState(copyData(), currentBank);
    }

    @Override
    public void loadState(CartridgeRamState state) {
        restoreData(state.data());
        currentBank = normalizeBank(state.currentBank());
    }

    @Override
    public String bankName() {
        return "Cartridge RAM";
    }

    @Override
    public int bankCount() {
        return data.length == 0 ? 0 : Math.max(1, (data.length + BANK_SIZE - 1) / BANK_SIZE);
    }

    @Override
    public int bankSize() {
        return BANK_SIZE;
    }

    @Override
    public int currentBank() {
        return currentBank;
    }

    @Override
    public byte readBank(int bank, int offset) {
        if (data.length == 0) {
            return (byte) 0xFF;
        }
        return data[bankOffset(bank, offset)];
    }

    @Override
    public void writeBank(int bank, int offset, byte value) {
        if (data.length == 0) {
            return;
        }
        data[bankOffset(bank, offset)] = value;
    }

    private int ramAddress(int bank, int address) {
        return bankOffset(bank, address - BASE_ADDRESS);
    }

    private int bankOffset(int bank, int offset) {
        int normalizedBank = normalizeBank(bank);
        int normalizedOffset = Math.floorMod(offset, BANK_SIZE);
        return Math.floorMod((normalizedBank * BANK_SIZE) + normalizedOffset, data.length);
    }

    private int normalizeBank(int bank) {
        int count = bankCount();
        return count == 0 ? 0 : Math.floorMod(bank, count);
    }
}
