package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.ppu.PpuState;
import dev.vitorsilverio.gbcemu.ppu.TileMapArea;
import dev.vitorsilverio.gbcemu.ppu.VideoRamState;
import dev.vitorsilverio.gbcemu.util.DebugJson;

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
        JButton dumpJson = new JButton("Dump JSON");
        dumpJson.addActionListener(event -> dumpJson());
        toolbar.add(refresh);
        toolbar.add(dump);
        toolbar.add(dumpJson);
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

    private void dumpJson() {
        DebugJson.writeTargetFile("debug-ppu-window.json", ppuJsonText(), "Failed to dump PPU debugger JSON file");
    }

    private String ppuJsonText() {
        Ppu.DebugSnapshot snapshot = ppu.debugSnapshot();
        Ppu.FrameDebugStats frameStats = ppu.frameDebugStats();
        PpuState state = ppu.saveState();
        VideoRamState videoRamState = ppu.getVideoRam().saveState();
        byte[] oam = ppu.getOam().saveState().data();
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"ppu\": {\n");
        DebugJson.appendBoolean(builder, "cgbMode", snapshot.cgbMode(), true, 4);
        DebugJson.appendHex(builder, "lcdc", snapshot.lcdc(), true, 4, 2);
        DebugJson.appendHex(builder, "stat", snapshot.stat(), true, 4, 2);
        DebugJson.appendString(builder, "mode", String.valueOf(snapshot.mode()), true, 4);
        DebugJson.appendNumber(builder, "line", snapshot.line(), true, 4);
        DebugJson.appendNumber(builder, "column", snapshot.column(), true, 4);
        DebugJson.appendNumber(builder, "cycles", snapshot.cycles(), true, 4);
        DebugJson.appendNumber(builder, "scrollX", snapshot.scrollX(), true, 4);
        DebugJson.appendNumber(builder, "scrollY", snapshot.scrollY(), true, 4);
        DebugJson.appendNumber(builder, "windowX", snapshot.windowX(), true, 4);
        DebugJson.appendNumber(builder, "windowY", snapshot.windowY(), true, 4);
        DebugJson.appendHex(builder, "lineCompare", snapshot.lineCompare(), true, 4, 2);
        DebugJson.appendString(builder, "objectPriorityMode", String.valueOf(state.objectPriorityMode()), true, 4);
        DebugJson.appendNumber(builder, "penaltyDelay", state.penaltyDelay(), true, 4);
        DebugJson.appendNumber(builder, "hBlankCycles", state.hBlankCycles(), true, 4);
        DebugJson.appendBoolean(builder, "frameReady", state.frameReady(), false, 4);
        builder.append("  },\n");
        appendFrameStatsJson(builder, frameStats);
        builder.append("  \"palettes\": {\n");
        DebugJson.appendHex(builder, "bgPaletteIndex", state.bgPaletteIndex() & 0xFF, true, 4, 2);
        DebugJson.appendString(builder, "bgPalette", DebugJson.bytesHex(state.bgPalette(), 0, state.bgPalette().length), true, 4);
        DebugJson.appendHex(builder, "objPaletteIndex", state.objPaletteIndex() & 0xFF, true, 4, 2);
        DebugJson.appendString(builder, "objPalette", DebugJson.bytesHex(state.objPalette(), 0, state.objPalette().length), true, 4);
        DebugJson.appendHex(builder, "bgPaletteDmg", state.bgPaletteDmg() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "obj0PaletteDmg", state.obj0PaletteDmg() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "obj1PaletteDmg", state.obj1PaletteDmg() & 0xFF, false, 4, 2);
        builder.append("  },\n");
        builder.append("  \"vram\": {\n");
        DebugJson.appendNumber(builder, "currentBank", videoRamState.bank(), true, 4);
        DebugJson.appendString(builder, "tileDataBank0Sample", tileDataSample(videoRamState, 0), true, 4);
        DebugJson.appendString(builder, "tileDataBank1Sample", tileDataSample(videoRamState, 1), true, 4);
        DebugJson.appendString(builder, "tileMap9800IndexesSample", DebugJson.bytesHex(videoRamState.tileMapIndexes(), 0, 64), true, 4);
        DebugJson.appendString(builder, "tileMap9800AttributesSample", DebugJson.bytesHex(videoRamState.tileMapAttributes(), 0, 64), true, 4);
        DebugJson.appendString(builder, "tileMap9c00IndexesSample", DebugJson.bytesHex(videoRamState.tileMapIndexes(), 0x400, 64), true, 4);
        DebugJson.appendString(builder, "tileMap9c00AttributesSample", DebugJson.bytesHex(videoRamState.tileMapAttributes(), 0x400, 64), false, 4);
        builder.append("  },\n");
        appendOamJson(builder, oam);
        builder.append("}\n");
        return builder.toString();
    }

    private void appendFrameStatsJson(StringBuilder builder, Ppu.FrameDebugStats stats) {
        builder.append("  \"frameStats\": {\n");
        DebugJson.appendNumber(builder, "blackPixels", stats.blackPixels(), true, 4);
        DebugJson.appendNumber(builder, "whitePixels", stats.whitePixels(), true, 4);
        DebugJson.appendNumber(builder, "otherPixels", stats.otherPixels(), true, 4);
        DebugJson.appendNumber(builder, "nonEmptyTiles", stats.nonEmptyTiles(), true, 4);
        DebugJson.appendHex(builder, "bgColor0", stats.bgColor0(), true, 4, 8);
        DebugJson.appendHex(builder, "bgColor1", stats.bgColor1(), true, 4, 8);
        DebugJson.appendHex(builder, "bgColor2", stats.bgColor2(), true, 4, 8);
        DebugJson.appendHex(builder, "bgColor3", stats.bgColor3(), true, 4, 8);
        DebugJson.appendHex(builder, "dmgBgPalette", stats.dmgBgPalette(), false, 4, 2);
        builder.append("  },\n");
    }

    private void appendOamJson(StringBuilder builder, byte[] oam) {
        builder.append("  \"oam\": [\n");
        for (int sprite = 0; sprite < 40; sprite++) {
            int offset = sprite * 4;
            builder.append("    {\n");
            DebugJson.appendNumber(builder, "index", sprite, true, 6);
            DebugJson.appendHex(builder, "y", oam[offset] & 0xFF, true, 6, 2);
            DebugJson.appendHex(builder, "x", oam[offset + 1] & 0xFF, true, 6, 2);
            DebugJson.appendHex(builder, "tile", oam[offset + 2] & 0xFF, true, 6, 2);
            DebugJson.appendHex(builder, "attributes", oam[offset + 3] & 0xFF, false, 6, 2);
            builder.append("    }");
            if (sprite < 39) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
    }

    private String tileDataSample(VideoRamState state, int bank) {
        StringBuilder builder = new StringBuilder();
        byte[][] tileData = state.tileData()[bank & 1];
        int length = Math.min(8, tileData.length);
        for (int tile = 0; tile < length; tile++) {
            if (tile > 0) {
                builder.append(" | ");
            }
            builder.append(String.format("%03d:", tile)).append(DebugJson.bytesHex(tileData[tile], 0, tileData[tile].length));
        }
        return builder.toString();
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
