package dev.vitorsilverio.gbcemu.ppu;

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

    public Display(Ppu ppu, KeyListener keyListener) {
        this.ppu = ppu;
        this.keyListener = keyListener;
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
        window.pack();
        window.setLocationRelativeTo(null);
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
