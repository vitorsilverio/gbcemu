package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.memory.MemoryBank;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CartDebugWindow {

    private final Cart cart;
    private final JFrame frame = new JFrame("Cart / MBC Debug");
    private final DefaultTableModel propertiesModel = tableModel("Property", "Value");
    private final DefaultTableModel banksModel = tableModel("Memory", "Current", "Banks", "Bank Size");
    private final JTable propertiesTable = new JTable(propertiesModel);
    private final JTable banksTable = new JTable(banksModel);
    private final JTextArea headerText = new JTextArea();

    public CartDebugWindow(Cart cart) {
        this.cart = cart;
        initialize();
        refresh();
        frame.setVisible(true);
    }

    private void initialize() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new java.awt.Dimension(760, 520));

        JPanel toolbar = new JPanel();
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        JButton dump = new JButton("Dump");
        dump.addActionListener(event -> dump());
        toolbar.add(refresh);
        toolbar.add(dump);
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
        new Timer(500, event -> {
            if (frame.isVisible()) {
                refresh();
            }
        }).start();
    }

    private void refresh() {
        propertiesModel.setRowCount(0);
        for (Map.Entry<String, String> entry : cart.debugProperties().entrySet()) {
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
        headerText.setText(cart.getHeader().toString());
    }

    private void dump() {
        File target = new File("target");
        if (!target.exists()) {
            target.mkdirs();
        }
        try {
            java.nio.file.Files.writeString(new File(target, "debug-cart-window.txt").toPath(), dumpText());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dump cart debugger", e);
        }
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
