package dev.vitorsilverio.gbcemu.ppu;

import java.io.Serializable;

public record PpuState(
        byte control,
        byte stat,
        byte[] bgPalette,
        byte bgPaletteIndex,
        byte[] objPalette,
        byte objPaletteIndex,
        byte bgPaletteDmg,
        byte obj0PaletteDmg,
        byte obj1PaletteDmg,
        int[][] frameBuffer,
        int[][] bgColorIndexes,
        boolean[][] bgPriorities,
        boolean cgbMode,
        int cycles,
        PpuMode mode,
        int currentLine,
        int currentColumn,
        int scrollX,
        int scrollY,
        int penaltyDelay,
        int hBlankCycles,
        ObjectPriorityMode objectPriorityMode,
        byte lineCompare,
        int windowX,
        int windowY,
        boolean previousStatSignal,
        boolean frameReady,
        boolean windowYCondition,
        int windowLineCounter,
        boolean windowStartedOnLine,
        int spriteCandidateCount
) implements Serializable {
}
