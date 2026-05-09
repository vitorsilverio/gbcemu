package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.snapshot.SaveStateFile;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateMetadata;
import dev.vitorsilverio.gbcemu.snapshot.SaveStateStore;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

class SaveStateDialog extends JDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final File romFile;
    private final SaveStateStore store;
    private final Supplier<SaveStateFile> saveStateSupplier;
    private final Consumer<SaveStateFile> restoreAction;
    private final DefaultListModel<SlotEntry> listModel = new DefaultListModel<>();
    private final JList<SlotEntry> slots = new JList<>(listModel);
    private final JLabel preview = new JLabel();
    private final JLabel createdAt = valueLabel();
    private final JLabel romTitle = valueLabel();
    private final JLabel cartridgeType = valueLabel();
    private final JLabel frameNumber = valueLabel();
    private final JLabel pc = valueLabel();
    private final JLabel file = valueLabel();

    SaveStateDialog(
            File romFile,
            SaveStateStore store,
            Supplier<SaveStateFile> saveStateSupplier,
            Consumer<SaveStateFile> restoreAction
    ) {
        super((java.awt.Frame) null, "Save states", false);
        this.romFile = romFile;
        this.store = store;
        this.saveStateSupplier = saveStateSupplier;
        this.restoreAction = restoreAction;
        buildUi();
        refresh();
        setSize(760, 440);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        slots.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        slots.addListSelectionListener(event -> updateDetails(slots.getSelectedValue()));

        JPanel details = new JPanel(new BorderLayout(8, 8));
        details.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        preview.setHorizontalAlignment(JLabel.CENTER);
        preview.setPreferredSize(new Dimension(320, 288));
        details.add(preview, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 4));
        addField(fields, "Created", createdAt);
        addField(fields, "ROM", romTitle);
        addField(fields, "Cart", cartridgeType);
        addField(fields, "Frame", frameNumber);
        addField(fields, "PC", pc);
        addField(fields, "File", file);
        details.add(fields, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(slots), details);
        splitPane.setDividerLocation(260);
        add(splitPane, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton saveNew = new JButton("Save new");
        saveNew.addActionListener(event -> saveNew());
        actions.add(saveNew);

        JButton overwrite = new JButton("Overwrite selected");
        overwrite.addActionListener(event -> overwriteSelected());
        actions.add(overwrite);

        JButton load = new JButton("Load selected");
        load.addActionListener(event -> loadSelected());
        actions.add(load);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        actions.add(refresh);

        add(actions, BorderLayout.SOUTH);
    }

    private void refresh() {
        List<SaveStateStore.Slot> loadedSlots = store.list(romFile);
        listModel.clear();
        for (SaveStateStore.Slot slot : loadedSlots) {
            listModel.addElement(new SlotEntry(slot.index(), slot));
        }
        int nextSlot = store.nextSlotIndex(romFile);
        listModel.addElement(new SlotEntry(nextSlot, null));
        slots.setSelectedIndex(listModel.size() > 1 ? listModel.size() - 2 : 0);
    }

    private void saveNew() {
        saveToSlot(store.nextSlotIndex(romFile));
    }

    private void overwriteSelected() {
        SlotEntry selected = slots.getSelectedValue();
        if (selected == null) {
            return;
        }
        saveToSlot(selected.index());
    }

    private void saveToSlot(int slotIndex) {
        try {
            store.save(romFile, slotIndex, saveStateSupplier.get());
            refresh();
            selectSlot(slotIndex);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save state: " + e.getMessage(), "Save states", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        SlotEntry selected = slots.getSelectedValue();
        if (selected == null || selected.slot() == null) {
            return;
        }
        restoreAction.accept(selected.slot().saveStateFile());
        dispose();
    }

    private void selectSlot(int slotIndex) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).index() == slotIndex) {
                slots.setSelectedIndex(i);
                return;
            }
        }
    }

    private void updateDetails(SlotEntry entry) {
        if (entry == null || entry.slot() == null) {
            preview.setIcon(null);
            preview.setText("Empty slot");
            createdAt.setText("");
            romTitle.setText("");
            cartridgeType.setText("");
            frameNumber.setText("");
            pc.setText("");
            file.setText("");
            return;
        }
        SaveStateMetadata metadata = entry.slot().saveStateFile().metadata();
        preview.setText("");
        preview.setIcon(new ImageIcon(previewImage(metadata)));
        createdAt.setText(DATE_FORMAT.format(metadata.createdAt()));
        romTitle.setText(metadata.romTitle());
        cartridgeType.setText(metadata.cartridgeType());
        frameNumber.setText(Long.toString(metadata.frameNumber()));
        pc.setText(String.format("%04X", metadata.pc()));
        file.setText(entry.slot().file().getName());
    }

    private Image previewImage(SaveStateMetadata metadata) {
        BufferedImage image = new BufferedImage(metadata.previewWidth(), metadata.previewHeight(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, metadata.previewWidth(), metadata.previewHeight(), metadata.previewArgb(), 0, metadata.previewWidth());
        return image.getScaledInstance(metadata.previewWidth() * 2, metadata.previewHeight() * 2, Image.SCALE_FAST);
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel();
        label.setMinimumSize(new Dimension(120, 20));
        return label;
    }

    private static void addField(JPanel panel, String name, JLabel value) {
        panel.add(new JLabel(name));
        panel.add(value);
    }

    private record SlotEntry(int index, SaveStateStore.Slot slot) {
        @Override
        public String toString() {
            if (slot == null) {
                return "Slot " + index + " (empty)";
            }
            SaveStateMetadata metadata = slot.saveStateFile().metadata();
            return "Slot " + index + " - " + DATE_FORMAT.format(metadata.createdAt()) + " - frame " + metadata.frameNumber();
        }
    }
}
