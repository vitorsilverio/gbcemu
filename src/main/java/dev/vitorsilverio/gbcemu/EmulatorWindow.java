package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.ppu.Ppu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EmulatorWindow {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 144;
    private static final int SCALE = 4;

    private final JFrame window = new JFrame("GBC EMU");
    private final JPanel screen;
    private final Timer repaintTimer;
    private final Timer overlayTimer;
    private Ppu ppu;
    private KeyListener keyListener;
    private OverlayIcon overlayIcon;

    public EmulatorWindow(EmulatorMenuActions menuActions) {
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        screen.setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        window.setContentPane(screen);
        window.setResizable(false);
        installMenu(menuActions);
        window.pack();
        window.setLocationRelativeTo(null);
    }

    public void show() {
        window.setVisible(true);
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
            repaintTimer.start();
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

        emulatorMenu.addSeparator();

        JMenuItem configureBios = new JMenuItem("Set default BIOS...");
        configureBios.addActionListener(event -> menuActions.configureDefaultBios().run());
        emulatorMenu.add(configureBios);

        menuBar.add(emulatorMenu);

        JMenu debugMenu = new JMenu("Debug");
        JMenuItem openDebugger = new JMenuItem("Open debugger");
        openDebugger.addActionListener(event -> menuActions.openDebugger().run());
        debugMenu.add(openDebugger);

        JMenuItem openAudioDebugger = new JMenuItem("Audio channels...");
        openAudioDebugger.addActionListener(event -> menuActions.audioDebugger().run());
        debugMenu.add(openAudioDebugger);
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
        rewindSnapshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
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

    private void drawFrame(Graphics g) {
        if(ppu!=null) {
            Image frame = ppu.getFrameBuffer();
            g.drawImage(frame, 0, 0, WIDTH * SCALE, HEIGHT * SCALE, null);
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
            int width = overlayIcon == OverlayIcon.PAUSE ? 96 : 88;
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
        }
    }

    public enum OverlayIcon {
        PLAY("PLAY"),
        PAUSE("PAUSE"),
        STOP("STOP"),
        REWIND("REW");

        private final String label;

        OverlayIcon(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }
}
