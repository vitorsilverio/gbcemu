package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.cartridge.CartHeader;
import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import java.util.List;
import java.util.Map;

public class CartDebugInterface implements DebugCartInterface {
    private final Cart cart;

    public CartDebugInterface(Cart cart) {
        this.cart = cart;
    }

    @Override
    public CartHeader header() {
        return cart.getHeader();
    }

    @Override
    public CartState state() {
        return cart.saveState();
    }

    @Override
    public Map<String, String> properties() {
        return cart.debugProperties();
    }

    @Override
    public List<MemoryBank> memoryBanks() {
        return cart.memoryBanks();
    }
}
