package dev.vitorsilverio.gbcemu.gui;

public record EmulatorMenuActions(
        Runnable openRom,
        Runnable configureDefaultBios,
        Runnable openSettings,
        Runnable pause,
        Runnable resume,
        Runnable stop,
        Runnable saveSnapshoot,
        Runnable restoreSnapshot,
        Runnable rewindSnapshot,
        Runnable manageSnapshots,
        Runnable cheats,
        Runnable audioDebugger,
        Runnable memoryDebugger,
        Runnable ppuDebugger,
        Runnable cpuDebugger,
        Runnable cartDebugger,
        Runnable dumpDebugBundle,
        Runnable dumpMemoryBanks
) {
}
