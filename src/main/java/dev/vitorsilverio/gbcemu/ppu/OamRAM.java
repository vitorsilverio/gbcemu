package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

public class OamRAM implements MemorySpace, Stateful<OamState> {

    private final ObjectAtribute[] objectAtributes = new ObjectAtribute[40];

    public OamRAM() {
        for (int i = 0; i < objectAtributes.length; i++) {
            objectAtributes[i] = new ObjectAtribute();
        }
    }

    @Override
    public OamState saveState() {
        byte[] data = new byte[objectAtributes.length * 4];
        for (int i = 0; i < objectAtributes.length; i++) {
            ObjectAtribute attribute = objectAtributes[i];
            int offset = i * 4;
            data[offset] = attribute.getY();
            data[offset + 1] = attribute.getX();
            data[offset + 2] = attribute.getTileIndex();
            data[offset + 3] = attribute.getAttributes();
        }
        return new OamState(data);
    }

    @Override
    public void loadState(OamState state) {
        byte[] data = state.data();
        for (int i = 0; i < objectAtributes.length && i * 4 + 3 < data.length; i++) {
            ObjectAtribute attribute = objectAtributes[i];
            int offset = i * 4;
            attribute.setY(data[offset]);
            attribute.setX(data[offset + 1]);
            attribute.setTileIndex(data[offset + 2]);
            attribute.setAttributes(data[offset + 3]);
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
        switch (address % 4) {
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

    public ObjectAtribute getObjectAtribute(int index) {
        if (index < 0 || index >= objectAtributes.length) {
            throw new IllegalArgumentException("Invalid object index: " + index);
        }
        return objectAtributes[index];
    }
}
