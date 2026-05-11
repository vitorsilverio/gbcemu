package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MultiplayerDialog {

    private Multiplayer multiplayer;
    private JFrame frame;
    private JRadioButton localRadio;
    private JRadioButton tcpRadio;
    private JRadioButton hostRadio;
    private JRadioButton guestRadio;
    private JTextField pathField;
    private JTextField hostField;
    private JTextField portField;
    private JLabel pathLabel;
    private JLabel hostLabel;
    private JLabel portLabel;
    private JButton browseButton;

    public MultiplayerDialog(Multiplayer multiplayer) {
        this.multiplayer = multiplayer;
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Multiplayer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Connection type
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Connection Type:"), gbc);

        localRadio = new JRadioButton("Local");
        tcpRadio = new JRadioButton("TCP Network");
        ButtonGroup connectionGroup = new ButtonGroup();
        connectionGroup.add(localRadio);
        connectionGroup.add(tcpRadio);
        localRadio.setSelected(true);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(localRadio, gbc);
        gbc.gridx = 1;
        panel.add(tcpRadio, gbc);

        // Role
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Role:"), gbc);

        hostRadio = new JRadioButton("Host");
        guestRadio = new JRadioButton("Guest");
        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(hostRadio);
        roleGroup.add(guestRadio);
        hostRadio.setSelected(true);

        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(hostRadio, gbc);
        gbc.gridx = 1;
        panel.add(guestRadio, gbc);

        // Local fields
        pathLabel = new JLabel("Socket Path:");
        pathField = new JTextField(System.getProperty("user.dir") + "/gbcemu.sock", 20);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(pathField.getText()));
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(pathLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(pathField, gbc);
        gbc.gridx = 3;
        panel.add(browseButton, gbc);

        // TCP fields
        hostLabel = new JLabel("Hostname:");
        hostField = new JTextField("localhost", 15);
        portLabel = new JLabel("Port:");
        portField = new JTextField("26803", 5);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(hostLabel, gbc);
        gbc.gridx = 1;
        panel.add(hostField, gbc);
        gbc.gridx = 2;
        panel.add(portLabel, gbc);
        gbc.gridx = 3;
        panel.add(portField, gbc);

        // Buttons
        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");

        connectButton.addActionListener(e -> {
            try {
                if (localRadio.isSelected()) {
                    String path = pathField.getText();
                    if (hostRadio.isSelected()) {
                        multiplayer.hostLocal(path);
                    } else {
                        multiplayer.joinLocal(path);
                    }
                } else {
                    String host = hostField.getText();
                    int port = Integer.parseInt(portField.getText());
                    if (hostRadio.isSelected()) {
                        multiplayer.hostTcp(host, port);
                    } else {
                        multiplayer.joinTcp(host, port);
                    }
                }
                frame.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> frame.dispose());

        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(connectButton, gbc);
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        // Listeners for visibility
        localRadio.addActionListener(e -> updateVisibility());
        tcpRadio.addActionListener(e -> updateVisibility());
        updateVisibility();

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    private void updateVisibility() {
        boolean isLocal = localRadio.isSelected();
        pathLabel.setVisible(isLocal);
        pathField.setVisible(isLocal);
        browseButton.setVisible(isLocal);
        hostLabel.setVisible(!isLocal);
        hostField.setVisible(!isLocal);
        portLabel.setVisible(!isLocal);
        portField.setVisible(!isLocal);
    }
}
