package dev.vitorsilverio.gbcemu.memory;

import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class WorkRam implements MemorySpace, MemoryBank, Stateful<WorkRamState> {

    private final int SVBK_REGISTER = 0xFF70;
    private int bank = 0;
    private final byte[] bank0 = new byte[0x2000];
    private final byte[][] banks = new byte[7][0x2000];

    @Override
    public WorkRamState saveState() {
        return new WorkRamState(bank, bank0.clone(), cloneBanks());
    }

    @Override
    public void loadState(WorkRamState state) {
        bank = state.bank() & 0x07;
        System.arraycopy(state.bank0(), 0, bank0, 0, Math.min(bank0.length, state.bank0().length));
        byte[][] stateBanks = state.banks();
        for (int i = 0; i < banks.length && i < stateBanks.length; i++) {
            System.arraycopy(stateBanks[i], 0, banks[i], 0, Math.min(banks[i].length, stateBanks[i].length));
        }
    }

    private byte[][] cloneBanks() {
        byte[][] copy = new byte[banks.length][];
        for (int i = 0; i < banks.length; i++) {
            copy[i] = banks[i].clone();
        }
        return copy;
    }

    @Override
    public boolean contains(int address) {
        return (address >= 0xC000 && address < 0xE000) || address == SVBK_REGISTER;
    }

    @Override
    public byte read(int address) {
        if (address == SVBK_REGISTER) {
            return (byte) (0xF8 | bank);
        }
        if (address < 0xD000) {
            return bank0[address - 0xC000];
        }
        return banks[getBank() - 1][address - 0xD000];
    }

    public int getBank() {
        return bank == 0 ? 1 : bank;
    }

    @Override
    public String bankName() {
        return "WRAM";
    }

    @Override
    public int bankCount() {
        return 8;
    }

    @Override
    public int bankSize() {
        return 0x1000;
    }

    @Override
    public int currentBank() {
        return getBank();
    }

    @Override
    public byte readBank(int bank, int offset) {
        offset = Math.floorMod(offset, bankSize());
        if (bank == 0) {
            return bank0[offset];
        }
        return banks[Math.floorMod(bank - 1, banks.length)][offset];
    }

    @Override
    public void writeBank(int bank, int offset, byte value) {
        offset = Math.floorMod(offset, bankSize());
        if (bank == 0) {
            bank0[offset] = value;
            return;
        }
        banks[Math.floorMod(bank - 1, banks.length)][offset] = value;
    }

    @Override
    public void write(int address, byte value) {
        if (address == SVBK_REGISTER) {
            bank = value & 0x07;
        } else if (address < 0xD000) {
            bank0[address - 0xC000] = value;
        } else {
            banks[getBank() - 1][address - 0xD000] = value;
        }
    }
}
