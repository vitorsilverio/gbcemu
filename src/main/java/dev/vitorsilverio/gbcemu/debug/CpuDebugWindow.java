package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.CpuState;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.ppu.Ppu;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CpuDebugWindow {

    private final Cpu cpu;
    private final Ppu ppu;
    private final DebugController debugController;
    private final DisassemblyCache disassemblyCache;
    private final JFrame window = new JFrame("CPU / Disassembly");
    private final Map<String, JTextField> stateFields = new LinkedHashMap<>();
    private final DefaultTableModel instructionModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable instructionTable = new JTable(instructionModel);

    public CpuDebugWindow(Cpu cpu, Bus bus, Ppu ppu, DebugController debugController) {
        this.cpu = cpu;
        this.ppu = ppu;
        this.debugController = debugController;
        this.disassemblyCache = new DisassemblyCache(bus);
        initialize();
        refresh();
        window.setVisible(true);
    }

    private void initialize() {
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.setMinimumSize(new java.awt.Dimension(840, 560));

        JPanel toolbar = new JPanel();
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        JButton toggleBreakpoint = new JButton("Toggle BP");
        toggleBreakpoint.addActionListener(event -> toggleSelectedInstructionBreakpoint());
        JButton dump = new JButton("Dump");
        dump.addActionListener(event -> dump());
        toolbar.add(refresh);
        toolbar.add(toggleBreakpoint);
        toolbar.add(dump);

        window.add(toolbar, BorderLayout.NORTH);
        window.add(statePanel(), BorderLayout.WEST);
        window.add(new JScrollPane(instructionTable), BorderLayout.CENTER);
        configureInstructionTable();
        window.pack();
        window.setLocationRelativeTo(null);
        new Timer(250, event -> {
            if (window.isVisible()) {
                refresh();
            }
        }).start();
    }

    private JPanel statePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        addStateField(panel, "PC");
        addStateField(panel, "SP");
        addStateField(panel, "AF");
        addStateField(panel, "BC");
        addStateField(panel, "DE");
        addStateField(panel, "HL");
        addStateField(panel, "A");
        addStateField(panel, "B");
        addStateField(panel, "C");
        addStateField(panel, "D");
        addStateField(panel, "E");
        addStateField(panel, "H");
        addStateField(panel, "L");
        addStateField(panel, "Flags");
        addStateField(panel, "IME");
        addStateField(panel, "Halted");
        addStateField(panel, "Stopped");
        addStateField(panel, "Speed");
        addStateField(panel, "Break");
        addStateField(panel, "LCDC");
        addStateField(panel, "STAT");
        addStateField(panel, "Mode");
        addStateField(panel, "LY");
        addStateField(panel, "LX");
        addStateField(panel, "Cycles");
        return panel;
    }

    private void addStateField(JPanel panel, String name) {
        panel.add(new JLabel(name));
        JTextField field = new JTextField(10);
        field.setEditable(false);
        field.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        stateFields.put(name, field);
        panel.add(field);
    }

    private void configureInstructionTable() {
        instructionModel.addColumn("Addr");
        instructionModel.addColumn("BP");
        instructionModel.addColumn("Bank");
        instructionModel.addColumn("Bytes");
        instructionModel.addColumn("Instruction");
        instructionTable.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        instructionTable.setRowHeight(22);
        instructionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        instructionTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        instructionTable.getColumnModel().getColumn(1).setPreferredWidth(34);
        instructionTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        instructionTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        instructionTable.getColumnModel().getColumn(4).setPreferredWidth(400);
        instructionTable.setDefaultRenderer(Object.class, new InstructionCellRenderer());
        instructionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && instructionTable.getSelectedRow() >= 0) {
                    toggleSelectedInstructionBreakpoint();
                }
            }
        });
    }

    private void refresh() {
        refreshStateFields();
        refreshInstructionTable();
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
    }

    private void setState(String name, String format, Object... args) {
        stateFields.get(name).setText(String.format(format, args));
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
                debugController.hasPcBreakpoint(decoded.address()) ? "*" : "",
                disassemblyCache.location(decoded.address()),
                decoded.bytes(),
                decoded.instruction()
        });
    }

    private void toggleSelectedInstructionBreakpoint() {
        int row = instructionTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int address = parseHex(String.valueOf(instructionModel.getValueAt(row, 0)), -1);
        if (address < 0 || address > 0xFFFF) {
            return;
        }
        debugController.togglePcBreakpoint(address);
        refreshInstructionTable();
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
            java.nio.file.Files.writeString(new File(target, "debug-cpu-window.txt").toPath(), dumpText());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump CPU debugger", e);
        }
    }

    private String dumpText() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, JTextField> entry : stateFields.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue().getText()).append('\n');
        }
        builder.append('\n');
        for (int row = 0; row < instructionModel.getRowCount(); row++) {
            builder.append(instructionModel.getValueAt(row, 0)).append(' ')
                    .append(instructionModel.getValueAt(row, 1)).append(' ')
                    .append(instructionModel.getValueAt(row, 2)).append(' ')
                    .append(instructionModel.getValueAt(row, 3)).append(' ')
                    .append(instructionModel.getValueAt(row, 4)).append('\n');
        }
        return builder.toString();
    }

    private class InstructionCellRenderer extends DefaultTableCellRenderer {
        private final Color breakpointBackground = new Color(80, 24, 24);
        private final Color breakpointForeground = new Color(255, 220, 220);

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (selected) {
                return component;
            }
            boolean hasBreakpoint = "*".equals(String.valueOf(table.getValueAt(row, 1)));
            if (hasBreakpoint) {
                component.setBackground(breakpointBackground);
                component.setForeground(breakpointForeground);
            } else {
                component.setBackground(table.getBackground());
                component.setForeground(table.getForeground());
            }
            return component;
        }
    }
}
