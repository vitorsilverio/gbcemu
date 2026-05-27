package dev.vitorsilverio.gbcemu.gui;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record EmulatorMenuActions(
        Runnable openRom,
        Supplier<List<File>> recentRoms,
        Consumer<File> openRecentRom,
        Runnable clearRecentRoms,
        Runnable openLinkedSession,
        Runnable openSettings,
        Runnable pause,
        Runnable resume,
        Runnable stop,
        Runnable restart,
        Consumer<Integer> openDetachedDisplay,
        Runnable addSecondConsole,
        Consumer<Integer> stopConsole,
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
