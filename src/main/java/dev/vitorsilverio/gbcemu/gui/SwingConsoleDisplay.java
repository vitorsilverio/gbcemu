package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.core.ConsoleDisplay;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;

import java.awt.event.KeyListener;

public final class SwingConsoleDisplay implements ConsoleDisplay {
    private final EmulatorWindow window;
    private final boolean secondary;
    private final KeyListener keyListener;

    private SwingConsoleDisplay(EmulatorWindow window, boolean secondary, KeyListener keyListener) {
        this.window = window;
        this.secondary = secondary;
        this.keyListener = keyListener;
    }

    public static SwingConsoleDisplay primary(EmulatorWindow window, KeyListener keyListener) {
        return new SwingConsoleDisplay(window, false, keyListener);
    }

    public static SwingConsoleDisplay secondary(EmulatorWindow window, KeyListener keyListener) {
        return new SwingConsoleDisplay(window, true, keyListener);
    }

    public static SwingConsoleDisplay detached(String title, AppSettings settings) {
        return primary(EmulatorWindow.detachedDisplay(title, settings), null);
    }

    @Override
    public void attach(Ppu ppu, SuperGameBoy superGameBoy) {
        if (secondary) {
            window.attachSecondary(ppu, keyListener, superGameBoy);
        } else {
            window.attach(ppu, keyListener, superGameBoy);
        }
    }

    @Override
    public void detach(Ppu ppu) {
        window.detach(ppu);
    }

    @Override
    public void renderFrame(Ppu ppu) {
        window.renderFrame(ppu);
    }

    @Override
    public void updatePerformanceStats(double fps, double speedPercent) {
        window.updatePerformanceStats(fps, speedPercent);
    }

    @Override
    public void applySettings(AppSettings settings) {
        window.applySettings(settings);
    }

    @Override
    public void show() {
        window.show();
    }

    @Override
    public boolean isOpen() {
        return window.isOpen();
    }

    @Override
    public void dispose() {
        window.dispose();
    }
}
