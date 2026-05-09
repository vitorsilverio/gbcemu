package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.audio.Apu;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class AudioDebugWindow {

    private final Apu apu;
    private final JFrame frame = new JFrame("Audio Debug");

    public AudioDebugWindow(Apu apu) {
        this.apu = apu;
        initializeWindow();
    }

    private void initializeWindow() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(420, 230));
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("Channel debug controls");
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        content.add(title, BorderLayout.NORTH);
        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.add(buildMasterControls(), BorderLayout.NORTH);
        controls.add(buildChannels(), BorderLayout.CENTER);
        content.add(controls, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.pack();
        frame.setVisible(true);
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
        addVolumeSlider(panel, 0, "Master", apu.debugMasterVolume(), apu::setDebugMasterVolume);
        addVolumeSlider(panel, 1, "Left", apu.debugLeftVolume(), apu::setDebugLeftVolume);
        addVolumeSlider(panel, 2, "Right", apu.debugRightVolume(), apu::setDebugRightVolume);
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

        JSlider volume = createSlider(apu.debugChannelVolume(channel));
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(volume, constraints);

        JLabel value = new JLabel(volume.getValue() + "%");
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(value, constraints);

        JCheckBox mute = new JCheckBox("Mute", apu.debugChannelMuted(channel));
        constraints.gridx = 3;
        panel.add(mute, constraints);

        volume.addChangeListener(event -> {
            apu.setDebugChannelVolume(channel, volume.getValue());
            value.setText(volume.getValue() + "%");
            mute.setSelected(volume.getValue() == 0);
        });
        mute.addActionListener(event -> {
            apu.setDebugChannelMuted(channel, mute.isSelected());
            volume.setValue(apu.debugChannelVolume(channel));
            value.setText(volume.getValue() + "%");
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

    @FunctionalInterface
    private interface VolumeSetter {
        void set(int volume);
    }
}
