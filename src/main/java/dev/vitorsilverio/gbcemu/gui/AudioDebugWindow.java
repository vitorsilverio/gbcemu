package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.audio.ApuChannelDebugSnapshot;
import dev.vitorsilverio.gbcemu.audio.ApuDebugSnapshot;
import dev.vitorsilverio.gbcemu.audio.ApuRegisterWrite;
import dev.vitorsilverio.gbcemu.debug.ApuDebugAudioInterface;
import dev.vitorsilverio.gbcemu.debug.DebugAudioInterface;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Supplier;

public class AudioDebugWindow {

    public record Target(String name, DebugAudioInterface audio) {
        @Override
        public String toString() {
            return name;
        }
    }

    private DebugAudioInterface audio;
    private final JComboBox<Target> targetSelector;
    private final Supplier<List<Target>> targetSupplier;
    private final JFrame frame = new JFrame("Audio Debug");
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel masterState = new JLabel();
    private final DefaultTableModel channelModel = new DefaultTableModel(
            new Object[]{"CH", "On", "DAC", "Period", "Hz", "Len", "Vol", "Env", "Timer", "Dig", "Out", "Pos", "Extra", "Detail"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel writesModel = new DefaultTableModel(
            new Object[]{"#", "Addr", "Reg", "Value", "Detail"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JCheckBox autoRefresh = new JCheckBox("Auto refresh");
    private final Timer refreshTimer = new Timer(1000, event -> {
        if (autoRefresh.isSelected() && frame.isVisible()) {
            refreshDebugState();
        }
    });
    private long lastDisplayedWriteSequence = -1;
    private boolean updatingTargetSelector;

    public AudioDebugWindow(dev.vitorsilverio.gbcemu.audio.Apu apu) {
        this.audio = new ApuDebugAudioInterface(apu);
        this.targetSelector = null;
        this.targetSupplier = null;
        this.audio.setWriteTraceEnabled(true);
        initializeWindow();
    }

    public AudioDebugWindow(List<Target> targets) {
        this(() -> targets);
    }

    public AudioDebugWindow(Supplier<List<Target>> targetSupplier) {
        List<Target> targets = targetSupplier.get();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one audio debug target is required");
        }
        this.targetSupplier = targetSupplier;
        this.targetSelector = new JComboBox<>(targets.toArray(Target[]::new));
        applyTarget(targets.getFirst());
        this.audio.setWriteTraceEnabled(true);
        initializeWindow();
    }

    private void initializeWindow() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                disableTraceForAllTargets();
            }
        });
        frame.setMinimumSize(new Dimension(880, 520));
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildToolbar(), BorderLayout.NORTH);

        tabs.addTab("Mixer", buildMixerTab());
        tabs.addTab("State", buildStateTab());
        tabs.addTab("Writes", buildWritesTab());
        tabs.setSelectedIndex(2);
        content.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.pack();
        frame.setVisible(true);
        refreshDebugState();
        refreshTimer.start();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel();
        if (targetSelector != null) {
            targetSelector.addActionListener(event -> {
                if (updatingTargetSelector) {
                    return;
                }
                Target target = (Target) targetSelector.getSelectedItem();
                if (target != null) {
                    audio.setWriteTraceEnabled(false);
                    applyTarget(target);
                    audio.setWriteTraceEnabled(true);
                    rebuildTabs();
                    refreshDebugState();
                }
            });
            actions.add(targetSelector);
        }
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refreshDebugState());
        actions.add(refresh);
        actions.add(autoRefresh);
        toolbar.add(masterState, BorderLayout.CENTER);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private void applyTarget(Target target) {
        this.audio = target.audio();
        this.lastDisplayedWriteSequence = -1;
    }

    private void rebuildTabs() {
        tabs.removeAll();
        tabs.addTab("Mixer", buildMixerTab());
        tabs.addTab("State", buildStateTab());
        tabs.addTab("Writes", buildWritesTab());
        tabs.setSelectedIndex(2);
    }

    private void disableTraceForAllTargets() {
        if (targetSelector == null) {
            audio.setWriteTraceEnabled(false);
            return;
        }
        refreshTargets(false);
        for (int i = 0; i < targetSelector.getItemCount(); i++) {
            targetSelector.getItemAt(i).audio().setWriteTraceEnabled(false);
        }
    }

    private JPanel buildMixerTab() {
        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.add(buildMasterControls(), BorderLayout.NORTH);
        controls.add(buildChannels(), BorderLayout.CENTER);
        return controls;
    }

    private JPanel buildStateTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable channels = new JTable(channelModel);
        channels.setFillsViewportHeight(true);
        channels.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(channels), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildWritesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable writes = new JTable(writesModel);
        writes.setFillsViewportHeight(true);
        panel.add(new JScrollPane(writes), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildChannels() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Channels"));
        for (int channel = 1; channel <= 4; channel++) {
            addChannel(panel, channel);
        }
        return panel;
    }

    private JPanel buildMasterControls() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Master / stereo"));
        addVolumeSlider(panel, 0, "Master", audio.masterVolume(), audio::setMasterVolume);
        addVolumeSlider(panel, 1, "Left", audio.leftVolume(), audio::setLeftVolume);
        addVolumeSlider(panel, 2, "Right", audio.rightVolume(), audio::setRightVolume);
        addFilterSlider(panel, 3, "Low-pass", audio.lowPassAlpha(), audio::setLowPassAlpha);
        return panel;
    }

    private void addChannel(JPanel panel, int channel) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = channel - 1;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;

        JLabel label = new JLabel(channelName(channel));
        constraints.gridx = 0;
        panel.add(label, constraints);

        JSlider volume = createSlider(audio.channelVolume(channel));
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(volume, constraints);

        JLabel value = new JLabel(volume.getValue() + "%");
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(value, constraints);

        JCheckBox mute = new JCheckBox("Mute", audio.channelMuted(channel));
        constraints.gridx = 3;
        panel.add(mute, constraints);

        volume.addChangeListener(event -> {
            audio.setChannelVolume(channel, volume.getValue());
            value.setText(volume.getValue() + "%");
        });
        mute.addActionListener(event -> {
            audio.setChannelMuted(channel, mute.isSelected());
        });
    }

    private void addVolumeSlider(JPanel panel, int row, String name, int initialValue, VolumeSetter setter) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        panel.add(new JLabel(name), constraints);

        JSlider slider = createSlider(initialValue);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(slider, constraints);

        JLabel value = new JLabel(initialValue + "%");
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(value, constraints);

        slider.addChangeListener(event -> {
            setter.set(slider.getValue());
            value.setText(slider.getValue() + "%");
        });
    }

    private void addFilterSlider(JPanel panel, int row, String name, int initialValue, VolumeSetter setter) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        panel.add(new JLabel(name), constraints);

        JSlider slider = new JSlider(50, 1000, initialValue);
        slider.setMajorTickSpacing(250);
        slider.setMinorTickSpacing(50);
        slider.setPaintTicks(true);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(slider, constraints);

        JLabel value = new JLabel(String.valueOf(initialValue));
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(value, constraints);

        slider.addChangeListener(event -> {
            setter.set(slider.getValue());
            value.setText(String.valueOf(slider.getValue()));
        });
    }

    private JSlider createSlider(int initialValue) {
        JSlider slider = new JSlider(0, 100, initialValue);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);
        return slider;
    }

    private static String channelName(int channel) {
        return switch (channel) {
            case 1 -> "CH1 Pulse";
            case 2 -> "CH2 Pulse";
            case 3 -> "CH3 Wave";
            case 4 -> "CH4 Noise";
            default -> "CH" + channel;
        };
    }

    private void refreshDebugState() {
        refreshTargets(true);
        ApuDebugSnapshot snapshot = audio.snapshot();
        masterState.setText(String.format(
                "NR50=%02X  NR51=%02X  NR52=%02X  FS=%d  sampleRate=%d  sampleAcc=%d  buffered=%d bytes  lowPass=%d",
                snapshot.nr50(),
                snapshot.nr51(),
                snapshot.nr52(),
                snapshot.frameSequencerStep(),
                snapshot.sampleRate(),
                snapshot.sampleAccumulator(),
                snapshot.bufferedSampleBytes(),
                snapshot.lowPassAlpha()
        ));
        masterState.setToolTipText(snapshot.audioSink() + " lowPass=" + snapshot.lowPassAlpha());
        refreshChannelRows(snapshot);
        refreshWriteRows(snapshot.recentWrites());
    }

    private void refreshTargets(boolean keepCurrentTraceEnabled) {
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
        if (audio != next.audio()) {
            audio.setWriteTraceEnabled(false);
            applyTarget(next);
            audio.setWriteTraceEnabled(keepCurrentTraceEnabled);
            rebuildTabs();
        }
    }

    private void refreshChannelRows(ApuDebugSnapshot snapshot) {
        channelModel.setRowCount(0);
        addChannelRow(snapshot.channel1());
        addChannelRow(snapshot.channel2());
        addChannelRow(snapshot.channel3());
        addChannelRow(snapshot.channel4());
    }

    private void addChannelRow(ApuChannelDebugSnapshot channel) {
        channelModel.addRow(new Object[]{
                channel.name(),
                channel.enabled() ? "yes" : "no",
                channel.dacEnabled() ? "yes" : "no",
                hex(channel.period(), 4),
                String.format("%.2f", channel.frequencyHz()),
                channel.lengthTimer(),
                channel.currentVolume(),
                channel.envelopeTimer(),
                channel.timer(),
                channel.digitalOutput(),
                channel.analogOutput(),
                hex(channel.sequencerPosition(), 4),
                hex(channel.extra(), 2),
                channel.detail()
        });
    }

    private void refreshWriteRows(List<ApuRegisterWrite> writes) {
        long lastSequence = writes.isEmpty() ? -1 : writes.get(writes.size() - 1).sequence();
        if (lastSequence == lastDisplayedWriteSequence) {
            return;
        }
        lastDisplayedWriteSequence = lastSequence;
        writesModel.setRowCount(0);
        for (ApuRegisterWrite write : writes) {
            writesModel.addRow(new Object[]{
                    write.sequence(),
                    hex(write.address(), 4),
                    registerName(write.address()),
                    hex(write.value(), 2),
                    write.detail()
            });
        }
    }

    private static String hex(int value, int digits) {
        return String.format("%0" + digits + "X", value & ((1 << (digits * 4)) - 1));
    }

    private static String registerName(int address) {
        return switch (address) {
            case 0xFF10 -> "NR10";
            case 0xFF11 -> "NR11";
            case 0xFF12 -> "NR12";
            case 0xFF13 -> "NR13";
            case 0xFF14 -> "NR14";
            case 0xFF16 -> "NR21";
            case 0xFF17 -> "NR22";
            case 0xFF18 -> "NR23";
            case 0xFF19 -> "NR24";
            case 0xFF1A -> "NR30";
            case 0xFF1B -> "NR31";
            case 0xFF1C -> "NR32";
            case 0xFF1D -> "NR33";
            case 0xFF1E -> "NR34";
            case 0xFF20 -> "NR41";
            case 0xFF21 -> "NR42";
            case 0xFF22 -> "NR43";
            case 0xFF23 -> "NR44";
            case 0xFF24 -> "NR50";
            case 0xFF25 -> "NR51";
            case 0xFF26 -> "NR52";
            default -> address >= 0xFF30 && address <= 0xFF3F ? "Wave RAM" : "";
        };
    }

    @FunctionalInterface
    private interface VolumeSetter {
        void set(int volume);
    }
}
