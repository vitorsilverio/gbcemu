package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;
import io.github.stanio.xbrz.awt.AwtXbrz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
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
    private Ppu secondaryPpu;
    private SuperGameBoy superGameBoy;
    private SuperGameBoy secondarySuperGameBoy;
    private Image primaryFrameSnapshot;
    private Image secondaryFrameSnapshot;
    private KeyListener keyListener;
    private KeyListener secondaryKeyListener;
    private OverlayIcon overlayIcon;
    private boolean windowLocationInitialized;

    public EmulatorWindow(EmulatorMenuActions menuActions, AppSettings settings) {
        this(menuActions, settings, JFrame.EXIT_ON_CLOSE);
    }

    public EmulatorWindow(EmulatorMenuActions menuActions, AppSettings settings, int closeOperation) {
        int scale = settings.screenScale();
        this.scale = Math.max(1, Math.min(8, scale));
        this.smoothScaling = settings.smoothScaling();
        this.xbrzFiltering = settings.xBrzFiltering();
        this.fullscreen = settings.fullscreen();
        window.setDefaultCloseOperation(closeOperation);
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
        updateScreenPreferredSize();
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
            updateScreenPreferredSize();
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
            updateScreenPreferredSize();
            applyWindowMode();
            screen.repaint();
        });
    }

    private void applyWindowMode() {
        boolean visible = window.isVisible();
        if (window.isDisplayable()) {
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
        attach(ppu, keyListener, null);
    }

    public void attach(Ppu ppu, KeyListener keyListener, SuperGameBoy superGameBoy) {
        this.ppu = ppu;
        this.superGameBoy = superGameBoy;
        SwingUtilities.invokeLater(() -> {
            if (this.ppu != ppu) {
                return;
            }
            detachKeyListener();
            this.keyListener = keyListener;
            if (keyListener != null) {
                screen.addKeyListener(keyListener);
            }
            updateScreenPreferredSize();
            applyWindowMode();
            screen.repaint();
        });
    }

    public void attachSecondary(Ppu ppu, KeyListener keyListener) {
        attachSecondary(ppu, keyListener, null);
    }

    public void attachSecondary(Ppu ppu, KeyListener keyListener, SuperGameBoy superGameBoy) {
        this.secondaryPpu = ppu;
        this.secondarySuperGameBoy = superGameBoy;
        SwingUtilities.invokeLater(() -> {
            if (this.secondaryPpu != ppu) {
                return;
            }
            detachSecondaryKeyListener();
            this.secondaryKeyListener = keyListener;
            if (keyListener != null) {
                screen.addKeyListener(keyListener);
            }
            updateScreenPreferredSize();
            applyWindowMode();
            screen.repaint();
        });
    }

    public void detach(Ppu expectedPpu) {
        if (expectedPpu != null && ppu != expectedPpu && secondaryPpu != expectedPpu) {
            return;
        }
        if (expectedPpu == null || ppu == expectedPpu) {
            ppu = null;
            superGameBoy = null;
        }
        if (expectedPpu == null || secondaryPpu == expectedPpu) {
            secondaryPpu = null;
            secondarySuperGameBoy = null;
        }
        SwingUtilities.invokeLater(() -> {
            if (expectedPpu == null || ppu == expectedPpu) {
                primaryFrameSnapshot = null;
            }
            if (expectedPpu == null || secondaryPpu == expectedPpu) {
                secondaryFrameSnapshot = null;
            }
            if (expectedPpu != null
                    && ppu != null
                    && secondaryPpu != null
                    && ppu != expectedPpu
                    && secondaryPpu != expectedPpu) {
                return;
            }
            detachKeyListener();
            detachSecondaryKeyListener();
            repaintTimer.stop();
            rewindHoldTimer.stop();
            updateScreenPreferredSize();
            screen.repaint();
        });
    }

    public void renderFrame(Ppu source) {
        if (source == null || (source != ppu && source != secondaryPpu)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (source == ppu) {
                primaryFrameSnapshot = captureDisplayFrame(source, superGameBoy);
            } else if (source == secondaryPpu) {
                secondaryFrameSnapshot = captureDisplayFrame(source, secondarySuperGameBoy);
            }
            updateScreenPreferredSize();
            screen.repaint();
        });
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

    private void detachSecondaryKeyListener() {
        if (secondaryKeyListener != null) {
            screen.removeKeyListener(secondaryKeyListener);
            secondaryKeyListener = null;
        }
    }

    private void updateScreenPreferredSize() {
        int primaryScale = displayScale(superGameBoy);
        int width = displayWidth(superGameBoy) * primaryScale;
        int height = displayHeight(superGameBoy) * primaryScale;
        if (secondaryPpu != null) {
            int secondaryScale = displayScale(secondarySuperGameBoy);
            width += displayWidth(secondarySuperGameBoy) * secondaryScale;
            height = Math.max(height, displayHeight(secondarySuperGameBoy) * secondaryScale);
        }
        Dimension preferredSize = new Dimension(width, height);
        boolean preferredSizeChanged = !preferredSize.equals(screen.getPreferredSize());
        if (preferredSizeChanged) {
            screen.setPreferredSize(preferredSize);
            screen.revalidate();
        }
        if (!fullscreen && (preferredSizeChanged || !preferredSize.equals(screen.getSize()))) {
            window.pack();
            window.validate();
            SwingUtilities.invokeLater(() -> {
                window.pack();
                window.validate();
            });
        }
    }

    private int displayWidth(SuperGameBoy sgb) {
        return sgb != null && sgb.hasBorder() ? SuperGameBoy.BORDER_WIDTH : WIDTH;
    }

    private int displayHeight(SuperGameBoy sgb) {
        return sgb != null && sgb.hasBorder() ? SuperGameBoy.BORDER_HEIGHT : HEIGHT;
    }

    private int displayScale(SuperGameBoy sgb) {
        return scale;
    }


    private void installMenu(EmulatorMenuActions menuActions) {
        JMenuBar menuBar = new JMenuBar();
        JMenu emulatorMenu = new JMenu("Emulator");

        JMenuItem openRom = new JMenuItem("Start ROM...");
        openRom.addActionListener(event -> menuActions.openRom().run());
        openRom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        emulatorMenu.add(openRom);

        JMenuItem openLinkedSession = new JMenuItem("Start linked session...");
        openLinkedSession.addActionListener(event -> menuActions.openLinkedSession().run());
        emulatorMenu.add(openLinkedSession);

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
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                smoothScaling ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        if (secondaryPpu == null) {
            int primaryScale = displayScale(superGameBoy);
            drawDisplay(graphics, ppu, primaryFrameSnapshot, superGameBoy, 0, 0, fullscreen ? screen.getWidth() : displayWidth(superGameBoy) * primaryScale, fullscreen ? screen.getHeight() : displayHeight(superGameBoy) * primaryScale);
        } else {
            int primaryScale = displayScale(superGameBoy);
            int secondaryScale = displayScale(secondarySuperGameBoy);
            int firstWidth = fullscreen ? screen.getWidth() / 2 : displayWidth(superGameBoy) * primaryScale;
            int secondWidth = fullscreen ? screen.getWidth() - firstWidth : displayWidth(secondarySuperGameBoy) * secondaryScale;
            int firstHeight = fullscreen ? screen.getHeight() : displayHeight(superGameBoy) * primaryScale;
            int secondHeight = fullscreen ? screen.getHeight() : displayHeight(secondarySuperGameBoy) * secondaryScale;
            drawDisplay(graphics, ppu, primaryFrameSnapshot, superGameBoy, 0, 0, firstWidth, firstHeight);
            drawDisplay(graphics, secondaryPpu, secondaryFrameSnapshot, secondarySuperGameBoy, firstWidth, 0, secondWidth, secondHeight);
        }
        drawOverlay(g);
    }

    private void drawDisplay(Graphics2D graphics, Ppu source, Image frameSnapshot, SuperGameBoy sgb, int x, int y, int drawWidth, int drawHeight) {
        if (sgb != null && sgb.hasBorder() && frameSnapshot != null) {
            graphics.drawImage(frameSnapshot, x, y, drawWidth, drawHeight, null);
            return;
        }
        if (sgb != null && sgb.hasBorder()) {
            Image border = sgb.borderImage();
            graphics.drawImage(border, x, y, drawWidth, drawHeight, null);
            int gameX = x + scaleCoordinate(SuperGameBoy.GAME_SCREEN_X, drawWidth, SuperGameBoy.BORDER_WIDTH);
            int gameY = y + scaleCoordinate(SuperGameBoy.GAME_SCREEN_Y, drawHeight, SuperGameBoy.BORDER_HEIGHT);
            int gameWidth = scaleCoordinate(WIDTH, drawWidth, SuperGameBoy.BORDER_WIDTH);
            int gameHeight = scaleCoordinate(HEIGHT, drawHeight, SuperGameBoy.BORDER_HEIGHT);
            drawPpuFrame(graphics, source, frameSnapshot, gameX, gameY, gameWidth, gameHeight);
            return;
        }
        drawPpuFrame(graphics, source, frameSnapshot, x, y, drawWidth, drawHeight);
    }

    private void drawPpuFrame(Graphics2D graphics, Ppu source, int x, int drawWidth, int drawHeight) {
        drawPpuFrame(graphics, source, null, x, 0, drawWidth, drawHeight);
    }

    private void drawPpuFrame(Graphics2D graphics, Ppu source, Image frameSnapshot, int x, int y, int drawWidth, int drawHeight) {
        if (source == null) {
            return;
        }
        Image frame = frameSnapshot != null ? frameSnapshot : source.getFrameBuffer();
        if (xbrzFiltering) {
            frame = AwtXbrz.scaleImage(frame, scale);
        }
        graphics.drawImage(frame, x, y, drawWidth, drawHeight, null);
    }

    private Image captureDisplayFrame(Ppu source, SuperGameBoy sgb) {
        if (source == null) {
            return null;
        }
        if (sgb != null && sgb.hasBorder()) {
            BufferedImage border = sgb.copyBorderImage();
            if (border != null) {
                BufferedImage composed = new BufferedImage(SuperGameBoy.BORDER_WIDTH, SuperGameBoy.BORDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = composed.createGraphics();
                try {
                    graphics.drawImage(border, 0, 0, null);
                    graphics.drawImage(sgb.colorizeFrame(source.getFrameBuffer()), SuperGameBoy.GAME_SCREEN_X, SuperGameBoy.GAME_SCREEN_Y, WIDTH, HEIGHT, null);
                } finally {
                    graphics.dispose();
                }
                return composed;
            }
        }
        return sgb != null && sgb.isEnabled()
                ? sgb.colorizeFrame(source.getFrameBuffer())
                : copyImage(source.getFrameBuffer());
    }

    private BufferedImage copyImage(Image source) {
        BufferedImage copy = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private int scaleCoordinate(int value, int targetSize, int sourceSize) {
        return Math.round(value * targetSize / (float) sourceSize);
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
