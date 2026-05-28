package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.cartridge.CartState;
import dev.vitorsilverio.gbcemu.debug.CartDebugInterface;
import dev.vitorsilverio.gbcemu.debug.DebugCartInterface;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;
import dev.vitorsilverio.gbcemu.util.DebugJson;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CartDebugWindow {

    public record Target(String name, DebugCartInterface cart) {
        @Override
        public String toString() {
            return name;
        }
    }

    private DebugCartInterface cart;
    private final JComboBox<Target> targetSelector;
    private final Supplier<List<Target>> targetSupplier;
    private final JFrame frame = new JFrame("Cart / MBC Debug");
    private final DefaultTableModel propertiesModel = tableModel("Property", "Value");
    private final DefaultTableModel banksModel = tableModel("Memory", "Current", "Banks", "Bank Size");
    private final JTable propertiesTable = new JTable(propertiesModel);
    private final JTable banksTable = new JTable(banksModel);
    private final JTextArea headerText = new JTextArea();
    private final JCheckBox autoRefresh = new JCheckBox("Auto refresh");
    private boolean updatingTargetSelector;
    private final Timer autoRefreshTimer = new Timer(1000, event -> {
        if (autoRefresh.isSelected() && frame.isVisible()) {
            refresh();
        }
    });

    public CartDebugWindow(dev.vitorsilverio.gbcemu.cartridge.Cart cart) {
        this.cart = new CartDebugInterface(cart);
        this.targetSelector = null;
        this.targetSupplier = null;
        initialize();
        refresh();
        frame.setVisible(true);
    }

    public CartDebugWindow(List<Target> targets) {
        this(() -> targets);
    }

    public CartDebugWindow(Supplier<List<Target>> targetSupplier) {
        List<Target> targets = targetSupplier.get();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one cart debug target is required");
        }
        this.targetSupplier = targetSupplier;
        this.targetSelector = new JComboBox<>(targets.toArray(Target[]::new));
        applyTarget(targets.getFirst());
        initialize();
        refresh();
        frame.setVisible(true);
    }

    private void initialize() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new java.awt.Dimension(760, 520));

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
        frame.add(toolbar, BorderLayout.NORTH);

        headerText.setEditable(false);
        headerText.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));

        JSplitPane tables = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(propertiesTable),
                new JScrollPane(banksTable)
        );
        tables.setResizeWeight(0.7);
        JSplitPane content = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tables, new JScrollPane(headerText));
        content.setResizeWeight(0.62);
        frame.add(content, BorderLayout.CENTER);

        configureTable(propertiesTable);
        configureTable(banksTable);
        frame.pack();
        frame.setLocationRelativeTo(null);
        autoRefreshTimer.start();
    }

    private void applyTarget(Target target) {
        this.cart = target.cart();
    }

    private void refresh() {
        refreshTargets();
        propertiesModel.setRowCount(0);
        for (Map.Entry<String, String> entry : cart.properties().entrySet()) {
            propertiesModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        banksModel.setRowCount(0);
        for (MemoryBank bank : cart.memoryBanks()) {
            banksModel.addRow(new Object[]{
                    bank.bankName(),
                    bank.currentBank(),
                    bank.bankCount(),
                    String.format("%04X", bank.bankSize())
            });
        }
        headerText.setText(cart.header().toString());
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

    private void dump() {
        File target = DebugJson.debugDirectory();
        try {
            java.nio.file.Files.writeString(new File(target, "debug-cart-window.txt").toPath(), dumpText());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump cart debugger", e);
        }
    }

    private void dumpJson() {
        DebugJson.writeTargetFile("debug-cart-window.json", dumpJsonText(), "Failed to dump cart debugger JSON");
    }

    private String dumpText() {
        StringBuilder builder = new StringBuilder("Cart / MBC\n");
        for (int row = 0; row < propertiesModel.getRowCount(); row++) {
            builder.append(propertiesModel.getValueAt(row, 0))
                    .append(": ")
                    .append(propertiesModel.getValueAt(row, 1))
                    .append('\n');
        }
        builder.append("\nBanks\n");
        for (int row = 0; row < banksModel.getRowCount(); row++) {
            builder.append(banksModel.getValueAt(row, 0))
                    .append(" current=")
                    .append(banksModel.getValueAt(row, 1))
                    .append(" banks=")
                    .append(banksModel.getValueAt(row, 2))
                    .append(" size=")
                    .append(banksModel.getValueAt(row, 3))
                    .append('\n');
        }
        builder.append("\nHeader\n").append(headerText.getText());
        return builder.toString();
    }

    private String dumpJsonText() {
        CartState state = cart.state();
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        DebugJson.appendObject(builder, "properties", cart.properties(), true, 2);
        builder.append("  \"externalRam\": {\n");
        DebugJson.appendNumber(builder, "size", state.externalRam().data().length, true, 4);
        DebugJson.appendNumber(builder, "currentBank", state.externalRam().currentBank(), false, 4);
        builder.append("  },\n");
        DebugJson.appendObject(builder, "mapperState", state.mapperState(), true, 2);
        appendBanksJson(builder);
        DebugJson.appendString(builder, "headerText", headerText.getText(), false, 2);
        builder.append("}\n");
        return builder.toString();
    }

    private void appendBanksJson(StringBuilder builder) {
        builder.append("  \"banks\": [\n");
        java.util.List<MemoryBank> banks = cart.memoryBanks();
        for (int index = 0; index < banks.size(); index++) {
            MemoryBank bank = banks.get(index);
            builder.append("    {\n");
            DebugJson.appendString(builder, "name", bank.bankName(), true, 6);
            DebugJson.appendNumber(builder, "currentBank", bank.currentBank(), true, 6);
            DebugJson.appendNumber(builder, "bankCount", bank.bankCount(), true, 6);
            DebugJson.appendNumber(builder, "bankSize", bank.bankSize(), true, 6);
            DebugJson.appendString(builder, "currentBankSample", DebugJson.memoryBankSample(bank, 256), false, 6);
            builder.append("    }");
            if (index < banks.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
    }

    private static DefaultTableModel tableModel(String... columns) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (String column : columns) {
            model.addColumn(column);
        }
        return model;
    }

    private static void configureTable(JTable table) {
        table.setRowHeight(22);
        table.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        table.getTableHeader().setReorderingAllowed(false);
    }
}
