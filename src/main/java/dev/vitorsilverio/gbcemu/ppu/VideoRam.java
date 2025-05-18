package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.memory.MemorySpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VideoRam implements MemorySpace {

    private static final int VBK = 0xFF4F;

    private final Tile[][] tiles = new Tile[2][384];
    private final TileMap[] tileMaps = new TileMap[256 * 256];
    private int bank = 0;

    public VideoRam() {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                tiles[i][j] = new Tile();
            }
        }
        for (int i = 0; i < tileMaps.length; i++) {
            tileMaps[i] = new TileMap();
        }
    }

    @Override
    public boolean contains(int address) {
        return address == VBK || 0x8000 <= address && address <= 0x9FFF;
    }

    @Override
    public byte read(int address) {
        if (address == VBK) {
            return (byte) bank;
        }


        if (address <= 0x97ff) { // Tile Data
            address -= 0x8000;
            return tiles[bank][address / 16].getData(address % 16);
        }
        // Tile Map
        address -= 0x9800;
        if (bank == 0) {
            return tileMaps[address].getIndex();
        } else {
            return tileMaps[address].getAttributes();
        }
    }

    @Override
    public void write(int address, byte value) {
        address&= 0xFFFF;
        if (address == VBK) {
            bank = value & 0x01;
            return;
        }

        if (address <= 0x97ff) { // Tile Data
            address -= 0x8000;
            tiles[bank][(address) / 16].setData(address % 16, value);
            return;
        }
        // Tile Map
        address -= 0x9800;
        if (bank == 0) {
            tileMaps[address].setIndex(value);
        } else {
            tileMaps[address].setAttributes(value);
        }
    }

    public Tile getTile(TileArea tileArea, int bank, int tileIndex) {
        int index = (tileArea.getBaseAddress() - 0x8000) / 16;
        if (tileArea.isSigned()) {
            index  = index + ((byte)tileIndex);
        } else {
            index  = index + tileIndex;
        }
        return tiles[bank][index];
    }

    public TileMap getTileMap(TileMapArea TileMapArea, int tileIndex) {
        int index = (TileMapArea.getAddress() - 0x9800) & 0xFFFF;
        return tileMaps[index + tileIndex];
    }

    @Override
    public String toString() {
        var map = Map.of(
                0, "█",
                1, "▓",
                2, "▒",
                3, "░"
        );
        StringBuilder sb = new StringBuilder();
        // print all tiles in a 32x24 grid
        for(int b= 0; b < 2; b++) {
            for (int y = 0; y < 48; y++) {
                List<Tile> row = new ArrayList<>();
                for (int x = 0; x < 8; x++) {
                    int tileIndex = y * 8 + x;
                    Tile tile = tiles[b][tileIndex];
                    row.add(tile);
                }
                List<String[]> list = row.stream().map(t -> t.toString().split("\n")).toList();
                for (int i = 0; i < 8; i++) {
                    for (String[] strings : list) {
                        sb.append(strings[i]);
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }
}
