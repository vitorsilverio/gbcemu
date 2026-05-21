package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.controller.GamepadController;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class SettingsDialog extends JDialog {

    private final Consumer<AppSettings> onSave;
    private final AppSettings currentSettings;
    private final JSpinner screenScale;
    private final JCheckBox smoothScaling;
    private final JCheckBox xBrzFiltering;
    private final JCheckBox fullscreen;
    private final JSpinner rewindSeconds;
    private final JSpinner rewindInterval;
    private final JSpinner turboMultiplier;
    private final KeyCaptureButton turboKey;
    private final JCheckBox turboToggleMode;
    private final JTextField defaultBiosPath;
    private final JSpinner rtcOffsetHours;
    private final JSpinner rtcOffsetMinutes;
    private final JSpinner rtcOffsetSeconds;
    private final JSlider masterVolume;
    private final JSlider leftVolume;
    private final JSlider rightVolume;
    private final JSlider[] channelVolumes = new JSlider[4];
    private final JCheckBox[] channelMuted = new JCheckBox[4];
    private final KeyCaptureButton[] controllerKeys = new KeyCaptureButton[AppSettings.CONTROLLER_BUTTON_NAMES.length];
    private final KeyCaptureButton[] player2ControllerKeys = new KeyCaptureButton[AppSettings.CONTROLLER_BUTTON_NAMES.length];
    private final JComboBox<String>[] gamepadDevices = new JComboBox[2];
    private final JSpinner[] gamepadDeadzones = new JSpinner[2];
    private final JButton[] showGamepadComponents = new JButton[2];
    private final JTextField[][] gamepadMappings = new JTextField[2][AppSettings.CONTROLLER_BUTTON_NAMES.length];

    public SettingsDialog(Frame owner, AppSettings settings, Consumer<AppSettings> onSave) {
        super(owner, "Settings", true);
        this.onSave = onSave;
        this.currentSettings = settings;
        this.screenScale = spinner(settings.screenScale(), 1, 8, 1);
        this.smoothScaling = new JCheckBox("Smooth scaling", settings.smoothScaling());
        this.xBrzFiltering = new JCheckBox("xBrz filtering", settings.xBrzFiltering());
        this.fullscreen = new JCheckBox("Fullscreen", settings.fullscreen());
        this.rewindSeconds = spinner(settings.rewindSeconds(), 0, 120, 1);
        this.rewindInterval = spinner(settings.rewindCaptureIntervalFrames(), 1, 60, 1);
        this.turboMultiplier = spinner(settings.turboMultiplier(), 1, 10, 1);
        this.turboKey = new KeyCaptureButton(settings.turboKeyCode());
        this.turboToggleMode = new JCheckBox("Toggle turbo", settings.turboToggleMode());
        this.defaultBiosPath = new JTextField(settings.defaultBiosPath(), 28);
        this.rtcOffsetHours = spinner(settings.rtcOffsetHours(), -9999, 9999, 1);
        this.rtcOffsetMinutes = spinner(settings.rtcOffsetMinutes(), -59, 59, 1);
        this.rtcOffsetSeconds = spinner(settings.rtcOffsetSeconds(), -59, 59, 1);
        this.masterVolume = slider(settings.audioMasterVolume());
        this.leftVolume = slider(settings.audioLeftVolume());
        this.rightVolume = slider(settings.audioRightVolume());
        for (int i = 0; i < channelVolumes.length; i++) {
            channelVolumes[i] = slider(settings.audioChannelVolume(i + 1));
            channelMuted[i] = new JCheckBox("Mute", settings.audioChannelMuted(i + 1));
        }
        for (int i = 0; i < controllerKeys.length; i++) {
            controllerKeys[i] = new KeyCaptureButton(settings.controllerKeyCode(i));
            player2ControllerKeys[i] = new KeyCaptureButton(settings.player2ControllerKeyCode(i));
        }
        List<String> devices = GamepadController.deviceNames();
        for (int player = 0; player < gamepadDevices.length; player++) {
            AppSettings.GamepadConfig config = settings.gamepadConfig(player);
            gamepadDevices[player] = gamepadDeviceCombo(devices, config.deviceIndex());
            gamepadDeadzones[player] = spinner(config.deadzonePercent(), 0, 95, 1);
            int playerIndex = player;
            showGamepadComponents[player] = new JButton("Components...");
            showGamepadComponents[player].addActionListener(event -> showGamepadComponents(playerIndex));
            for (int i = 0; i < gamepadMappings[player].length; i++) {
                gamepadMappings[player][i] = new JTextField(config.mappings()[i], 22);
            }
        }
        initialize();
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", generalPanel());
        tabs.addTab("Graphics", graphicsPanel());
        tabs.addTab("Audio", audioPanel());
        tabs.addTab("Controls", controlsPanel());
        add(tabs, BorderLayout.CENTER);

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

    private JPanel generalPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addRow(fields, 0, "Rewind seconds", rewindSeconds);
        addRow(fields, 1, "Rewind interval frames", rewindInterval);
        addRow(fields, 2, "Turbo multiplier", turboMultiplier);
        addRow(fields, 3, "Turbo key", turboKey);
        addRow(fields, 4, "Turbo mode", turboToggleMode);
        addRow(fields, 5, "Default BIOS", defaultBiosPanel());
        addRow(fields, 6, "RTC offset", rtcOffsetPanel());
        return wrapPanel(fields);
    }

    private JPanel rtcOffsetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        addInlineSpinner(panel, 0, "Hours", rtcOffsetHours);
        addInlineSpinner(panel, 1, "Minutes", rtcOffsetMinutes);
        addInlineSpinner(panel, 2, "Seconds", rtcOffsetSeconds);
        return panel;
    }

    private JPanel defaultBiosPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton browse = new JButton("Browse...");
        browse.addActionListener(event -> chooseDefaultBios());
        panel.add(defaultBiosPath, BorderLayout.CENTER);
        panel.add(browse, BorderLayout.EAST);
        return panel;
    }

    private void chooseDefaultBios() {
        JFileChooser chooser = new JFileChooser(currentDirectory());
        chooser.setDialogTitle("Set default BIOS");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            defaultBiosPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private JPanel graphicsPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addRow(fields, 0, "Screen scale", screenScale);
        addRow(fields, 1, "Screen filter", smoothScaling);
        addRow(fields, 2, "xBrz filter", xBrzFiltering);
        addRow(fields, 3, "Fullscreen", fullscreen);
        return wrapPanel(fields);
    }

    private JPanel audioPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addRow(fields, 0, "Master volume", masterVolume);
        addRow(fields, 1, "Left volume", leftVolume);
        addRow(fields, 2, "Right volume", rightVolume);
        for (int i = 0; i < channelVolumes.length; i++) {
            JPanel channel = new JPanel(new BorderLayout(6, 0));
            channel.add(channelVolumes[i], BorderLayout.CENTER);
            channel.add(channelMuted[i], BorderLayout.EAST);
            addRow(fields, i + 3, "Channel " + (i + 1) + " volume", channel);
        }
        return wrapPanel(fields);
    }

    private JPanel controlsPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addSectionLabel(fields, 0, "Keyboard");
        addControlHeader(fields, 1, 1, "Player 1");
        addControlHeader(fields, 1, 2, "Player 2");
        for (int i = 0; i < controllerKeys.length; i++) {
            int row = i + 2;
            addControlLabel(fields, row, AppSettings.CONTROLLER_BUTTON_NAMES[i]);
            addControlButton(fields, row, 1, controllerKeys[i]);
            addControlButton(fields, row, 2, player2ControllerKeys[i]);
        }
        int row = controllerKeys.length + 3;
        addSectionLabel(fields, row++, "Gamepad");
        addControlLabel(fields, row, "Device");
        addControlComponent(fields, row, 1, gamepadDevices[0]);
        addControlComponent(fields, row++, 2, gamepadDevices[1]);
        addControlLabel(fields, row, "Deadzone %");
        addControlComponent(fields, row, 1, gamepadDeadzones[0]);
        addControlComponent(fields, row++, 2, gamepadDeadzones[1]);
        addControlLabel(fields, row, "Detected inputs");
        addControlComponent(fields, row, 1, showGamepadComponents[0]);
        addControlComponent(fields, row++, 2, showGamepadComponents[1]);
        for (int i = 0; i < AppSettings.CONTROLLER_BUTTON_NAMES.length; i++) {
            addControlLabel(fields, row, AppSettings.CONTROLLER_BUTTON_NAMES[i]);
            addControlComponent(fields, row, 1, gamepadMappings[0][i]);
            addControlComponent(fields, row++, 2, gamepadMappings[1][i]);
        }
        return wrapPanel(fields);
    }

    private void addSectionLabel(JPanel panel, int row, String text) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(10, 6, 4, 6);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(text), constraints);
    }

    private void addControlHeader(JPanel panel, int row, int column, String text) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(4, 6, 8, 6);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(text), constraints);
    }

    private void addControlLabel(JPanel panel, int row, String text) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.insets = new Insets(4, 6, 4, 10);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(text), constraints);
    }

    private void addControlButton(JPanel panel, int row, int column, KeyCaptureButton button) {
        addControlComponent(panel, row, column, button);
    }

    private void addControlComponent(JPanel panel, int row, int column, java.awt.Component component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(4, 6, 4, 6);
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, constraints);
    }

    private JPanel wrapPanel(JPanel fields) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(fields, BorderLayout.NORTH);
        return panel;
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

    private void addInlineSpinner(JPanel panel, int column, String label, JSpinner spinner) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column * 2;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, column == 0 ? 0 : 8, 0, 4);
        panel.add(new JLabel(label), constraints);
        constraints.gridx = column * 2 + 1;
        panel.add(spinner, constraints);
    }

    private void save() {
        int[] channelValues = new int[4];
        boolean[] mutedValues = new boolean[4];
        int[] keyValues = new int[controllerKeys.length];
        int[] player2KeyValues = new int[player2ControllerKeys.length];
        AppSettings.GamepadConfig[] gamepadValues = new AppSettings.GamepadConfig[2];
        for (int i = 0; i < channelValues.length; i++) {
            channelValues[i] = channelVolumes[i].getValue();
            mutedValues[i] = channelMuted[i].isSelected();
        }
        for (int i = 0; i < keyValues.length; i++) {
            keyValues[i] = controllerKeys[i].keyCode();
            player2KeyValues[i] = player2ControllerKeys[i].keyCode();
        }
        for (int player = 0; player < gamepadValues.length; player++) {
            String[] mappings = new String[AppSettings.CONTROLLER_BUTTON_NAMES.length];
            for (int i = 0; i < mappings.length; i++) {
                mappings[i] = gamepadMappings[player][i].getText();
            }
            gamepadValues[player] = new AppSettings.GamepadConfig(
                    gamepadDevices[player].getSelectedIndex() - 1,
                    (int) gamepadDeadzones[player].getValue(),
                    mappings
            );
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
                keyValues,
                player2KeyValues,
                gamepadValues,
                (int) turboMultiplier.getValue(),
                turboKey.keyCode(),
                turboToggleMode.isSelected(),
                xBrzFiltering.isSelected(),
                defaultBiosPath.getText(),
                (int) rtcOffsetHours.getValue(),
                (int) rtcOffsetMinutes.getValue(),
                (int) rtcOffsetSeconds.getValue(),
                currentSettings.multiplayerTcpMode(),
                currentSettings.multiplayerHostMode(),
                currentSettings.multiplayerLocalPath(),
                currentSettings.multiplayerTcpHost(),
                currentSettings.multiplayerTcpPort()
        ).normalized());
        dispose();
    }

    private File currentDirectory() {
        String path = defaultBiosPath.getText();
        if (path != null && !path.isBlank()) {
            File file = new File(path);
            File parent = file.isDirectory() ? file : file.getParentFile();
            if (parent != null && parent.isDirectory()) {
                return parent;
            }
        }
        return new File(System.getProperty("user.dir"));
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

    private JComboBox<String> gamepadDeviceCombo(List<String> devices, int selectedDeviceIndex) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("Disabled");
        for (String device : devices) {
            combo.addItem(device);
        }
        while (combo.getItemCount() <= selectedDeviceIndex + 1) {
            combo.addItem("#" + combo.getItemCount() + " not connected");
        }
        combo.setSelectedIndex(Math.max(0, Math.min(selectedDeviceIndex + 1, combo.getItemCount() - 1)));
        return combo;
    }

    private void showGamepadComponents(int player) {
        int deviceIndex = gamepadDevices[player].getSelectedIndex() - 1;
        JTextArea content = new JTextArea(GamepadController.componentSnapshot(deviceIndex), 18, 48);
        content.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(content), "Player " + (player + 1) + " gamepad inputs", JOptionPane.INFORMATION_MESSAGE);
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
