package dev.vitorsilverio.gbcemu.cartridge;

import java.io.Serializable;
import java.util.Map;

public record CartState(
        CartridgeRamState externalRam,
        Map<String, Object> mapperState
) implements Serializable {
}
