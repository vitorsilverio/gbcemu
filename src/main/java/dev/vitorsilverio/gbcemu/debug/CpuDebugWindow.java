package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.interrupt.InterruptState;
import dev.vitorsilverio.gbcemu.interrupt.InterruptManager;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.cpu.CpuState;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import dev.vitorsilverio.gbcemu.peripherals.SerialState;
import dev.vitorsilverio.gbcemu.peripherals.TimerState;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import dev.vitorsilverio.gbcemu.util.DebugJson;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import java.util.List;
import java.util.Map;

public class CpuDebugWindow {

    public record Target(String name, Cpu cpu, Bus bus, Ppu ppu, DebugController debugController, LinkCable linkCable) {
        @Override
        public String toString() {
            return name;
        }
    }

    private Cpu cpu;
    private Bus bus;
    private Ppu ppu;
    private DebugController debugController;
    private LinkCable linkCable;
    private DisassemblyCache disassemblyCache;
    private final JComboBox<Target> targetSelector;
    private final JFrame window = new JFrame("CPU / Disassembly");
    private final Map<String, JTextField> stateFields = new LinkedHashMap<>();
    private final DefaultTableModel instructionModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable instructionTable = new JTable(instructionModel);
    private final JTextField watchAddress = new JTextField(4);
    private final JTextField watchValue = new JTextField(2);
    private final JCheckBox autoRefresh = new JCheckBox("Auto refresh");
    private final Timer autoRefreshTimer = new Timer(1000, event -> {
        if (autoRefresh.isSelected() && window.isVisible()) {
            refresh();
        }
    });

    public CpuDebugWindow(Cpu cpu, Bus bus, Ppu ppu, DebugController debugController, LinkCable linkCable) {
        this.cpu = cpu;
        this.bus = bus;
        this.ppu = ppu;
        this.debugController = debugController;
        this.linkCable = linkCable;
        this.targetSelector = null;
        this.disassemblyCache = new DisassemblyCache(bus);
        initialize();
        refresh();
        window.setVisible(true);
    }

