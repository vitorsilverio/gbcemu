package dev.vitorsilverio.gbcemu;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class SettingsDialog extends JDialog {

    private final Consumer<AppSettings> onSave;
    private final JSpinner screenScale;
    private final JCheckBox smoothScaling;
    private final JCheckBox fullscreen;
    private final JSpinner rewindSeconds;
    private final JSpinner rewindInterval;
    private final JSlider masterVolume;
    private final JSlider leftVolume;
    private final JSlider rightVolume;
    private final JSlider[] channelVolumes = new JSlider[4];
    private final JCheckBox[] channelMuted = new JCheckBox[4];
    private final KeyCaptureButton[] controllerKeys = new KeyCaptureButton[AppSettings.CONTROLLER_BUTTON_NAMES.length];

    public SettingsDialog(Frame owner, AppSettings settings, Consumer<AppSettings> onSave) {
        super(owner, "Settings", true);
        this.onSave = onSave;
        this.screenScale = spinner(settings.screenScale(), 1, 8, 1);
        this.smoothScaling = new JCheckBox("Smooth scaling", settings.smoothScaling());
        this.fullscreen = new JCheckBox("Fullscreen", settings.fullscreen());
        this.rewindSeconds = spinner(settings.rewindSeconds(), 1, 120, 1);
        this.rewindInterval = spinner(settings.rewindCaptureIntervalFrames(), 1, 60, 1);
        this.masterVolume = slider(settings.audioMasterVolume());
        this.leftVolume = slider(settings.audioLeftVolume());
        this.rightVolume = slider(settings.audioRightVolume());
        for (int i = 0; i < channelVolumes.length; i++) {
            channelVolumes[i] = slider(settings.audioChannelVolume(i + 1));
            channelMuted[i] = new JCheckBox("Mute", settings.audioChannelMuted(i + 1));
        }
        for (int i = 0; i < controllerKeys.length; i++) {
            controllerKeys[i] = new KeyCaptureButton(settings.controllerKeyCode(i));
        }
        initialize();
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridBagLayout());
        addRow(fields, 0, "Screen scale", screenScale);
        addRow(fields, 1, "Screen filter", smoothScaling);
        addRow(fields, 2, "Fullscreen", fullscreen);
        addRow(fields, 3, "Rewind seconds", rewindSeconds);
        addRow(fields, 4, "Rewind interval frames", rewindInterval);
        addRow(fields, 5, "Master volume", masterVolume);
        addRow(fields, 6, "Left volume", leftVolume);
        addRow(fields, 7, "Right volume", rightVolume);
        for (int i = 0; i < channelVolumes.length; i++) {
            JPanel channel = new JPanel(new BorderLayout(6, 0));
            channel.add(channelVolumes[i], BorderLayout.CENTER);
            channel.add(channelMuted[i], BorderLayout.EAST);
            addRow(fields, i + 8, "Channel " + (i + 1) + " volume", channel);
        }
        for (int i = 0; i < controllerKeys.length; i++) {
            addRow(fields, i + 12, "Button " + AppSettings.CONTROLLER_BUTTON_NAMES[i], controllerKeys[i]);
        }
        add(fields, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton save = new JButton("Save");
        save.addActionListener(event -> save());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> dispose());
        buttons.add(save);
        buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(4, 6, 4, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, constraints);
    }

    private void save() {
        int[] channelValues = new int[4];
        boolean[] mutedValues = new boolean[4];
        int[] keyValues = new int[controllerKeys.length];
        for (int i = 0; i < channelValues.length; i++) {
            channelValues[i] = channelVolumes[i].getValue();
            mutedValues[i] = channelMuted[i].isSelected();
        }
        for (int i = 0; i < keyValues.length; i++) {
            keyValues[i] = controllerKeys[i].keyCode();
        }
        onSave.accept(new AppSettings(
                (int) screenScale.getValue(),
                smoothScaling.isSelected(),
                fullscreen.isSelected(),
                (int) rewindSeconds.getValue(),
                (int) rewindInterval.getValue(),
                masterVolume.getValue(),
                leftVolume.getValue(),
                rightVolume.getValue(),
                channelValues,
                mutedValues,
                keyValues
        ).normalized());
        dispose();
    }

    private JSpinner spinner(int value, int min, int max, int step) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, step));
    }

    private JSlider slider(int value) {
        JSlider slider = new JSlider(0, 100, value);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);
        return slider;
    }

    private static class KeyCaptureButton extends JButton {
        private int keyCode;

        private KeyCaptureButton(int keyCode) {
            this.keyCode = keyCode;
            setFocusable(true);
            updateText();
            addActionListener(event -> {
                setText("Press key...");
                requestFocusInWindow();
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    KeyCaptureButton.this.keyCode = event.getKeyCode();
                    updateText();
                }
            });
        }

        private int keyCode() {
            return keyCode;
        }

        private void updateText() {
            setText(KeyEvent.getKeyText(keyCode));
        }
    }
}
