package dev.vitorsilverio.gbcemu.gui;

public record EmulatorMenuActions(
        Runnable openRom,
        Runnable openLinkedSession,
        Runnable openSettings,
        Runnable pause,
        Runnable resume,
        Runnable stop,
        Runnable restart,
        Runnable saveSnapshoot,
        Runnable restoreSnapshot,
        Runnable rewindSnapshot,
        Runnable rewindSnapshotSilent,
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
