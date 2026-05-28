package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.ppu.PpuState;
import dev.vitorsilverio.gbcemu.ppu.TileMapArea;
import dev.vitorsilverio.gbcemu.ppu.VideoRamState;
import dev.vitorsilverio.gbcemu.sgb.SuperGameBoy;
import dev.vitorsilverio.gbcemu.util.DebugJson;
import dev.vitorsilverio.gbcemu.util.RawImage;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.DefaultComboBoxModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class PpuDebugWindow {

    public record Target(String name, Ppu ppu, SuperGameBoy superGameBoy) {
        @Override
        public String toString() {
            return name;
        }
    }

    private Ppu ppu;
    private SuperGameBoy superGameBoy;
    private final JComboBox<Target> targetSelector;
    private final Supplier<List<Target>> targetSupplier;
    private final JFrame frame = new JFrame("PPU Debug");
    private final ImagePanel tilesBank0 = new ImagePanel(3);
    private final ImagePanel tilesBank1 = new ImagePanel(3);
    private final ImagePanel bgMap9800 = new ImagePanel(2);
    private final ImagePanel bgMap9C00 = new ImagePanel(2);
    private final ImagePanel bgPalettes = new ImagePanel(4);
    private final ImagePanel objPalettes = new ImagePanel(4);
    private final ImagePanel sgbBorder = new ImagePanel(2);
    private final JTextArea sgbStatus = new JTextArea("SGB inactive", 2, 80);
    private final JCheckBox autoRefresh = new JCheckBox("Auto refresh");
    private Dimension lastSgbBorderPreferredSize = sgbBorder.getPreferredSize();
    private boolean updatingTargetSelector;
    private final Timer autoRefreshTimer = new Timer(1000, event -> {
        if (autoRefresh.isSelected() && frame.isVisible()) {
            refresh();
        }
    });

    public PpuDebugWindow(Ppu ppu) {
        this(ppu, null);
    }

    public PpuDebugWindow(Ppu ppu, SuperGameBoy superGameBoy) {
        this.ppu = ppu;
        this.superGameBoy = superGameBoy;
        this.targetSelector = null;
        this.targetSupplier = null;
        initializeWindow();
    }

    public PpuDebugWindow(List<Target> targets) {
        this(() -> targets);
    }

    public PpuDebugWindow(Supplier<List<Target>> targetSupplier) {
        List<Target> targets = targetSupplier.get();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one PPU debug target is required");
        }
        this.targetSupplier = targetSupplier;
        this.targetSelector = new JComboBox<>(targets.toArray(Target[]::new));
        applyTarget(targets.getFirst());
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
        tabs.addTab("SGB Border", sgbBorderPanel());
        content.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(content);
        refresh();
        frame.pack();
        frame.setVisible(true);
        autoRefreshTimer.start();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel();
        if (targetSelector != null) {
            targetSelector.addActionListener(event -> {
                if (updatingTargetSelector) {
                    return;
                }
                Target target = (Target) targetSelector.getSelectedItem();
                if (target != null) {
                    applyTarget(target);
                    refresh();
                }
            });
            toolbar.add(targetSelector);
        }
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        JButton dump = new JButton("Dump");
        dump.addActionListener(event -> dump());
        JButton dumpJson = new JButton("Dump JSON");
        dumpJson.addActionListener(event -> dumpJson());
        toolbar.add(refresh);
        toolbar.add(autoRefresh);
        toolbar.add(dump);
        toolbar.add(dumpJson);
        return toolbar;
    }

    private void applyTarget(Target target) {
        this.ppu = target.ppu();
        this.superGameBoy = target.superGameBoy();
        this.lastSgbBorderPreferredSize = sgbBorder.getPreferredSize();
    }

    private JPanel imageGrid(ImagePanel first, ImagePanel second) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 8));
        panel.add(new JScrollPane(first));
        panel.add(new JScrollPane(second));
        return panel;
    }

    private JPanel sgbBorderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        sgbStatus.setEditable(false);
        sgbStatus.setLineWrap(true);
        sgbStatus.setWrapStyleWord(false);
        sgbStatus.setOpaque(false);
        sgbStatus.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(sgbStatus, BorderLayout.NORTH);
        panel.add(new JScrollPane(sgbBorder), BorderLayout.CENTER);
        return panel;
    }

    private void refresh() {
        refreshTargets();
        tilesBank0.setImage(ppu.debugTileImage(0));
        tilesBank1.setImage(ppu.debugTileImage(1));
        bgMap9800.setImage(ppu.debugTileMapImage(TileMapArea.IN_9800));
        bgMap9C00.setImage(ppu.debugTileMapImage(TileMapArea.IN_9C00));
        bgPalettes.setImage(ppu.debugPaletteImage(false));
        objPalettes.setImage(ppu.debugPaletteImage(true));
        sgbBorder.setImage(sgbPreviewImage());
        sgbStatus.setText(sgbStatusText());
        repackWhenSgbBorderSizeChanges();
    }

    private void refreshTargets() {
        if (targetSupplier == null || targetSelector == null) {
            return;
        }
        List<Target> targets = targetSupplier.get();
        if (targets.isEmpty()) {
            return;
        }
        Target selected = (Target) targetSelector.getSelectedItem();
        Target next = selected != null && targets.contains(selected) ? selected : targets.getFirst();
        updatingTargetSelector = true;
        try {
            targetSelector.setModel(new DefaultComboBoxModel<>(targets.toArray(Target[]::new)));
            targetSelector.setSelectedItem(next);
        } finally {
            updatingTargetSelector = false;
        }
        applyTarget(next);
    }

    private BufferedImage sgbPreviewImage() {
        BufferedImage border = superGameBoy == null ? null : SwingImages.toBufferedImage(superGameBoy.copyBorderImage());
        if (border == null) {
            return null;
        }
        BufferedImage preview = new BufferedImage(SuperGameBoy.BORDER_WIDTH, SuperGameBoy.BORDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        try {
            graphics.drawImage(border, 0, 0, null);
            graphics.drawImage(SwingImages.toBufferedImage(superGameBoy.colorizeFrame(ppu.getFrameBuffer())), SuperGameBoy.GAME_SCREEN_X, SuperGameBoy.GAME_SCREEN_Y, 160, 144, null);
        } finally {
            graphics.dispose();
        }
        return preview;
    }

    private String sgbStatusText() {
        if (superGameBoy == null) {
            return "SGB not attached";
        }
        return String.format(
                "enabled=%s border=%s transferMask=%s systemPalettes=%s last=%s recent=%s header=%02X receiving=%s packets=%d/%d pending=%s/%d queued=%d tileBytes=%d pctBytes=%d mapUnique=%d attrPalettes=%d total=%d invalid=%d ignored=%d pulses=%d joypads=%d selected=%d mask=%d palettes=%s packet=%s",
                superGameBoy.isEnabled(),
                superGameBoy.hasBorder(),
                superGameBoy.isTransferMaskActive(),
                superGameBoy.systemPalettesReady(),
                superGameBoy.lastCommandName(),
                superGameBoy.recentCommandHistory(),
                superGameBoy.lastHeaderByte(),
                superGameBoy.isReceivingPacket(),
                superGameBoy.receivedPackets(),
                superGameBoy.expectedPackets(),
                superGameBoy.pendingTransferName(),
                superGameBoy.pendingTransferFrames(),
                superGameBoy.pendingTransferCount(),
                superGameBoy.borderTileNonZeroBytes(),
                superGameBoy.pictureTransferNonZeroBytes(),
                superGameBoy.pictureTransferUniqueMapEntries(),
                superGameBoy.screenAttributeUniqueCount(),
                superGameBoy.packetsReceived(),
                superGameBoy.invalidPackets(),
                superGameBoy.ignoredPackets(),
                superGameBoy.pulseCount(),
                superGameBoy.joypadCount(),
                superGameBoy.selectedJoypad(),
                superGameBoy.maskMode(),
                superGameBoy.screenPalettesSample(),
                superGameBoy.lastPacketHex()
        );
    }

    private void repackWhenSgbBorderSizeChanges() {
        Dimension preferredSize = sgbBorder.getPreferredSize();
        if (!preferredSize.equals(lastSgbBorderPreferredSize)) {
            lastSgbBorderPreferredSize = preferredSize;
            frame.pack();
        }
    }

    private void dump() {
        File target = DebugJson.debugDirectory();
        try {
            ImageIO.write(SwingImages.toBufferedImage(ppu.debugTileImage(0)), "png", new File(target, "debug-ppu-tiles-bank0.png"));
            ImageIO.write(SwingImages.toBufferedImage(ppu.debugTileImage(1)), "png", new File(target, "debug-ppu-tiles-bank1.png"));
            ImageIO.write(SwingImages.toBufferedImage(ppu.debugTileMapImage(TileMapArea.IN_9800)), "png", new File(target, "debug-ppu-tilemap-9800.png"));
            ImageIO.write(SwingImages.toBufferedImage(ppu.debugTileMapImage(TileMapArea.IN_9C00)), "png", new File(target, "debug-ppu-tilemap-9c00.png"));
            ImageIO.write(SwingImages.toBufferedImage(ppu.debugPaletteImage(false)), "png", new File(target, "debug-ppu-bg-palettes.png"));
            ImageIO.write(SwingImages.toBufferedImage(ppu.debugPaletteImage(true)), "png", new File(target, "debug-ppu-obj-palettes.png"));
            BufferedImage border = superGameBoy == null ? null : SwingImages.toBufferedImage(superGameBoy.copyBorderImage());
            if (border != null) {
                ImageIO.write(border, "png", new File(target, "debug-ppu-sgb-border.png"));
            }
            BufferedImage sgbFrame = superGameBoy == null || !superGameBoy.isEnabled()
                    ? null
                    : SwingImages.toBufferedImage(superGameBoy.colorizeFrame(ppu.getFrameBuffer()));
            if (sgbFrame != null) {
                ImageIO.write(sgbFrame, "png", new File(target, "debug-ppu-sgb-frame.png"));
            }
            BufferedImage sgbAttributes = superGameBoy == null || !superGameBoy.isEnabled()
                    ? null
                    : SwingImages.toBufferedImage(superGameBoy.debugAttributeImage());
            if (sgbAttributes != null) {
                ImageIO.write(sgbAttributes, "png", new File(target, "debug-ppu-sgb-attributes.png"));
            }
            BufferedImage sgbPreview = sgbPreviewImage();
            if (sgbPreview != null) {
                ImageIO.write(sgbPreview, "png", new File(target, "debug-ppu-sgb-preview.png"));
            }
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
        appendSuperGameBoyJson(builder);
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

    private void appendSuperGameBoyJson(StringBuilder builder) {
        builder.append("  \"superGameBoy\": {\n");
        DebugJson.appendBoolean(builder, "enabled", superGameBoy != null && superGameBoy.isEnabled(), true, 4);
        DebugJson.appendBoolean(builder, "borderReady", superGameBoy != null && superGameBoy.hasBorder(), true, 4);
        DebugJson.appendString(builder, "sgbBorderPng", superGameBoy != null && superGameBoy.hasBorder() ? "debug-ppu-sgb-border.png" : "", true, 4);
        DebugJson.appendString(builder, "sgbFramePng", superGameBoy != null && superGameBoy.isEnabled() ? "debug-ppu-sgb-frame.png" : "", true, 4);
        DebugJson.appendString(builder, "sgbAttributesPng", superGameBoy != null && superGameBoy.isEnabled() ? "debug-ppu-sgb-attributes.png" : "", true, 4);
        DebugJson.appendString(builder, "sgbPreviewPng", superGameBoy != null && superGameBoy.hasBorder() ? "debug-ppu-sgb-preview.png" : "", true, 4);
        DebugJson.appendBoolean(builder, "transferMaskActive", superGameBoy != null && superGameBoy.isTransferMaskActive(), true, 4);
        DebugJson.appendBoolean(builder, "systemPalettesReady", superGameBoy != null && superGameBoy.systemPalettesReady(), true, 4);
        DebugJson.appendString(builder, "lastCommand", superGameBoy == null ? "" : superGameBoy.lastCommandName(), true, 4);
        DebugJson.appendString(builder, "recentCommands", superGameBoy == null ? "" : superGameBoy.recentCommandHistory(), true, 4);
        DebugJson.appendHex(builder, "lastHeaderByte", superGameBoy == null ? 0 : superGameBoy.lastHeaderByte(), true, 4, 2);
        DebugJson.appendString(builder, "lastPacketHex", superGameBoy == null ? "" : superGameBoy.lastPacketHex(), true, 4);
        DebugJson.appendNumber(builder, "maskMode", superGameBoy == null ? 0 : superGameBoy.maskMode(), true, 4);
        DebugJson.appendBoolean(builder, "receivingPacket", superGameBoy != null && superGameBoy.isReceivingPacket(), true, 4);
        DebugJson.appendNumber(builder, "expectedPackets", superGameBoy == null ? 0 : superGameBoy.expectedPackets(), true, 4);
        DebugJson.appendNumber(builder, "receivedPackets", superGameBoy == null ? 0 : superGameBoy.receivedPackets(), true, 4);
        DebugJson.appendString(builder, "pendingTransfer", superGameBoy == null ? "" : superGameBoy.pendingTransferName(), true, 4);
        DebugJson.appendNumber(builder, "pendingTransferFrames", superGameBoy == null ? 0 : superGameBoy.pendingTransferFrames(), true, 4);
        DebugJson.appendNumber(builder, "pendingTransferCount", superGameBoy == null ? 0 : superGameBoy.pendingTransferCount(), true, 4);
        DebugJson.appendNumber(builder, "borderTileNonZeroBytes", superGameBoy == null ? 0 : superGameBoy.borderTileNonZeroBytes(), true, 4);
        DebugJson.appendNumber(builder, "pictureTransferNonZeroBytes", superGameBoy == null ? 0 : superGameBoy.pictureTransferNonZeroBytes(), true, 4);
        DebugJson.appendNumber(builder, "pictureTransferUniqueMapEntries", superGameBoy == null ? 0 : superGameBoy.pictureTransferUniqueMapEntries(), true, 4);
        DebugJson.appendString(builder, "screenPalettes", superGameBoy == null ? "" : superGameBoy.screenPalettesSample(), true, 4);
        DebugJson.appendNumber(builder, "screenAttributeUniqueCount", superGameBoy == null ? 0 : superGameBoy.screenAttributeUniqueCount(), true, 4);
        DebugJson.appendString(builder, "borderTileDataSample", superGameBoy == null ? "" : superGameBoy.borderTileDataSample(), true, 4);
        DebugJson.appendString(builder, "pictureTransferMapSample", superGameBoy == null ? "" : superGameBoy.pictureTransferMapSample(), true, 4);
        DebugJson.appendString(builder, "pictureTransferPaletteSample", superGameBoy == null ? "" : superGameBoy.pictureTransferPaletteSample(), true, 4);
        DebugJson.appendLong(builder, "packetsReceivedTotal", superGameBoy == null ? 0 : superGameBoy.packetsReceived(), true, 4);
        DebugJson.appendLong(builder, "invalidPackets", superGameBoy == null ? 0 : superGameBoy.invalidPackets(), true, 4);
        DebugJson.appendLong(builder, "ignoredPackets", superGameBoy == null ? 0 : superGameBoy.ignoredPackets(), true, 4);
        DebugJson.appendNumber(builder, "pulseCount", superGameBoy == null ? 0 : superGameBoy.pulseCount(), true, 4);
        DebugJson.appendNumber(builder, "joypadCount", superGameBoy == null ? 1 : superGameBoy.joypadCount(), true, 4);
        DebugJson.appendNumber(builder, "selectedJoypad", superGameBoy == null ? 0 : superGameBoy.selectedJoypad(), false, 4);
        builder.append("  },\n");
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

        private void setImage(RawImage image) {
            this.image = SwingImages.toBufferedImage(image);
            if (image != null) {
                setPreferredSize(new Dimension(image.width() * scale, image.height() * scale));
            }
            revalidate();
            repaint();
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
