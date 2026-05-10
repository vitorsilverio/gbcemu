package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.util.DebugJson;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

public class MemoryDebugWindow {

    private final Bus bus;
    private final JFrame frame = new JFrame("Memory Debug");
    private final JTextArea memoryMapText = new JTextArea();
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
    private boolean updatingMemoryTable;

    public MemoryDebugWindow(Bus bus) {
        this.bus = bus;
        initializeWindow();
    }

    private void initializeWindow() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 560));
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildControls(), BorderLayout.NORTH);
        content.add(new JScrollPane(memoryTable), BorderLayout.CENTER);
        content.add(buildMemoryMap(), BorderLayout.EAST);

        configureMemoryTable();
        frame.setContentPane(content);
        refresh();
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel buildControls() {
        JPanel controls = new JPanel();
        memoryRegion.addActionListener(event -> applyMemoryRegion());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        JButton dump = new JButton("Dump");
        dump.addActionListener(event -> dumpMemory());
        JButton dumpJson = new JButton("Dump JSON");
        dumpJson.addActionListener(event -> dumpMemoryJson());

        controls.add(new JLabel("Region"));
        controls.add(memoryRegion);
        controls.add(new JLabel("Start"));
        controls.add(memoryStart);
        controls.add(new JLabel("Length"));
        controls.add(memoryLength);
        controls.add(refresh);
        controls.add(dump);
        controls.add(dumpJson);
        return controls;
    }

    private JScrollPane buildMemoryMap() {
        memoryMapText.setEditable(false);
        memoryMapText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        memoryMapText.setRows(18);
        memoryMapText.setColumns(30);
        return new JScrollPane(memoryMapText);
    }

    private void configureMemoryTable() {
        memoryModel.addColumn("Addr");
        for (int i = 0; i < 16; i++) {
            memoryModel.addColumn(String.format("%X", i));
        }
        memoryTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
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
    }

    private void refresh() {
        refreshMemoryTable();
        memoryMapText.setText(memoryMapText());
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

    private void dumpMemory() {
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            java.nio.file.Files.writeString(new File(target, "debug-memory-window.txt").toPath(),
                    memoryMapText() + "\n" + memoryTableText());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump memory debugger file", e);
        }
    }

    private void dumpMemoryJson() {
        DebugJson.writeTargetFile("debug-memory-window.json", memoryJsonText(), "Failed to dump memory debugger JSON file");
    }

    private String memoryJsonText() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"region\": {\n");
        DebugJson.appendString(builder, "name", String.valueOf(memoryRegion.getSelectedItem()), true, 4);
        DebugJson.appendString(builder, "start", String.format("%04X", parseHex(memoryStart.getText(), 0xC000) & 0xFFFF), true, 4);
        DebugJson.appendNumber(builder, "length", Math.max(16, Math.min(parseHex(memoryLength.getText(), 0x0100), 0x10000)), false, 4);
        builder.append("  },\n");
        appendMemoryMapJson(builder);
        appendMemoryBanksJson(builder);
        appendVisibleMemoryJson(builder);
        builder.append("}\n");
        return builder.toString();
    }

    private void appendMemoryMapJson(StringBuilder builder) {
        builder.append("  \"memoryMap\": [\n");
        java.util.List<Bus.MemoryMapEntry> entries = bus.memoryMap();
        for (int index = 0; index < entries.size(); index++) {
            Bus.MemoryMapEntry entry = entries.get(index);
            builder.append("    {\n");
            DebugJson.appendString(builder, "start", String.format("%04X", entry.start()), true, 6);
            DebugJson.appendString(builder, "end", String.format("%04X", entry.end()), true, 6);
            DebugJson.appendString(builder, "owner", entry.owner(), false, 6);
            builder.append("    }");
            if (index < entries.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
    }

    private void appendMemoryBanksJson(StringBuilder builder) {
        builder.append("  \"memoryBanks\": [\n");
        java.util.List<MemoryBank> banks = bus.memoryBanks();
        for (int index = 0; index < banks.size(); index++) {
            MemoryBank bank = banks.get(index);
            builder.append("    {\n");
            DebugJson.appendString(builder, "name", bank.bankName(), true, 6);
            DebugJson.appendNumber(builder, "bankCount", bank.bankCount(), true, 6);
            DebugJson.appendNumber(builder, "bankSize", bank.bankSize(), true, 6);
            DebugJson.appendNumber(builder, "currentBank", bank.currentBank(), true, 6);
            DebugJson.appendString(builder, "currentBankSample", DebugJson.memoryBankSample(bank, 256), false, 6);
            builder.append("    }");
            if (index < banks.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
    }

    private void appendVisibleMemoryJson(StringBuilder builder) {
        builder.append("  \"visibleMemory\": [\n");
        for (int row = 0; row < memoryModel.getRowCount(); row++) {
            builder.append("    {\n");
            DebugJson.appendString(builder, "address", String.valueOf(memoryModel.getValueAt(row, 0)), true, 6);
            DebugJson.appendString(builder, "bytes", visibleRowBytes(row), false, 6);
            builder.append("    }");
            if (row < memoryModel.getRowCount() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
    }

    private String visibleRowBytes(int row) {
        StringBuilder builder = new StringBuilder();
        for (int column = 1; column < memoryModel.getColumnCount(); column++) {
            if (column > 1) {
                builder.append(' ');
            }
            builder.append(memoryModel.getValueAt(row, column));
        }
        return builder.toString();
    }

    private int parseHex(String text, int fallback) {
        try {
            String normalized = text.trim().replace("0x", "").replace("$", "");
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
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
}
