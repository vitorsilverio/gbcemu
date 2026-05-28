package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.cartridge.CartHeader;
import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import java.util.List;
import java.util.Map;

public interface DebugCartInterface {
    CartHeader header();

    CartState state();

    Map<String, String> properties();

    List<MemoryBank> memoryBanks();
}
