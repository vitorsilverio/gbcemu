package dev.vitorsilverio.gbcemu.ppu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;

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
        initializeWindow();
        this.keyListener = keyListener;
    }

    private void initializeWindow() {
        window = new JFrame("GBC EMU");
        window.addKeyListener(keyListener);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(width * scale, height * scale);
        window.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scale = e.getComponent().getWidth() / width;
                e.getComponent().setSize(width * scale, height * scale);
            }

        });
        screen = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame(g);
            }
        };
        screen.setPreferredSize(new Dimension(width * scale, height * scale));
        window.setContentPane(screen);
        window.pack();
    }

    private void drawFrame(Graphics g) {
        Image frame = ppu.getFrameBuffer();
        g.drawImage(frame, 0, 0, width * scale, height * scale, null);
    }


    @Override
    public void run() {
        BufferStrategy bs = window.getBufferStrategy();
        if (bs == null) {
            window.createBufferStrategy(3); // Use double or triple buffering
        }
        window.setVisible(true);
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
