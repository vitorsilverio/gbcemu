package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import io.github.stanio.xbrz.awt.AwtXbrz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

public class EmulatorWindow {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 144;
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(EmulatorWindow.class);
    private static final String WINDOW_X = "mainWindowX";
    private static final String WINDOW_Y = "mainWindowY";
    private static final String WINDOW_CASCADE = "mainWindowCascade";
    private static final int WINDOW_CASCADE_STEP = 32;
    private static final int WINDOW_CASCADE_SLOTS = 8;

    private final JFrame window = new JFrame("GBC EMU");
    private final JPanel screen;
    private final Timer repaintTimer;
    private final Timer overlayTimer;
    private Timer rewindHoldTimer;
    private int scale;
    private boolean smoothScaling;
    private boolean xbrzFiltering;
    private boolean fullscreen;
    private Ppu ppu;
    private KeyListener keyListener;
    private OverlayIcon overlayIcon;
    private boolean windowLocationInitialized;

    public EmulatorWindow(EmulatorMenuActions menuActions, AppSettings settings) {
        int scale = settings.screenScale();
        this.scale = Math.max(1, Math.min(8, scale));
        this.smoothScaling = settings.smoothScaling();
        this.xbrzFiltering = settings.xBrzFiltering();
        this.fullscreen = settings.fullscreen();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocationByPlatform(true);
        screen = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame(g);
            }
        };
        repaintTimer = new Timer(16, event -> screen.repaint());
        overlayTimer = new Timer(900, event -> {
            overlayIcon = null;
            screen.repaint();
        });
        overlayTimer.setRepeats(false);
        screen.setFocusable(true);
        screen.setPreferredSize(new Dimension(WIDTH * this.scale, HEIGHT * this.scale));
        window.setContentPane(screen);
        window.setResizable(false);
        installMenu(menuActions);
        installRewindHoldKey(menuActions);
        installWindowLifecycle(menuActions);
        installWindowPositionPersistence();
        applyWindowMode();
    }

    public void show() {
        window.setVisible(true);
    }

    public Frame owner() {
        return window;
    }

    public void setScale(int scale) {
        this.scale = Math.max(1, Math.min(8, scale));
        SwingUtilities.invokeLater(() -> {
            screen.setPreferredSize(new Dimension(WIDTH * this.scale, HEIGHT * this.scale));
            applyWindowMode();
            screen.repaint();
        });
    }

    public void applySettings(AppSettings settings) {
        this.scale = Math.max(1, Math.min(8, settings.screenScale()));
        this.smoothScaling = settings.smoothScaling();
        this.xbrzFiltering = settings.xBrzFiltering();
        this.fullscreen = settings.fullscreen();
        SwingUtilities.invokeLater(() -> {
            screen.setPreferredSize(new Dimension(WIDTH * this.scale, HEIGHT * this.scale));
            applyWindowMode();
            screen.repaint();
        });
    }

    private void applyWindowMode() {
        boolean visible = window.isVisible();
        if (visible) {
            window.dispose();
        }
        window.setUndecorated(fullscreen);
        window.setResizable(fullscreen);
        if (fullscreen) {
            window.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            window.setExtendedState(JFrame.NORMAL);
            window.pack();
            restoreWindowLocationIfNeeded();
        }
        if (visible) {
            window.setVisible(true);
        }
    }

    public void attach(Ppu ppu, KeyListener keyListener) {
        this.ppu = ppu;
        SwingUtilities.invokeLater(() -> {
            if (this.ppu != ppu) {
                return;
            }
            detachKeyListener();
            this.keyListener = keyListener;
            if (keyListener != null) {
                screen.addKeyListener(keyListener);
            }
            screen.repaint();
        });
    }

    public void detach(Ppu expectedPpu) {
        if (expectedPpu != null && ppu != expectedPpu) {
            return;
        }
        ppu = null;
        SwingUtilities.invokeLater(() -> {
            if (expectedPpu != null && ppu != null && ppu != expectedPpu) {
                return;
            }
            detachKeyListener();
            repaintTimer.stop();
            rewindHoldTimer.stop();
            screen.repaint();
        });
    }

    public void renderFrame(Ppu source) {
        if (source == null || source != ppu) {
            return;
        }
        SwingUtilities.invokeLater(screen::repaint);
    }

    public void showOverlay(OverlayIcon icon) {
        SwingUtilities.invokeLater(() -> {
            overlayIcon = icon;
            overlayTimer.restart();
            screen.repaint();
        });
    }

    public void updatePerformanceStats(double fps, double speedPercent) {
        SwingUtilities.invokeLater(() ->
                window.setTitle(String.format("GBC EMU - %.1f FPS (%.0f%%)", fps, speedPercent))
        );
    }

    public void resetTitle() {
        SwingUtilities.invokeLater(() -> window.setTitle("GBC EMU"));
    }

    private void detachKeyListener() {
        if (keyListener != null) {
            screen.removeKeyListener(keyListener);
            keyListener = null;
        }
    }


    private void installMenu(EmulatorMenuActions menuActions) {
        JMenuBar menuBar = new JMenuBar();
        JMenu emulatorMenu = new JMenu("Emulator");

        JMenuItem openRom = new JMenuItem("Start ROM...");
        openRom.addActionListener(event -> menuActions.openRom().run());
        openRom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        emulatorMenu.add(openRom);

        JMenuItem pause = new JMenuItem("Pause");
        pause.addActionListener(event -> menuActions.pause().run());
        pause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        emulatorMenu.add(pause);

        JMenuItem resume = new JMenuItem("Resume");
        resume.addActionListener(event -> menuActions.resume().run());
        resume.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        emulatorMenu.add(resume);

        JMenuItem stop = new JMenuItem("Stop");
        stop.addActionListener(event -> menuActions.stop().run());
        emulatorMenu.add(stop);

        JMenuItem restart = new JMenuItem("Restart");
        restart.addActionListener(event -> menuActions.restart().run());
        restart.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        emulatorMenu.add(restart);

        emulatorMenu.addSeparator();

        JMenuItem settings = new JMenuItem("Settings...");
        settings.addActionListener(event -> menuActions.openSettings().run());
        emulatorMenu.add(settings);

        menuBar.add(emulatorMenu);

        JMenu debugMenu = new JMenu("Debug");
        JMenuItem openAudioDebugger = new JMenuItem("Audio channels...");
        openAudioDebugger.addActionListener(event -> menuActions.audioDebugger().run());
        debugMenu.add(openAudioDebugger);

        JMenuItem openMemoryDebugger = new JMenuItem("Memory...");
        openMemoryDebugger.addActionListener(event -> menuActions.memoryDebugger().run());
        debugMenu.add(openMemoryDebugger);

        JMenuItem openPpuDebugger = new JMenuItem("PPU / Tiles...");
        openPpuDebugger.addActionListener(event -> menuActions.ppuDebugger().run());
        debugMenu.add(openPpuDebugger);

        JMenuItem openCpuDebugger = new JMenuItem("CPU / Disassembly...");
        openCpuDebugger.addActionListener(event -> menuActions.cpuDebugger().run());
        debugMenu.add(openCpuDebugger);

        JMenuItem openCartDebugger = new JMenuItem("Cart / MBC...");
        openCartDebugger.addActionListener(event -> menuActions.cartDebugger().run());
        debugMenu.add(openCartDebugger);

        debugMenu.addSeparator();

        JMenuItem dumpDebugBundle = new JMenuItem("Dump debug bundle");
        dumpDebugBundle.addActionListener(event -> menuActions.dumpDebugBundle().run());
        dumpDebugBundle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        debugMenu.add(dumpDebugBundle);

        JMenuItem dumpMemoryBanks = new JMenuItem("Dump memory banks");
        dumpMemoryBanks.addActionListener(event -> menuActions.dumpMemoryBanks().run());
        debugMenu.add(dumpMemoryBanks);
        menuBar.add(debugMenu);

        JMenu snapshotMenu = new JMenu("Save states");
        JMenuItem saveSnapshot = new JMenuItem("Save slot 0");
        saveSnapshot.addActionListener(event -> menuActions.saveSnapshoot().run());
        saveSnapshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        snapshotMenu.add(saveSnapshot);

        JMenuItem restoreSnapshot = new JMenuItem("Load slot 0");
        restoreSnapshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        restoreSnapshot.addActionListener(event -> menuActions.restoreSnapshot().run());
        snapshotMenu.add(restoreSnapshot);

        snapshotMenu.addSeparator();

        JMenuItem rewindSnapshot = new JMenuItem("Rewind one snapshot");
        rewindSnapshot.addActionListener(event -> menuActions.rewindSnapshot().run());
        snapshotMenu.add(rewindSnapshot);

        snapshotMenu.addSeparator();

        JMenuItem manageSnapshot = new JMenuItem("Manage save states...");
        manageSnapshot.addActionListener(event -> menuActions.manageSnapshots().run());
        snapshotMenu.add(manageSnapshot);

        menuBar.add(snapshotMenu);

        JMenu cheatMenu = new JMenu("Cheats");
        JMenuItem openCheatsMenu = new JMenuItem("Open cheats");
        openCheatsMenu.addActionListener(event -> menuActions.cheats().run());
        cheatMenu.add(openCheatsMenu);
        menuBar.add(cheatMenu);

        JMenu multiplayerMenu = new JMenu("Multiplayer");
        JMenuItem openMultiplayerMenu = new JMenuItem("Configure multiplayer");
        openMultiplayerMenu.addActionListener(event -> menuActions.multiplayer().run());
        multiplayerMenu.add(openMultiplayerMenu);
        menuBar.add(multiplayerMenu);

        window.setJMenuBar(menuBar);



    }

    private void installRewindHoldKey(EmulatorMenuActions menuActions) {
        rewindHoldTimer = new Timer(90, event -> menuActions.rewindSnapshotSilent().run());
        rewindHoldTimer.setRepeats(true);
        InputMap inputMap = screen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = screen.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0, false), "rewind-pressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0, true), "rewind-released");
        actionMap.put("rewind-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (!rewindHoldTimer.isRunning()) {
                    menuActions.rewindSnapshotSilent().run();
                    rewindHoldTimer.start();
                }
            }
        });
        actionMap.put("rewind-released", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                rewindHoldTimer.stop();
            }
        });
    }

    private void installWindowLifecycle(EmulatorMenuActions menuActions) {
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveWindowLocation();
                rewindHoldTimer.stop();
                menuActions.stop().run();
            }

            @Override
            public void windowDeactivated(WindowEvent event) {
                rewindHoldTimer.stop();
            }
        });
    }

    private void installWindowPositionPersistence() {
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                saveWindowLocation();
            }
        });
    }

    private void restoreWindowLocationIfNeeded() {
        if (windowLocationInitialized) {
            return;
        }
        windowLocationInitialized = true;
        int x = PREFERENCES.getInt(WINDOW_X, Integer.MIN_VALUE);
        int y = PREFERENCES.getInt(WINDOW_Y, Integer.MIN_VALUE);
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
            window.setLocationByPlatform(true);
            return;
        }

        int cascade = PREFERENCES.getInt(WINDOW_CASCADE, 0);
        PREFERENCES.putInt(WINDOW_CASCADE, (cascade + 1) % WINDOW_CASCADE_SLOTS);
        Rectangle bounds = new Rectangle(
                x + cascade * WINDOW_CASCADE_STEP,
                y + cascade * WINDOW_CASCADE_STEP,
                window.getWidth(),
                window.getHeight()
        );
        window.setLocation(clampToScreen(bounds).getLocation());
    }

    private void saveWindowLocation() {
        if (fullscreen || !window.isShowing()) {
            return;
        }
        Point location = window.getLocation();
        PREFERENCES.putInt(WINDOW_X, location.x);
        PREFERENCES.putInt(WINDOW_Y, location.y);
    }

    private Rectangle clampToScreen(Rectangle bounds) {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int maxX = Math.max(screenBounds.x, screenBounds.x + screenBounds.width - bounds.width);
        int maxY = Math.max(screenBounds.y, screenBounds.y + screenBounds.height - bounds.height);
        int x = Math.max(screenBounds.x, Math.min(bounds.x, maxX));
        int y = Math.max(screenBounds.y, Math.min(bounds.y, maxY));
        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    private void drawFrame(Graphics g) {
        if(ppu!=null) {
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    smoothScaling ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );
            Image frame = ppu.getFrameBuffer();
            int drawWidth = fullscreen ? screen.getWidth() : WIDTH * scale;
            int drawHeight = fullscreen ? screen.getHeight() : HEIGHT * scale;
            if (xbrzFiltering) {
                frame = AwtXbrz.scaleImage(frame, scale);
            }
            graphics.drawImage(frame, 0, 0, drawWidth, drawHeight, null);
        }
        drawOverlay(g);
    }

    private void drawOverlay(Graphics g) {
        if (overlayIcon == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = overlayIcon == OverlayIcon.PAUSE
                    || overlayIcon == OverlayIcon.SAVE
                    || overlayIcon == OverlayIcon.LOAD ? 96 : 88;
            int x = screen.getWidth() - width - 16;
            int y = 14;
            int height = 42;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(x, y, width, height, 8, 8);
            g2.setColor(new Color(255, 255, 255, 230));
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawOverlaySymbol(g2, overlayIcon, x + 16, y + 10);
            g2.setColor(new Color(255, 255, 255, 110));
            g2.drawLine(x + 44, y + 8, x + 44, y + height - 8);
            g2.setColor(new Color(255, 255, 255, 230));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString(overlayIcon.label(), x + 54, y + 27);
        } finally {
            g2.dispose();
        }
    }

    private void drawOverlaySymbol(Graphics2D g2, OverlayIcon icon, int x, int y) {
        switch (icon) {
            case PLAY -> {
                Polygon triangle = new Polygon();
                triangle.addPoint(x, y);
                triangle.addPoint(x, y + 22);
                triangle.addPoint(x + 18, y + 11);
                g2.fillPolygon(triangle);
            }
            case PAUSE -> {
                g2.fillRect(x, y, 6, 22);
                g2.fillRect(x + 12, y, 6, 22);
            }
            case STOP -> g2.fillRect(x, y + 2, 18, 18);
            case REWIND -> {
                Polygon left = new Polygon();
                left.addPoint(x + 8, y);
                left.addPoint(x + 8, y + 22);
                left.addPoint(x - 6, y + 11);
                g2.fillPolygon(left);
                Polygon right = new Polygon();
                right.addPoint(x + 22, y);
                right.addPoint(x + 22, y + 22);
                right.addPoint(x + 8, y + 11);
                g2.fillPolygon(right);
            }
            case SAVE -> {
                g2.drawRect(x - 1, y + 1, 20, 18);
                g2.drawLine(x + 3, y + 6, x + 15, y + 6);
                g2.drawLine(x + 4, y + 15, x + 14, y + 15);
            }
            case LOAD -> {
                g2.drawRect(x - 1, y + 1, 20, 18);
                g2.drawLine(x + 9, y + 5, x + 9, y + 16);
                g2.drawLine(x + 4, y + 11, x + 9, y + 16);
                g2.drawLine(x + 14, y + 11, x + 9, y + 16);
            }
        }
    }

    public enum OverlayIcon {
        PLAY("PLAY"),
        PAUSE("PAUSE"),
        STOP("STOP"),
        REWIND("REW"),
        SAVE("SAVE"),
        LOAD("LOAD");

        private final String label;

        OverlayIcon(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }
}
