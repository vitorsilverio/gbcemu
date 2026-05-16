package dev.vitorsilverio.gbcemu.misc;

import java.io.Serializable;

public record CgbUndocumentedRegistersState(
        byte ff72,
        byte ff73,
        byte ff74,
        byte ff75
) implements Serializable {
}