    public CpuDebugWindow(List<Target> targets) {
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one CPU debug target is required");
        }
        this.targetSelector = new JComboBox<>(targets.toArray(Target[]::new));
        applyTarget(targets.getFirst());
        initialize();
        refresh();
        window.setVisible(true);
    }

    private void initialize() {
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.setMinimumSize(new java.awt.Dimension(840, 560));

        JPanel toolbar = new JPanel();
        if (targetSelector != null) {
            targetSelector.addActionListener(event -> {
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
        JButton step = new JButton("Step");
        step.addActionListener(event -> stepInstruction());
        JButton stepLine = new JButton("Step Line");
        stepLine.addActionListener(event -> stepScanline());
        JButton stepFrame = new JButton("Step Frame");
        stepFrame.addActionListener(event -> stepFrame());
        JButton runHBlank = new JButton("Run HBlank");
        runHBlank.addActionListener(event -> runUntilHBlank());
        JButton runVBlank = new JButton("Run VBlank");
        runVBlank.addActionListener(event -> runUntilVBlank());
        JButton toggleBreakpoint = new JButton("Toggle BP");
        toggleBreakpoint.addActionListener(event -> toggleSelectedInstructionBreakpoint());
        JButton dump = new JButton("Dump");
        dump.addActionListener(event -> dump());
        JButton dumpJson = new JButton("Dump JSON");
        dumpJson.addActionListener(event -> dumpJson());
        toolbar.add(refresh);
        toolbar.add(autoRefresh);
        toolbar.add(step);
        toolbar.add(stepLine);
        toolbar.add(stepFrame);
        toolbar.add(runHBlank);
        toolbar.add(runVBlank);
        toolbar.add(toggleBreakpoint);
        toolbar.add(new JLabel("Watch"));
        watchAddress.setToolTipText("Address, for example C000");
        watchValue.setToolTipText("Optional byte value, for example FF");
        toolbar.add(watchAddress);
        toolbar.add(new JLabel("="));
        toolbar.add(watchValue);
        JButton watchRead = new JButton("Read");
        watchRead.addActionListener(event -> addWatchpoint(DebugController.AccessType.READ));
        JButton watchWrite = new JButton("Write");
        watchWrite.addActionListener(event -> addWatchpoint(DebugController.AccessType.WRITE));
        JButton clearWatch = new JButton("Clear Watch");
        clearWatch.addActionListener(event -> clearWatchpoints());
        toolbar.add(watchRead);
        toolbar.add(watchWrite);
        toolbar.add(clearWatch);
        toolbar.add(dump);
        toolbar.add(dumpJson);

        window.add(toolbar, BorderLayout.NORTH);
        window.add(statePanel(), BorderLayout.WEST);
        window.add(new JScrollPane(instructionTable), BorderLayout.CENTER);
        configureInstructionTable();
        window.pack();
        window.setLocationRelativeTo(null);
        autoRefreshTimer.start();
    }

    private void applyTarget(Target target) {
        this.cpu = target.cpu();
        this.bus = target.bus();
        this.ppu = target.ppu();
        this.debugController = target.debugController();
        this.linkCable = target.linkCable();
        this.disassemblyCache = new DisassemblyCache(bus);
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
        addStateField(panel, "Watch");
        addStateField(panel, "LCDC");
        addStateField(panel, "STAT");
        addStateField(panel, "Mode");
        addStateField(panel, "LY");
        addStateField(panel, "LX");
        addStateField(panel, "Cycles");
        addStateField(panel, "Link");
        addStateField(panel, "LinkRole");
        addStateField(panel, "LinkCollision");
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
        stateFields.get("Watch").setText(watchpointsText());
        setState("LCDC", "%02X", ppuSnapshot.lcdc());
        setState("STAT", "%02X", ppuSnapshot.stat());
        stateFields.get("Mode").setText(String.valueOf(ppuSnapshot.mode()));
        setState("LY", "%02X", ppuSnapshot.line());
        setState("LX", "%03d", ppuSnapshot.column());
        setState("Cycles", "%03d", ppuSnapshot.cycles());
        refreshLinkFields();
    }

    private void refreshLinkFields() {
        if (linkCable == null) {
            stateFields.get("Link").setText("false");
            stateFields.get("LinkRole").setText("-");
            stateFields.get("LinkCollision").setText("false");
            return;
        }
        stateFields.get("Link").setText(Boolean.toString(linkCable.isActive()));
        stateFields.get("LinkRole").setText(linkCable.isEffectiveMaster() ? "master" : "slave");
        stateFields.get("LinkCollision").setText(Boolean.toString(linkCable.hasDualMasterCollision()));
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

    private void stepInstruction() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        debugController.requestInstructionStep();
    }

    private void stepScanline() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        debugController.requestScanlineStep();
    }

    private void stepFrame() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        debugController.requestFrameStep();
    }

    private void runUntilHBlank() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        debugController.requestRunUntilHBlank();
    }

    private void runUntilVBlank() {
        debugController.ignorePcBreakpointOnce(cpu.getPc());
        debugController.requestRunUntilVBlank();
    }

    private void addWatchpoint(DebugController.AccessType accessType) {
        int address = parseHex(watchAddress.getText(), -1);
        if (address < 0 || address > 0xFFFF) {
            return;
        }
        int value = watchValue.getText().isBlank() ? -1 : parseHex(watchValue.getText(), -1);
        if (value > 0xFF) {
            return;
        }
        debugController.addWatchpoint(accessType, address, value < 0 ? null : value);
        refreshStateFields();
    }

    private void clearWatchpoints() {
        debugController.clearWatchpoints();
        refreshStateFields();
    }

    private String watchpointsText() {
        java.util.List<DebugController.Watchpoint> watchpoints = debugController.watchpoints();
        if (watchpoints.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < watchpoints.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(watchpoints.get(index).description());
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

    private void dumpJson() {
        DebugJson.writeTargetFile("debug-cpu-window.json", dumpJsonText(), "Failed to dump CPU debugger JSON");
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

    private String dumpJsonText() {
        CpuState cpuState = cpu.saveState();
        Ppu.DebugSnapshot ppuSnapshot = ppu.debugSnapshot();
        InterruptState interruptState = bus.findMemorySpace(InterruptManager.class)
                .map(InterruptManager::saveState)
                .orElse(new InterruptState((byte) 0, (byte) 0));
        TimerState timerState = bus.findMemorySpace(dev.vitorsilverio.gbcemu.peripherals.Timer.class)
                .map(dev.vitorsilverio.gbcemu.peripherals.Timer::saveState)
                .orElse(new TimerState(0, (byte) 0, (byte) 0, (byte) 0, 0));
        SerialState serialState = bus.findMemorySpace(Serial.class)
                .map(Serial::saveState)
                .orElse(new SerialState(0, 0, 0, 0, ""));
        Serial serial = bus.findMemorySpace(Serial.class).orElse(null);
        Cart cart = bus.findMemorySpace(Cart.class).orElse(null);
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"cpu\": {\n");
        DebugJson.appendHex(builder, "pc", cpuState.pc(), true, 4, 4);
        DebugJson.appendHex(builder, "sp", cpuState.sp(), true, 4, 4);
        DebugJson.appendHex(builder, "af", cpuState.af(), true, 4, 4);
        DebugJson.appendHex(builder, "bc", cpuState.bc(), true, 4, 4);
        DebugJson.appendHex(builder, "de", cpuState.de(), true, 4, 4);
        DebugJson.appendHex(builder, "hl", cpuState.hl(), true, 4, 4);
        DebugJson.appendHex(builder, "a", cpuState.aUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "b", cpuState.bUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "c", cpuState.cUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "d", cpuState.dUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "e", cpuState.eUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "h", cpuState.hUnsigned(), true, 4, 2);
        DebugJson.appendHex(builder, "l", cpuState.lUnsigned(), true, 4, 2);
        DebugJson.appendBoolean(builder, "zeroFlag", cpuState.zeroFlag(), true, 4);
        DebugJson.appendBoolean(builder, "negativeFlag", cpuState.negativeFlag(), true, 4);
        DebugJson.appendBoolean(builder, "halfCarryFlag", cpuState.halfCarryFlag(), true, 4);
        DebugJson.appendBoolean(builder, "carryFlag", cpuState.carryFlag(), true, 4);
        DebugJson.appendBoolean(builder, "ime", cpuState.ime(), true, 4);
        DebugJson.appendBoolean(builder, "halted", cpuState.halted(), true, 4);
        DebugJson.appendBoolean(builder, "stopped", cpuState.stopped(), true, 4);
        DebugJson.appendNumber(builder, "speedRate", cpuState.speedRate(), false, 4);
        builder.append("  },\n");
        builder.append("  \"ppu\": {\n");
        DebugJson.appendBoolean(builder, "cgbMode", ppuSnapshot.cgbMode(), true, 4);
        DebugJson.appendHex(builder, "lcdc", ppuSnapshot.lcdc(), true, 4, 2);
        DebugJson.appendHex(builder, "stat", ppuSnapshot.stat(), true, 4, 2);
        DebugJson.appendString(builder, "mode", String.valueOf(ppuSnapshot.mode()), true, 4);
        DebugJson.appendNumber(builder, "line", ppuSnapshot.line(), true, 4);
        DebugJson.appendNumber(builder, "column", ppuSnapshot.column(), true, 4);
        DebugJson.appendNumber(builder, "cycles", ppuSnapshot.cycles(), true, 4);
        DebugJson.appendNumber(builder, "scrollX", ppuSnapshot.scrollX(), true, 4);
        DebugJson.appendNumber(builder, "scrollY", ppuSnapshot.scrollY(), true, 4);
        DebugJson.appendNumber(builder, "windowX", ppuSnapshot.windowX(), true, 4);
        DebugJson.appendNumber(builder, "windowY", ppuSnapshot.windowY(), true, 4);
        DebugJson.appendHex(builder, "lineCompare", ppuSnapshot.lineCompare(), false, 4, 2);
        builder.append("  },\n");
        builder.append("  \"interrupts\": {\n");
        DebugJson.appendHex(builder, "ie", interruptState.ieReg() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "if", interruptState.ifReg() & 0xFF, true, 4, 2);
        DebugJson.appendString(builder, "pending", bus.getPendingInterrupt().map(Enum::name).orElse(""), false, 4);
        builder.append("  },\n");
        builder.append("  \"timer\": {\n");
        DebugJson.appendNumber(builder, "systemCounter", timerState.systemCounter(), true, 4);
        DebugJson.appendHex(builder, "div", (timerState.systemCounter() >> 8) & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "tima", timerState.timerCounter() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "tma", timerState.timerModulo() & 0xFF, true, 4, 2);
        DebugJson.appendHex(builder, "tac", timerState.timerControl() & 0xFF, true, 4, 2);
        DebugJson.appendNumber(builder, "overflowDelay", timerState.overflowDelay(), false, 4);
        builder.append("  },\n");
        builder.append("  \"serial\": {\n");
        DebugJson.appendHex(builder, "sb", serialState.sb(), true, 4, 2);
        DebugJson.appendHex(builder, "sc", serialState.sc(), true, 4, 2);
        DebugJson.appendBoolean(builder, "transferActive", serial != null && serial.isTransferActive(), true, 4);
        DebugJson.appendBoolean(builder, "internalClock", serial != null && serial.isInternalClockSelected(), true, 4);
        DebugJson.appendBoolean(builder, "fastClock", serial != null && serial.isFastClockSelected(), true, 4);
        DebugJson.appendBoolean(builder, "masterWaitingResponse", serial != null && serial.isMasterWaitingResponse(), true, 4);
        DebugJson.appendNumber(builder, "transferCyclesRemaining", serialState.transferCyclesRemaining(), true, 4);
        DebugJson.appendHex(builder, "outgoingByte", serialState.outgoingByte(), true, 4, 2);
        DebugJson.appendHex(builder, "lastCompletedOutgoingByte", serial == null ? 0xFF : serial.lastCompletedOutgoingByte(), true, 4, 2);
        DebugJson.appendHex(builder, "lastCompletedIncomingByte", serial == null ? 0xFF : serial.lastCompletedIncomingByte(), true, 4, 2);
        DebugJson.appendLong(builder, "completedTransfers", serial == null ? 0L : serial.completedTransfers(), true, 4);
        DebugJson.appendString(builder, "pendingText", serialState.pendingText(), true, 4);
        DebugJson.appendString(builder, "transcript", serial == null ? "" : serial.transcript(), true, 4);
        appendSerialTransferHistoryJson(builder, serial, 4);
        builder.append("  },\n");
        appendLinkJson(builder);
        appendCartJson(builder, cart);
        appendMemoryBanksJson(builder);
        DebugJson.appendString(builder, "breakReason", debugController.breakReason(), true, 2);
        appendWatchpointsJson(builder);
        builder.append("  \"disassembly\": [\n");
        for (int row = 0; row < instructionModel.getRowCount(); row++) {
            builder.append("    {\n");
            DebugJson.appendString(builder, "address", String.valueOf(instructionModel.getValueAt(row, 0)), true, 6);
            DebugJson.appendBoolean(builder, "breakpoint", "*".equals(String.valueOf(instructionModel.getValueAt(row, 1))), true, 6);
            DebugJson.appendString(builder, "bank", String.valueOf(instructionModel.getValueAt(row, 2)), true, 6);
            DebugJson.appendString(builder, "bytes", String.valueOf(instructionModel.getValueAt(row, 3)), true, 6);
            DebugJson.appendString(builder, "instruction", String.valueOf(instructionModel.getValueAt(row, 4)), false, 6);
            builder.append("    }");
            if (row < instructionModel.getRowCount() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendLinkJson(StringBuilder builder) {
        builder.append("  \"link\": ");
        if (linkCable == null) {
            builder.append("null,\n");
            return;
        }
        var local = linkCable.localState();
        var peer = linkCable.peerState();
        builder.append("{\n");
        DebugJson.appendBoolean(builder, "active", linkCable.isActive(), true, 4);
        DebugJson.appendBoolean(builder, "connected", linkCable.isConnected(), true, 4);
        DebugJson.appendBoolean(builder, "hosting", linkCable.isHosting(), true, 4);
        DebugJson.appendBoolean(builder, "effectiveMaster", linkCable.isEffectiveMaster(), true, 4);
        DebugJson.appendBoolean(builder, "dualMasterCollision", linkCable.hasDualMasterCollision(), true, 4);
        DebugJson.appendLong(builder, "clockPulsesSent", linkCable.clockPulsesSent(), true, 4);
        DebugJson.appendLong(builder, "clockPulsesReceived", linkCable.clockPulsesReceived(), true, 4);
        DebugJson.appendLong(builder, "clockResponsesSent", linkCable.clockResponsesSent(), true, 4);
        DebugJson.appendLong(builder, "clockResponsesReceived", linkCable.clockResponsesReceived(), true, 4);
        DebugJson.appendLong(builder, "clockPulsesDiscarded", linkCable.clockPulsesDiscarded(), true, 4);
        DebugJson.appendNumber(builder, "pendingIncomingClockCount", linkCable.pendingIncomingClockCount(), true, 4);
        DebugJson.appendBoolean(builder, "pendingInternalClockByte", linkCable.hasPendingInternalClockByte(), true, 4);
        DebugJson.appendBoolean(builder, "inFlightTransfer", linkCable.hasInFlightTransfer(), true, 4);
        DebugJson.appendHex(builder, "lastClockPulseSent", linkCable.lastClockPulseSent(), true, 4, 2);
        DebugJson.appendHex(builder, "lastClockPulseReceived", linkCable.lastClockPulseReceived(), true, 4, 2);
        DebugJson.appendHex(builder, "lastClockResponseSent", linkCable.lastClockResponseSent(), true, 4, 2);
        DebugJson.appendHex(builder, "lastClockResponseReceived", linkCable.lastClockResponseReceived(), true, 4, 2);
        DebugJson.appendBoolean(builder, "localTransferActive", local.transferActive(), true, 4);
        DebugJson.appendBoolean(builder, "localInternalClock", local.internalClock(), true, 4);
        DebugJson.appendBoolean(builder, "localMasterWaitingResponse", local.masterWaitingResponse(), true, 4);
        DebugJson.appendHex(builder, "localSc", local.sc(), true, 4, 2);
        DebugJson.appendHex(builder, "localOutgoingByte", local.outgoingByte(), true, 4, 2);
        DebugJson.appendBoolean(builder, "peerTransferActive", peer.transferActive(), true, 4);
        DebugJson.appendBoolean(builder, "peerInternalClock", peer.internalClock(), true, 4);
        DebugJson.appendHex(builder, "peerSc", peer.sc(), true, 4, 2);
        DebugJson.appendHex(builder, "peerOutgoingByte", peer.outgoingByte(), false, 4, 2);
        builder.append("  },\n");
    }

    private void appendSerialTransferHistoryJson(StringBuilder builder, Serial serial, int indent) {
        DebugJson.appendIndent(builder, indent);
        builder.append("\"recentTransfers\": [\n");
        int count = serial == null ? 0 : serial.transferHistoryCount();
        for (int index = 0; index < count; index++) {
            DebugJson.appendIndent(builder, indent + 2);
            builder.append("{\n");
            DebugJson.appendHex(builder, "out", serial.transferHistoryOutgoing(index), true, indent + 4, 2);
            DebugJson.appendHex(builder, "in", serial.transferHistoryIncoming(index), true, indent + 4, 2);
            DebugJson.appendBoolean(builder, "internalClock", serial.transferHistoryInternalClock(index), true, indent + 4);
            DebugJson.appendBoolean(builder, "completedAsMaster", serial.transferHistoryCompletedAsMaster(index), false, indent + 4);
            DebugJson.appendIndent(builder, indent + 2);
            builder.append('}');
            if (index < count - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        DebugJson.appendIndent(builder, indent);
        builder.append("]\n");
    }

    private void appendCartJson(StringBuilder builder, Cart cart) {
        builder.append("  \"cart\": ");
        if (cart == null) {
            builder.append("null,\n");
            return;
        }
        CartState cartState = cart.saveState();
        builder.append("{\n");
        DebugJson.appendObject(builder, "properties", cart.debugProperties(), true, 4);
        builder.append("    \"externalRam\": {\n");
        DebugJson.appendNumber(builder, "size", cartState.externalRam().data().length, true, 6);
        DebugJson.appendNumber(builder, "currentBank", cartState.externalRam().currentBank(), false, 6);
        builder.append("    },\n");
        DebugJson.appendObject(builder, "mapperState", cartState.mapperState(), false, 4);
        builder.append("  },\n");
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
            DebugJson.appendString(builder, "currentBankSample", DebugJson.memoryBankSample(bank, 32), false, 6);
            builder.append("    }");
            if (index < banks.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
    }

    private void appendWatchpointsJson(StringBuilder builder) {
        builder.append("  \"watchpoints\": [\n");
        java.util.List<DebugController.Watchpoint> watchpoints = debugController.watchpoints();
        for (int index = 0; index < watchpoints.size(); index++) {
            DebugController.Watchpoint watchpoint = watchpoints.get(index);
            builder.append("    {\n");
            DebugJson.appendString(builder, "type", watchpoint.accessType().name(), true, 6);
            DebugJson.appendHex(builder, "address", watchpoint.address(), true, 6, 4);
            if (watchpoint.value() == null) {
                DebugJson.appendString(builder, "value", "", false, 6);
            } else {
                DebugJson.appendHex(builder, "value", watchpoint.value(), false, 6, 2);
            }
            builder.append("    }");
            if (index < watchpoints.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
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
