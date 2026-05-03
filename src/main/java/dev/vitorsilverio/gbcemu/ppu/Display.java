package dev.vitorsilverio.gbcemu.ppu;

import dev.vitorsilverio.gbcemu.EmulatorMenuActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;

public class Display implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(Display.class);

    private final Ppu ppu;
    private final int width = 160;
    private final int height = 144;
    private int scale = 4;

    private JPanel screen;
    private JFrame window;
    private final KeyListener keyListener;
    private final EmulatorMenuActions menuActions;

    public Display(Ppu ppu, KeyListener keyListener) {
        this(ppu, keyListener, null);
    }

    public Display(Ppu ppu, KeyListener keyListener, EmulatorMenuActions menuActions) {
        this.ppu = ppu;
        this.keyListener = keyListener;
        this.menuActions = menuActions;
        initializeWindow();
    }

    private void initializeWindow() {
        window = new JFrame("GBC EMU");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        screen = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame(g);
            }
        };
        screen.setFocusable(true);
        screen.addKeyListener(keyListener);
        screen.setPreferredSize(new Dimension(width * scale, height * scale));
        window.setContentPane(screen);
        window.setResizable(false);
        installMenu();
        window.pack();
        window.setLocationRelativeTo(null);
    }

    private void installMenu() {
        if (menuActions == null) {
            return;
        }

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Emulator");

        JMenuItem openRom = new JMenuItem("Start ROM...");
        openRom.addActionListener(event -> menuActions.openRom().run());
        fileMenu.add(openRom);

        JMenuItem pause = new JMenuItem("Pause");
        pause.addActionListener(event -> menuActions.pause().run());
        fileMenu.add(pause);

        JMenuItem resume = new JMenuItem("Resume");
        resume.addActionListener(event -> menuActions.resume().run());
        fileMenu.add(resume);

        JMenuItem stop = new JMenuItem("Stop");
        stop.addActionListener(event -> menuActions.stop().run());
        fileMenu.add(stop);

        fileMenu.addSeparator();

        JMenuItem configureBios = new JMenuItem("Set default BIOS...");
        configureBios.addActionListener(event -> menuActions.configureDefaultBios().run());
        fileMenu.add(configureBios);

        menuBar.add(fileMenu);

        JMenu debugMenu = new JMenu("Debug");
        JMenuItem openDebugger = new JMenuItem("Open debugger");
        openDebugger.addActionListener(event -> menuActions.openDebugger().run());
        debugMenu.add(openDebugger);
        menuBar.add(debugMenu);

        window.setJMenuBar(menuBar);
    }

    private void drawFrame(Graphics g) {
        Image frame = ppu.getFrameBuffer();
        g.drawImage(frame, 0, 0, width * scale, height * scale, null);
    }


    @Override
    public void run() {
        window.setVisible(true);
        screen.requestFocusInWindow();
        while (true) {
           try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                logger.error("Error {}", e);
            }
            screen.repaint();
        }

    }
}
