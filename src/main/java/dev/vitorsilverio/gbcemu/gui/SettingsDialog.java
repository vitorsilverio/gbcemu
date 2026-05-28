package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.gui.audio.AudioOutput;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
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
    private final JCheckBox superGameBoyBordersEnabled;
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
    private final JComboBox<String> audioDspPreset;
    private final JSlider audioDspIntensity;
    private final JSlider audioChorusAmount;
    private final JSlider audioReverbAmount;
    private final JComboBox<String> audioOutputDevice;
    private final JSpinner audioOutputBufferMillis;
    private final JComboBox<String> soundFontMode;
    private final JTextField soundFontPath;
    private final JSlider[] channelVolumes = new JSlider[4];
    private final JCheckBox[] channelMuted = new JCheckBox[4];
    private final KeyCaptureButton[] controllerKeys = new KeyCaptureButton[AppSettings.CONTROLLER_BUTTON_NAMES.length];
    private final KeyCaptureButton[] player2ControllerKeys = new KeyCaptureButton[AppSettings.CONTROLLER_BUTTON_NAMES.length];
    private final JComboBox<String>[] gamepadDevices = new JComboBox[2];
    private final JSpinner[] gamepadDeadzones = new JSpinner[2];
    private final JButton[] showGamepadComponents = new JButton[2];
    private final JTextField[][] gamepadMappings = new JTextField[2][AppSettings.CONTROLLER_BUTTON_NAMES.length];
    private final JButton[][] captureGamepadMappings = new JButton[2][AppSettings.CONTROLLER_BUTTON_NAMES.length];

    public SettingsDialog(Frame owner, AppSettings settings, Consumer<AppSettings> onSave) {
        super(owner, "Settings", true);
        this.onSave = onSave;
        this.currentSettings = settings;
        this.screenScale = spinner(settings.screenScale(), 1, 8, 1);
        this.smoothScaling = new JCheckBox("Smooth scaling", settings.smoothScaling());
        this.xBrzFiltering = new JCheckBox("xBrz filtering", settings.xBrzFiltering());
        this.superGameBoyBordersEnabled = new JCheckBox("Enable Super Game Boy borders", settings.superGameBoyBordersEnabled());
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
        AppSettings.AudioEnhancementConfig enhancement = settings.normalizedAudioEnhancement();
        this.audioDspPreset = combo(new String[]{"Raw", "Warm", "Wide", "Room", "Toy Synth"}, enhancement.dspPreset());
        this.audioDspIntensity = slider(enhancement.dspIntensity());
        this.audioChorusAmount = slider(enhancement.chorusAmount());
        this.audioReverbAmount = slider(enhancement.reverbAmount());
        this.audioOutputDevice = audioOutputDeviceCombo(enhancement.outputDeviceName());
        this.audioOutputBufferMillis = spinner(enhancement.outputBufferMillis(), 20, 500, 10);
        this.soundFontMode = combo(new String[]{"Off", "Overlay", "Replace original", "Percussion overlay"}, enhancement.soundFontMode());
        this.soundFontPath = new JTextField(enhancement.soundFontPath(), 28);
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
            gamepadDevices[player] = gamepadDeviceCombo(devices, config);
            gamepadDeadzones[player] = spinner(config.deadzonePercent(), 0, 95, 1);
            int playerIndex = player;
            showGamepadComponents[player] = new JButton("Components...");
            showGamepadComponents[player].addActionListener(event -> showGamepadComponents(playerIndex));
            for (int i = 0; i < gamepadMappings[player].length; i++) {
                int buttonIndex = i;
                gamepadMappings[player][i] = new JTextField(config.mappings()[i], 22);
                captureGamepadMappings[player][i] = new JButton("Capture");
                captureGamepadMappings[player][i].addActionListener(event -> captureGamepadMapping(playerIndex, buttonIndex));
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
        return wrapPanel(fields, this::resetGeneralDefaults);
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
        addRow(fields, 3, "SGB borders", superGameBoyBordersEnabled);
        addRow(fields, 4, "Fullscreen", fullscreen);
        return wrapPanel(fields, this::resetGraphicsDefaults);
    }

    private JPanel audioPanel() {
        JPanel fields = new JPanel(new GridBagLayout());
        addRow(fields, 0, "Master volume", masterVolume);
        addRow(fields, 1, "Left volume", leftVolume);
        addRow(fields, 2, "Right volume", rightVolume);
        addRow(fields, 3, "Output device", audioOutputDevice);
        addRow(fields, 4, "Buffer ms", audioOutputBufferMillis);
        for (int i = 0; i < channelVolumes.length; i++) {
            JPanel channel = new JPanel(new BorderLayout(6, 0));
            channel.add(channelVolumes[i], BorderLayout.CENTER);
            channel.add(channelMuted[i], BorderLayout.EAST);
            addRow(fields, i + 5, "Channel " + (i + 1) + " volume", channel);
        }
        int row = channelVolumes.length + 6;
        addSectionLabel(fields, row++, "Enhanced audio");
        addRow(fields, row++, "DSP preset", audioDspPreset);
        addRow(fields, row++, "DSP intensity", audioDspIntensity);
        addRow(fields, row++, "Chorus amount", audioChorusAmount);
        addRow(fields, row++, "Reverb amount", audioReverbAmount);
        addSectionLabel(fields, row++, "SoundFont experimental");
        addHint(fields, row++, "Uses Java MIDI. Overlay keeps the original PCM; Replace original silences it.");
        addRow(fields, row++, "Mode", soundFontMode);
        addRow(fields, row, "SoundFont file", soundFontPanel());
        return wrapPanel(fields, this::resetAudioDefaults);
    }

    private JPanel soundFontPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton browse = new JButton("Browse...");
        browse.addActionListener(event -> chooseSoundFont());
        panel.add(soundFontPath, BorderLayout.CENTER);
        panel.add(browse, BorderLayout.EAST);
        return panel;
    }

    private void chooseSoundFont() {
        JFileChooser chooser = new JFileChooser(soundFontDirectory());
        chooser.setDialogTitle("Set SoundFont");
        chooser.setFileFilter(new FileNameExtensionFilter("SoundFont files", "sf2", "sfz"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            soundFontPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private JPanel controlsPanel() {
        JTabbedPane playerTabs = new JTabbedPane();
        playerTabs.addTab("Player 1", playerControlsPanel(0));
        playerTabs.addTab("Player 2", playerControlsPanel(1));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(playerTabs, BorderLayout.CENTER);
        JButton reset = new JButton("Reset tab defaults");
        reset.addActionListener(event -> resetControlsDefaults());
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(reset, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane playerControlsPanel(int player) {
        JPanel fields = new JPanel(new GridBagLayout());
        int row = 0;
        addSectionLabel(fields, row++, "Keyboard");
        KeyCaptureButton[] keys = player == 0 ? controllerKeys : player2ControllerKeys;
        for (int i = 0; i < keys.length; i++) {
            addRow(fields, row++, AppSettings.CONTROLLER_BUTTON_NAMES[i], keys[i]);
        }

        addSectionLabel(fields, row++, "Gamepad");
        addRow(fields, row++, "Device", gamepadDevices[player]);
        addRow(fields, row++, "Deadzone %", gamepadDeadzones[player]);
        addRow(fields, row++, "Detected inputs", showGamepadComponents[player]);
        addHint(fields, row++, "Use Capture to replace a mapping with the next pressed button or axis.");
        for (int i = 0; i < AppSettings.CONTROLLER_BUTTON_NAMES.length; i++) {
            addRow(fields, row++, AppSettings.CONTROLLER_BUTTON_NAMES[i], gamepadMappingPanel(player, i));
        }
        JScrollPane scrollPane = new JScrollPane(fields);
        scrollPane.setPreferredSize(new Dimension(720, 500));
        return scrollPane;
    }

    private JPanel gamepadMappingPanel(int player, int buttonIndex) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.add(gamepadMappings[player][buttonIndex], BorderLayout.CENTER);
        panel.add(captureGamepadMappings[player][buttonIndex], BorderLayout.EAST);
        return panel;
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

    private void addHint(JPanel panel, int row, String text) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(text), constraints);
    }

    private JPanel wrapPanel(JPanel fields, Runnable resetAction) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(fields, BorderLayout.NORTH);
        JButton reset = new JButton("Reset tab defaults");
        reset.addActionListener(event -> resetAction.run());
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(reset, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
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
                    GamepadController.deviceProfileKey(gamepadDevices[player].getSelectedIndex() - 1),
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
                new AppSettings.AudioEnhancementConfig(
                        selected(audioDspPreset),
                        audioDspIntensity.getValue(),
                        audioChorusAmount.getValue(),
                        audioReverbAmount.getValue(),
                        selected(soundFontMode),
                        soundFontPath.getText(),
                        selectedAudioOutputDevice(),
                        (int) audioOutputBufferMillis.getValue()
                ),
                keyValues,
                player2KeyValues,
                gamepadValues,
                (int) turboMultiplier.getValue(),
                turboKey.keyCode(),
                turboToggleMode.isSelected(),
                xBrzFiltering.isSelected(),
                superGameBoyBordersEnabled.isSelected(),
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

    private File soundFontDirectory() {
        String path = soundFontPath.getText();
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

    private JComboBox<String> combo(String[] values, String selected) {
        JComboBox<String> combo = new JComboBox<>(values);
        combo.setSelectedItem(selected);
        return combo;
    }

    private String selected(JComboBox<String> combo) {
        Object value = combo.getSelectedItem();
        return value == null ? "" : value.toString();
    }

    private JComboBox<String> gamepadDeviceCombo(List<String> devices, AppSettings.GamepadConfig config) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("Disabled");
        for (String device : devices) {
            combo.addItem(device);
        }
        int selectedDeviceIndex = findDeviceIndex(devices, config);
        while (combo.getItemCount() <= selectedDeviceIndex + 1) {
            combo.addItem("#" + combo.getItemCount() + " not connected");
        }
        combo.setSelectedIndex(Math.max(0, Math.min(selectedDeviceIndex + 1, combo.getItemCount() - 1)));
        return combo;
    }

    private JComboBox<String> audioOutputDeviceCombo(String selectedDeviceName) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("System default");
        for (String deviceName : AudioOutput.outputDeviceNames(48_000)) {
            combo.addItem(deviceName);
        }
        if (selectedDeviceName != null && !selectedDeviceName.isBlank()) {
            if (!comboContains(combo, selectedDeviceName)) {
                combo.addItem(selectedDeviceName);
            }
            combo.setSelectedItem(selectedDeviceName);
        }
        return combo;
    }

    private boolean comboContains(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equals(combo.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private String selectedAudioOutputDevice() {
        Object value = audioOutputDevice.getSelectedItem();
        if (value == null || "System default".equals(value.toString())) {
            return "";
        }
        return value.toString();
    }

    private int findDeviceIndex(List<String> devices, AppSettings.GamepadConfig config) {
        String deviceName = config.deviceName();
        if (deviceName != null && !deviceName.isBlank()) {
            for (int i = 0; i < devices.size(); i++) {
                if (devices.get(i).endsWith(deviceName)) {
                    return i;
                }
            }
        }
        return config.deviceIndex();
    }

    private void resetGeneralDefaults() {
        AppSettings defaults = AppSettings.defaults();
        rewindSeconds.setValue(defaults.rewindSeconds());
        rewindInterval.setValue(defaults.rewindCaptureIntervalFrames());
        turboMultiplier.setValue(defaults.turboMultiplier());
        turboKey.setKeyCode(defaults.turboKeyCode());
        turboToggleMode.setSelected(defaults.turboToggleMode());
        defaultBiosPath.setText(defaults.defaultBiosPath());
        rtcOffsetHours.setValue(defaults.rtcOffsetHours());
        rtcOffsetMinutes.setValue(defaults.rtcOffsetMinutes());
        rtcOffsetSeconds.setValue(defaults.rtcOffsetSeconds());
    }

    private void resetGraphicsDefaults() {
        AppSettings defaults = AppSettings.defaults();
        screenScale.setValue(defaults.screenScale());
        smoothScaling.setSelected(defaults.smoothScaling());
        xBrzFiltering.setSelected(defaults.xBrzFiltering());
        superGameBoyBordersEnabled.setSelected(defaults.superGameBoyBordersEnabled());
        fullscreen.setSelected(defaults.fullscreen());
    }

    private void resetAudioDefaults() {
        AppSettings defaults = AppSettings.defaults();
        AppSettings.AudioEnhancementConfig enhancement = defaults.normalizedAudioEnhancement();
        masterVolume.setValue(defaults.audioMasterVolume());
        leftVolume.setValue(defaults.audioLeftVolume());
        rightVolume.setValue(defaults.audioRightVolume());
        audioOutputDevice.setSelectedIndex(0);
        audioOutputBufferMillis.setValue(enhancement.outputBufferMillis());
        for (int i = 0; i < channelVolumes.length; i++) {
            channelVolumes[i].setValue(defaults.audioChannelVolume(i + 1));
            channelMuted[i].setSelected(defaults.audioChannelMuted(i + 1));
        }
        audioDspPreset.setSelectedItem(enhancement.dspPreset());
        audioDspIntensity.setValue(enhancement.dspIntensity());
        audioChorusAmount.setValue(enhancement.chorusAmount());
        audioReverbAmount.setValue(enhancement.reverbAmount());
        soundFontMode.setSelectedItem("Off");
        soundFontPath.setText(enhancement.soundFontPath());
    }

    private void resetControlsDefaults() {
        AppSettings defaults = AppSettings.defaults();
        for (int i = 0; i < controllerKeys.length; i++) {
            controllerKeys[i].setKeyCode(defaults.controllerKeyCode(i));
            player2ControllerKeys[i].setKeyCode(defaults.player2ControllerKeyCode(i));
        }
        for (int player = 0; player < gamepadDevices.length; player++) {
            AppSettings.GamepadConfig config = defaults.gamepadConfig(player);
            setGamepadDeviceSelection(gamepadDevices[player], config.deviceIndex());
            gamepadDeadzones[player].setValue(config.deadzonePercent());
            for (int i = 0; i < gamepadMappings[player].length; i++) {
                gamepadMappings[player][i].setText(config.mappings()[i]);
            }
        }
    }

    private void setGamepadDeviceSelection(JComboBox<String> combo, int deviceIndex) {
        int selectedIndex = deviceIndex + 1;
        while (combo.getItemCount() <= selectedIndex) {
            combo.addItem("#" + combo.getItemCount() + " not connected");
        }
        combo.setSelectedIndex(Math.max(0, selectedIndex));
    }

    private void showGamepadComponents(int player) {
        int deviceIndex = gamepadDevices[player].getSelectedIndex() - 1;
        JTextArea content = new JTextArea(GamepadController.componentSnapshot(deviceIndex), 18, 48);
        content.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(content), "Player " + (player + 1) + " gamepad inputs", JOptionPane.INFORMATION_MESSAGE);
    }

    private void captureGamepadMapping(int player, int buttonIndex) {
        int deviceIndex = gamepadDevices[player].getSelectedIndex() - 1;
        if (deviceIndex < 0) {
            JOptionPane.showMessageDialog(this,
                    "Select a gamepad device before capturing.",
                    "Gamepad capture",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JButton captureButton = captureGamepadMappings[player][buttonIndex];
        captureButton.setEnabled(false);
        captureButton.setText("Press...");
        int deadzone = (int) gamepadDeadzones[player].getValue();
        Thread thread = new Thread(() -> {
            String token = GamepadController.captureNextMapping(deviceIndex, deadzone, 5000L);
            SwingUtilities.invokeLater(() -> {
                captureButton.setEnabled(true);
                captureButton.setText("Capture");
                if (token.isBlank()) {
                    JOptionPane.showMessageDialog(this,
                            "No input detected before timeout.",
                            "Gamepad capture",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                gamepadMappings[player][buttonIndex].setText(token);
            });
        }, "gbcemu-gamepad-capture");
        thread.setDaemon(true);
        thread.start();
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

        private void setKeyCode(int keyCode) {
            this.keyCode = keyCode;
            updateText();
        }

        private void updateText() {
            setText(KeyEvent.getKeyText(keyCode));
        }
    }
}
