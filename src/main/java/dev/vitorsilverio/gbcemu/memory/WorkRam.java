package dev.vitorsilverio.gbcemu.memory;

public class WorkRam implements MemorySpace{

    private final int SVBK_REGISTER = 0xFF70;
    private int bank = 0;
    private final byte[] bank0 = new byte[0x2000];
    private final byte[][] banks = new byte[7][0x2000];

    @Override
    public boolean contains(int address) {
        return (address >= 0xC000 && address < 0xE000) || address == SVBK_REGISTER;
    }

    @Override
    public byte read(int address) {
        if (address == SVBK_REGISTER) {
            return (byte) bank;
        }
        if (address < 0xD000) {
            return bank0[address - 0xC000];
        }
        return banks[getBank()][address - 0xD000];
    }

    public int getBank() {
        if (bank == 0) {
            bank = 1;
        }
        return bank;
    }

    @Override
    public void write(int address, byte value) {
        if (address == SVBK_REGISTER) {
            bank = value & 0x07;
            if (bank == 0) {
                bank = 1;
            }
        } else if (address < 0xD000) {
            bank0[address - 0xC000] = value;
        } else {
            banks[bank - 1][address - 0xD000] = value;
        }
    }
}
