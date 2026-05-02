package dev.vitorsilverio.gbcemu;

public record EmulatorMenuActions(
        Runnable openRom,
        Runnable configureDefaultBios
) {
}
