package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.CpuState;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.ppu.TileMapArea;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DebugWindow {

    private static DebugWindow current;

    private final Cpu cpu;
    private final Bus bus;
    private final Ppu ppu;
    private final DebugController debugController;
    private final Runnable pauseAction;
    private final Runnable resumeAction;
    private final DisassemblyCache disassemblyCache;
    private final JFrame window = new JFrame("GBC EMU Debugger");
    private final Map<String, JTextField> stateFields = new LinkedHashMap<>();
    private final DefaultTableModel instructionModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable instructionTable = new JTable(instructionModel);
    private final JTextArea memoryMapText = textArea();
    private final JTextField memoryStart = new JTextField("C000", 6);
    private final JTextField memoryLength = new JTextField("0100", 6);
    private final JComboBox<MemoryRegion> memoryRegion = new JComboBox<>(MemoryRegion.values());
    private final DefaultTableModel memoryModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column > 0;
        }
    };
    private final JTable memoryTable = new JTable(memoryModel);
    private final JTextField pcBreakpoint = new JTextField("0100", 6);
    private final DefaultListModel<String> breakpointModel = new DefaultListModel<>();
    private final JList<String> breakpointList = new JList<>(breakpointModel);
    private boolean updatingMemoryTable;
    private final ImagePanel tilesBank0 = new ImagePanel(3);
    private final ImagePanel tilesBank1 = new ImagePanel(3);
    private final ImagePanel bgMap9800 = new ImagePanel(2);
    private final ImagePanel bgMap9C00 = new ImagePanel(2);
    private final ImagePanel bgPalettes = new ImagePanel(4);
    private final ImagePanel objPalettes = new ImagePanel(4);

    public static void open(Cpu cpu, Bus bus, Ppu ppu, DebugController debugController, Runnable pauseAction, Runnable resumeAction) {
        if (current == null) {
            current = new DebugWindow(cpu, bus, ppu, debugController, pauseAction, resumeAction);
        }
        current.show();
    }

    private DebugWindow(Cpu cpu, Bus bus, Ppu ppu, DebugController debugController, Runnable pauseAction, Runnable resumeAction) {
        this.cpu = cpu;
        this.bus = bus;
        this.ppu = ppu;
        this.debugController = debugController;
        this.pauseAction = pauseAction;
        this.resumeAction = resumeAction;
        this.disassemblyCache = new DisassemblyCache(bus);
        initialize();
    }

    private void initialize() {
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        window.setLayout(new BorderLayout());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        JButton dump = new JButton("Dump to target/debug-*");
        dump.addActionListener(event -> dump());
        JPanel toolbar = new JPanel();
        toolbar.add(refresh);
        toolbar.add(dump);
        window.add(toolbar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("CPU / Instructions", cpuPanel());
        tabs.addTab("Breakpoints", breakpointsPanel());
        tabs.addTab("Memory Map", new JScrollPane(memoryMapText));
        tabs.addTab("Memory", memoryPanel());
        tabs.addTab("Tiles", imageGrid(tilesBank0, tilesBank1));
        tabs.addTab("Tile Maps", imageGrid(bgMap9800, bgMap9C00));
        tabs.addTab("Palettes", imageGrid(bgPalettes, objPalettes));
        window.add(tabs, BorderLayout.CENTER);

        window.setSize(900, 720);
        window.setLocationRelativeTo(null);
        new Timer(250, event -> {
            if (window.isVisible()) {
                refresh();
            }
        }).start();
    }

    private JPanel cpuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel statePanel = new JPanel(new GridLayout(0, 6, 4, 4));
        addStateField(statePanel, "PC");
        addStateField(statePanel, "SP");
        addStateField(statePanel, "AF");
        addStateField(statePanel, "BC");
        addStateField(statePanel, "DE");
        addStateField(statePanel, "HL");
        addStateField(statePanel, "A");
        addStateField(statePanel, "B");
        addStateField(statePanel, "C");
        addStateField(statePanel, "D");
        addStateField(statePanel, "E");
        addStateField(statePanel, "H");
        addStateField(statePanel, "L");
        addStateField(statePanel, "Flags");
        addStateField(statePanel, "IME");
        addStateField(statePanel, "Halted");
        addStateField(statePanel, "Stopped");
        addStateField(statePanel, "Speed");
        addStateField(statePanel, "Break");
        addStateField(statePanel, "LCDC");
        addStateField(statePanel, "STAT");
        addStateField(statePanel, "Mode");
        addStateField(statePanel, "LY");
        addStateField(statePanel, "LX");
        addStateField(statePanel, "Cycles");
        addStateField(statePanel, "SCX");
        addStateField(statePanel, "SCY");
        addStateField(statePanel, "WX");
        addStateField(statePanel, "WY");
        addStateField(statePanel, "LYC");

        instructionModel.addColumn("Addr");
        instructionModel.addColumn("Bank");
        instructionModel.addColumn("Bytes");
        instructionModel.addColumn("Instruction");
        instructionTable.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        instructionTable.setRowHeight(22);
        instructionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        instructionTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        instructionTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        instructionTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        instructionTable.getColumnModel().getColumn(3).setPreferredWidth(360);

        panel.add(statePanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(instructionTable), BorderLayout.CENTER);
        return panel;
    }

    private void addStateField(JPanel panel, String name) {
        panel.add(new JLabel(name));
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        stateFields.put(name, field);
        panel.add(field);
    }

    private JPanel breakpointsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();

        JButton pause = new JButton("Pause");
        pause.addActionListener(event -> pauseAction.run());
        JButton resume = new JButton("Resume");
        resume.addActionListener(event -> resumeAction.run());
        JButton add = new JButton("Add PC");
        add.addActionListener(event -> addPcBreakpoint());
        JButton remove = new JButton("Remove Selected");
        remove.addActionListener(event -> removeSelectedPcBreakpoint());

        controls.add(pause);
        controls.add(resume);
        controls.add(new JLabel("PC"));
        controls.add(pcBreakpoint);
        controls.add(add);
        controls.add(remove);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(breakpointList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel memoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();
        memoryRegion.addActionListener(event -> applyMemoryRegion());
        JButton refreshMemory = new JButton("Refresh Memory");
        refreshMemory.addActionListener(event -> refreshMemoryTable());
        controls.add(new JLabel("Region"));
        controls.add(memoryRegion);
        controls.add(new JLabel("Start"));
        controls.add(memoryStart);
        controls.add(new JLabel("Length"));
        controls.add(memoryLength);
        controls.add(refreshMemory);
        panel.add(controls, BorderLayout.NORTH);
        configureMemoryTable();
        panel.add(new JScrollPane(memoryTable), BorderLayout.CENTER);
        return panel;
    }

    private void configureMemoryTable() {
        memoryModel.addColumn("Addr");
        for (int i = 0; i < 16; i++) {
            memoryModel.addColumn(String.format("%X", i));
        }
        memoryTable.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        memoryTable.setRowHeight(22);
        memoryTable.setCellSelectionEnabled(true);
        memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        memoryTable.getColumnModel().getColumn(0).setPreferredWidth(58);
        for (int i = 1; i < memoryTable.getColumnCount(); i++) {
            memoryTable.getColumnModel().getColumn(i).setPreferredWidth(34);
        }
        memoryModel.addTableModelListener(event -> {
            if (!updatingMemoryTable && event.getType() == TableModelEvent.UPDATE && event.getColumn() > 0) {
                writeMemoryCell(event.getFirstRow(), event.getColumn());
            }
        });
        refreshMemoryTable();
    }

    private JPanel imageGrid(ImagePanel first, ImagePanel second) {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JScrollPane(first));
        panel.add(new JScrollPane(second));
        return panel;
    }

    private static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        return area;
    }

    private void show() {
        refresh();
        window.setVisible(true);
    }

    private void refresh() {
        refreshStateFields();
        refreshInstructionTable();
        memoryMapText.setText(memoryMapText());
        refreshBreakpoints();
        tilesBank0.setImage(ppu.debugTileImage(0));
        tilesBank1.setImage(ppu.debugTileImage(1));
        bgMap9800.setImage(ppu.debugTileMapImage(TileMapArea.IN_9800));
        bgMap9C00.setImage(ppu.debugTileMapImage(TileMapArea.IN_9C00));
        bgPalettes.setImage(ppu.debugPaletteImage(false));
        objPalettes.setImage(ppu.debugPaletteImage(true));
    }

    private void refreshStateFields() {
        CpuState cpuState = cpu.saveState();
        Ppu.DebugSnapshot ppuSnapshot = ppu.debugSnapshot();
        setState("PC", "%04X", cpuState.pc());
        setState("SP", "%04X", cpuState.sp());
        setState("AF", "%04X", cpuState.af());
        setState("BC", "%04X", cpuState.bc());
        setState("DE", "%04X", cpuState.de());
        setState("HL", "%04X", cpuState.hl());
        setState("A", "%02X", cpuState.aUnsigned());
        setState("B", "%02X", cpuState.bUnsigned());
        setState("C", "%02X", cpuState.cUnsigned());
        setState("D", "%02X", cpuState.dUnsigned());
        setState("E", "%02X", cpuState.eUnsigned());
        setState("H", "%02X", cpuState.hUnsigned());
        setState("L", "%02X", cpuState.lUnsigned());
        setState("Flags", "%s%s%s%s",
                cpuState.zeroFlag() ? "Z" : "-",
                cpuState.negativeFlag() ? "N" : "-",
                cpuState.halfCarryFlag() ? "H" : "-",
                cpuState.carryFlag() ? "C" : "-");
        setState("IME", "%s", cpuState.ime());
        setState("Halted", "%s", cpuState.halted());
        setState("Stopped", "%s", cpuState.stopped());
        setState("Speed", "%dx", cpuState.speedRate());
        stateFields.get("Break").setText(debugController.breakReason());
        setState("LCDC", "%02X", ppuSnapshot.lcdc());
        setState("STAT", "%02X", ppuSnapshot.stat());
        stateFields.get("Mode").setText(String.valueOf(ppuSnapshot.mode()));
        setState("LY", "%02X", ppuSnapshot.line());
        setState("LX", "%03d", ppuSnapshot.column());
        setState("Cycles", "%03d", ppuSnapshot.cycles());
        setState("SCX", "%02X", ppuSnapshot.scrollX());
        setState("SCY", "%02X", ppuSnapshot.scrollY());
        setState("WX", "%02X", ppuSnapshot.windowX());
        setState("WY", "%02X", ppuSnapshot.windowY());
        setState("LYC", "%02X", ppuSnapshot.lineCompare());
    }

    private void setState(String name, String format, Object... args) {
        stateFields.get(name).setText(String.format(format, args));
    }

    private String cpuSnapshotText() {
        CpuState cpuState = cpu.saveState();
        Ppu.DebugSnapshot ppuSnapshot = ppu.debugSnapshot();
        return String.format("""
                        CPU
                        PC:%04X SP:%04X AF:%04X BC:%04X DE:%04X HL:%04X
                        A:%02X B:%02X C:%02X D:%02X E:%02X H:%02X L:%02X
                        Flags: Z=%s N=%s H=%s C=%s IME=%s halted=%s stopped=%s haltBug=%s speed=%dx
                        Break: %s

                        PPU
                        LCDC:%02X STAT:%02X mode:%s LY:%02X LX:%03d cycles:%03d SCX:%02X SCY:%02X WX:%02X WY:%02X LYC:%02X CGB:%s
                        """,
                cpuState.pc(), cpuState.sp(), cpuState.af(), cpuState.bc(), cpuState.de(), cpuState.hl(),
                cpuState.aUnsigned(), cpuState.bUnsigned(), cpuState.cUnsigned(), cpuState.dUnsigned(), cpuState.eUnsigned(), cpuState.hUnsigned(), cpuState.lUnsigned(),
                cpuState.zeroFlag(), cpuState.negativeFlag(), cpuState.halfCarryFlag(), cpuState.carryFlag(),
                cpuState.ime(), cpuState.halted(), cpuState.stopped(), cpuState.haltBug(), cpuState.speedRate(),
                debugController.breakReason(),
                ppuSnapshot.lcdc(), ppuSnapshot.stat(), ppuSnapshot.mode(), ppuSnapshot.line(), ppuSnapshot.column(),
                ppuSnapshot.cycles(), ppuSnapshot.scrollX(), ppuSnapshot.scrollY(), ppuSnapshot.windowX(), ppuSnapshot.windowY(),
                ppuSnapshot.lineCompare(), ppuSnapshot.cgbMode());
    }

    private void addPcBreakpoint() {
        int pc = parseHex(pcBreakpoint.getText(), -1);
        if (pc < 0 || pc > 0xFFFF) {
            return;
        }
        debugController.addPcBreakpoint(pc);
        refreshBreakpoints();
    }

    private void removeSelectedPcBreakpoint() {
        String selected = breakpointList.getSelectedValue();
        if (selected == null) {
            return;
        }
        debugController.removePcBreakpoint(parseHex(selected, -1));
        refreshBreakpoints();
    }

    private void refreshBreakpoints() {
        breakpointModel.clear();
        for (int breakpoint : debugController.pcBreakpoints()) {
            breakpointModel.addElement(String.format("%04X", breakpoint));
        }
    }

    private String instructionText() {
        StringBuilder builder = new StringBuilder("Instructions near PC\n");
        int address = cpu.getPc();
        for (int i = 0; i < 24; i++) {
            Disassembler.Decoded decoded = disassemblyCache.decode(address);
            builder.append(String.format("%04X: %s%n", address, decoded.text()));
            address = (address + decoded.length()) & 0xFFFF;
        }
        return builder.toString();
    }

    private void refreshInstructionTable() {
        instructionModel.setRowCount(0);
        int pc = cpu.getPc();
        int selectedRow = 0;
        for (Disassembler.Decoded decoded : disassemblyCache.previousInstructions(pc, 12)) {
            addInstructionRow(decoded);
            selectedRow++;
        }
        for (Disassembler.Decoded decoded : disassemblyCache.decodeForward(pc, 28)) {
            addInstructionRow(decoded);
        }
        if (instructionModel.getRowCount() > 0) {
            instructionTable.setRowSelectionInterval(selectedRow, selectedRow);
            instructionTable.scrollRectToVisible(instructionTable.getCellRect(Math.max(0, selectedRow - 4), 0, true));
        }
    }

    private void addInstructionRow(Disassembler.Decoded decoded) {
        instructionModel.addRow(new Object[]{
                String.format("%04X", decoded.address()),
                disassemblyCache.location(decoded.address()),
                decoded.bytes(),
                decoded.instruction()
        });
    }


    private String memoryMapText() {
        StringBuilder builder = new StringBuilder("Runtime memory map\n");
        for (Bus.MemoryMapEntry entry : bus.memoryMap()) {
            builder.append(String.format("%04X-%04X  %s%n", entry.start(), entry.end(), entry.owner()));
        }
        builder.append("\nMemory banks\n");
        for (var bank : bus.memoryBanks()) {
            builder.append(String.format(
                    "%s  current=%d  banks=%d  size=%04X%n",
                    bank.bankName(),
                    bank.currentBank(),
                    bank.bankCount(),
                    bank.bankSize()
            ));
        }
        return builder.toString();
    }

    private void applyMemoryRegion() {
        MemoryRegion selected = (MemoryRegion) memoryRegion.getSelectedItem();
        if (selected == null) {
            return;
        }
        memoryStart.setText(String.format("%04X", selected.start));
        memoryLength.setText(String.format("%04X", selected.length));
        refreshMemoryTable();
    }

    private void refreshMemoryTable() {
        int start = parseHex(memoryStart.getText(), 0xC000) & 0xFFFF;
        int length = Math.max(16, Math.min(parseHex(memoryLength.getText(), 0x0100), 0x10000));
        int rows = (length + 15) / 16;
        updatingMemoryTable = true;
        memoryModel.setRowCount(0);
        for (int row = 0; row < rows; row++) {
            Object[] values = new Object[17];
            int address = (start + row * 16) & 0xFFFF;
            values[0] = String.format("%04X", address);
            for (int column = 0; column < 16; column++) {
                int cellAddress = (address + column) & 0xFFFF;
                values[column + 1] = String.format("%02X", bus.read(cellAddress) & 0xFF);
            }
            memoryModel.addRow(values);
        }
        updatingMemoryTable = false;
    }

    private void writeMemoryCell(int row, int column) {
        int rowAddress = parseHex(String.valueOf(memoryModel.getValueAt(row, 0)), 0);
        int address = (rowAddress + column - 1) & 0xFFFF;
        int byteValue = parseHex(String.valueOf(memoryModel.getValueAt(row, column)), -1);
        if (byteValue < 0 || byteValue > 0xFF) {
            refreshMemoryTable();
            return;
        }
        bus.write(address, (byte) byteValue);
        updatingMemoryTable = true;
        memoryModel.setValueAt(String.format("%02X", byteValue), row, column);
        updatingMemoryTable = false;
    }

    private int parseHex(String text, int fallback) {
        try {
            String normalized = text.trim().replace("0x", "").replace("$", "");
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void dump() {
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            java.nio.file.Files.writeString(new File(target, "debug-snapshot.txt").toPath(),
                    cpuSnapshotText() + "\n\n" + instructionText() + "\n" + memoryMapText());
            java.nio.file.Files.writeString(new File(target, "debug-memory.txt").toPath(), memoryTableText());
            ImageIO.write(ppu.debugTileImage(0), "png", new File(target, "debug-tiles-bank0.png"));
            ImageIO.write(ppu.debugTileImage(1), "png", new File(target, "debug-tiles-bank1.png"));
            ImageIO.write(ppu.debugTileMapImage(TileMapArea.IN_9800), "png", new File(target, "debug-tilemap-9800.png"));
            ImageIO.write(ppu.debugTileMapImage(TileMapArea.IN_9C00), "png", new File(target, "debug-tilemap-9c00.png"));
            ImageIO.write(ppu.debugPaletteImage(false), "png", new File(target, "debug-bg-palettes.png"));
            ImageIO.write(ppu.debugPaletteImage(true), "png", new File(target, "debug-obj-palettes.png"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump debugger files", e);
        }
    }

    private String memoryTableText() {
        StringBuilder builder = new StringBuilder();
        builder.append("        00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n");
        for (int row = 0; row < memoryModel.getRowCount(); row++) {
            builder.append(memoryModel.getValueAt(row, 0)).append("   ");
            for (int column = 1; column < memoryModel.getColumnCount(); column++) {
                builder.append(memoryModel.getValueAt(row, column)).append(' ');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private enum MemoryRegion {
        ROM0(0x0000, 0x4000),
        ROMX(0x4000, 0x4000),
        VRAM(0x8000, 0x2000),
        EXRAM(0xA000, 0x2000),
        WRAM0(0xC000, 0x1000),
        WRAMX(0xD000, 0x1000),
        OAM(0xFE00, 0x00A0),
        IO(0xFF00, 0x0080),
        HRAM(0xFF80, 0x007F);

        private final int start;
        private final int length;

        MemoryRegion(int start, int length) {
            this.start = start;
            this.length = length;
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
