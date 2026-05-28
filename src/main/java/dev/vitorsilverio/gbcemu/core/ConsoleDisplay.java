package dev.vitorsilverio.gbcemu.core;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;

public interface ConsoleDisplay {
    void attach(Ppu ppu, SuperGameBoy superGameBoy);

    void detach(Ppu ppu);

    void renderFrame(Ppu ppu);

    void updatePerformanceStats(double fps, double speedPercent);

    void applySettings(AppSettings settings);

    void show();

    boolean isOpen();

    void dispose();
}
