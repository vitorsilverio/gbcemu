package dev.vitorsilverio.gbcemu.memory;

import java.io.Serializable;

public record BiosState(boolean enabled) implements Serializable {
}
