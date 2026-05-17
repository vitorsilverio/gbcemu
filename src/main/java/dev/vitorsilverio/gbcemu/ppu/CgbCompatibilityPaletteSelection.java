package dev.vitorsilverio.gbcemu.ppu;

public record CgbCompatibilityPaletteSelection(
        int paletteId,
        int paletteGroup,
        int obj0PaletteWordOffset,
        int obj1PaletteWordOffset,
        int bgPaletteWordOffset,
        boolean logoTilemapRequired
) {
}
