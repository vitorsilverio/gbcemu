package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.ppu.TileMapArea;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PpuDebugWindow {

    private final Ppu ppu;
    private final JFrame frame = new JFrame("PPU Debug");
    private final ImagePanel tilesBank0 = new ImagePanel(3);
    private final ImagePanel tilesBank1 = new ImagePanel(3);
    private final ImagePanel bgMap9800 = new ImagePanel(2);
    private final ImagePanel bgMap9C00 = new ImagePanel(2);
    private final ImagePanel bgPalettes = new ImagePanel(4);
    private final ImagePanel objPalettes = new ImagePanel(4);

    public PpuDebugWindow(Ppu ppu) {
        this.ppu = ppu;
        initializeWindow();
    }

    private void initializeWindow() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildToolbar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tiles", imageGrid(tilesBank0, tilesBank1));
        tabs.addTab("Tile Maps", imageGrid(bgMap9800, bgMap9C00));
        tabs.addTab("Palettes", imageGrid(bgPalettes, objPalettes));
        content.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(content);
        refresh();
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel();
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        JButton dump = new JButton("Dump");
        dump.addActionListener(event -> dump());
        toolbar.add(refresh);
        toolbar.add(dump);
        return toolbar;
    }

    private JPanel imageGrid(ImagePanel first, ImagePanel second) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 8));
        panel.add(new JScrollPane(first));
        panel.add(new JScrollPane(second));
        return panel;
    }

    private void refresh() {
        tilesBank0.setImage(ppu.debugTileImage(0));
        tilesBank1.setImage(ppu.debugTileImage(1));
        bgMap9800.setImage(ppu.debugTileMapImage(TileMapArea.IN_9800));
        bgMap9C00.setImage(ppu.debugTileMapImage(TileMapArea.IN_9C00));
        bgPalettes.setImage(ppu.debugPaletteImage(false));
        objPalettes.setImage(ppu.debugPaletteImage(true));
    }

    private void dump() {
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            ImageIO.write(ppu.debugTileImage(0), "png", new File(target, "debug-ppu-tiles-bank0.png"));
            ImageIO.write(ppu.debugTileImage(1), "png", new File(target, "debug-ppu-tiles-bank1.png"));
            ImageIO.write(ppu.debugTileMapImage(TileMapArea.IN_9800), "png", new File(target, "debug-ppu-tilemap-9800.png"));
            ImageIO.write(ppu.debugTileMapImage(TileMapArea.IN_9C00), "png", new File(target, "debug-ppu-tilemap-9c00.png"));
            ImageIO.write(ppu.debugPaletteImage(false), "png", new File(target, "debug-ppu-bg-palettes.png"));
            ImageIO.write(ppu.debugPaletteImage(true), "png", new File(target, "debug-ppu-obj-palettes.png"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump PPU debugger files", e);
        }
    }

    private static class ImagePanel extends JPanel {
        private final int scale;
        private BufferedImage image;

        private ImagePanel(int scale) {
            this.scale = scale;
            setPreferredSize(new Dimension(512, 512));
        }

        private void setImage(BufferedImage image) {
            this.image = image;
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth() * scale, image.getHeight() * scale));
            }
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) {
                return;
            }
            Image scaled = image.getScaledInstance(image.getWidth() * scale, image.getHeight() * scale, Image.SCALE_FAST);
            graphics.drawImage(scaled, 0, 0, null);
        }
    }
}
