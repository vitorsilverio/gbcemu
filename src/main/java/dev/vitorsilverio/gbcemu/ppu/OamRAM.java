package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;

public class OamRAM implements MemorySpace {

    private final ObjectAtribute[] objectAtributes = new ObjectAtribute[40];

    public OamRAM() {
        for (int i = 0; i < objectAtributes.length; i++) {
            objectAtributes[i] = new ObjectAtribute();
        }
    }

    @Override
    public boolean contains(int address) {
        return address >= 0xFE00 && address < 0xFEA0;
    }

    @Override
    public byte read(int address) {
        address -= 0xFE00;
        int index = address / 4;
        return switch (address % 4) {
            case 0 -> objectAtributes[index].getY();
            case 1 -> objectAtributes[index].getX();
            case 2 -> objectAtributes[index].getTileIndex();
            case 3 -> objectAtributes[index].getAttributes();
            default -> throw new IllegalArgumentException("Invalid address: " + address);
        };
    }

    @Override
    public void write(int address, byte value) {
        address -= 0xFE00;
        int index = address / 4;
        switch (index % 4) {
            case 0:
                objectAtributes[index].setY(value);
                break;
            case 1:
                objectAtributes[index].setX(value);
                break;
            case 2:
                objectAtributes[index].setTileIndex(value);
                break;
            case 3:
                objectAtributes[index].setAttributes(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid address: " + address);
        }
    }
}
