package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisassemblyCache {

    private final DebugMemoryInterface memory;
    private final Map<Key, Disassembler.Decoded> decoded = new HashMap<>();

    public DisassemblyCache(DebugMemoryInterface memory) {
        this.memory = memory;
    }

    Disassembler.Decoded decode(int address) {
        Key key = key(address);
        Disassembler.Decoded cached = decoded.get(key);
        if (cached != null && bytesMatch(cached)) {
            return cached;
        }
        Disassembler.Decoded fresh = Disassembler.decode(address, valueAddress -> memory.read(valueAddress) & 0xFF);
        decoded.put(key, fresh);
        return fresh;
    }

    private boolean bytesMatch(Disassembler.Decoded cached) {
        StringBuilder current = new StringBuilder();
        for (int offset = 0; offset < cached.length(); offset++) {
            if (offset > 0) {
                current.append(' ');
            }
            current.append(String.format("%02X", memory.read(cached.address() + offset) & 0xFF));
        }
        return cached.bytes().contentEquals(current);
    }

    public List<Disassembler.Decoded> previousInstructions(int pc, int limit) {
        Key pcKey = key(pc);
        List<Disassembler.Decoded> values = new ArrayList<>();
        decoded.entrySet().stream()
                .filter(entry -> entry.getKey().space.equals(pcKey.space))
                .filter(entry -> entry.getKey().bank == pcKey.bank)
                .filter(entry -> entry.getKey().address < pc)
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Key::address).reversed()))
                .limit(limit)
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingInt(Disassembler.Decoded::address))
                .forEach(values::add);
        return values;
    }

    public List<Disassembler.Decoded> decodeForward(int address, int count) {
        List<Disassembler.Decoded> values = new ArrayList<>();
        int cursor = address;
        for (int i = 0; i < count; i++) {
            Disassembler.Decoded value = decode(cursor);
            values.add(value);
            cursor = (cursor + value.length()) & 0xFFFF;
        }
        return values;
    }

    public String location(int address) {
        Key key = key(address);
        return key.bank < 0 ? key.space : key.space + ":" + key.bank;
    }

    private Key key(int address) {
        address &= 0xFFFF;
        if (address < 0x4000) {
            return new Key("Cartridge ROM", 0, address);
        }
        if (address < 0x8000) {
            return new Key("Cartridge ROM", currentBank("Cartridge ROM"), address);
        }
        if (address >= 0x8000 && address < 0xA000) {
            return new Key("VRAM", currentBank("VRAM"), address);
        }
        if (address >= 0xC000 && address < 0xD000) {
            return new Key("WRAM", 0, address);
        }
        if (address >= 0xD000 && address < 0xE000) {
            return new Key("WRAM", currentBank("WRAM"), address);
        }
        return new Key("BUS", -1, address);
    }

    private int currentBank(String name) {
        for (MemoryBank bank : memory.memoryBanks()) {
            if (bank.bankName().equals(name)) {
                return bank.currentBank();
            }
        }
        return 0;
    }

    private record Key(String space, int bank, int address) {
    }
}
