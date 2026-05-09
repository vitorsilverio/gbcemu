package dev.vitorsilverio.gbcemu;

public record EmulatorMenuActions(
        Runnable openRom,
        Runnable configureDefaultBios,
        Runnable openDebugger,
        Runnable pause,
        Runnable resume,
        Runnable stop,
        Runnable saveSnapshoot,
        Runnable restoreSnapshot,
        Runnable rewindSnapshot,
        Runnable manageSnapshots,
        Runnable cheats
) {
}
