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
    private Ppu ppu;
    private KeyListener keyListener;

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
        menuBar.add(debugMenu);

        JMenu snapshotMenu = new JMenu("Save states");
        JMenuItem saveSnapshot = new JMenuItem("Save states...");
        saveSnapshot.addActionListener(event -> menuActions.saveSnapshoot().run());
        saveSnapshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        snapshotMenu.add(saveSnapshot);

        JMenuItem restoreSnapshot = new JMenuItem("Load states...");
        restoreSnapshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        restoreSnapshot.addActionListener(event -> menuActions.restoreSnapshot().run());
        snapshotMenu.add(restoreSnapshot);

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
    }


}
