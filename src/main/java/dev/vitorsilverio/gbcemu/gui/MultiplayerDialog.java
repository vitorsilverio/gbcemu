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
    private JLabel statusLabel;
    private JButton connectButton;
    private javax.swing.Timer refreshTimer;
    private final Frame owner;
    private final Runnable onConfigurationChanged;

    public MultiplayerDialog(Multiplayer multiplayer, Frame owner, Runnable onConfigurationChanged) {
        this.multiplayer = multiplayer;
        this.owner = owner;
        this.onConfigurationChanged = onConfigurationChanged;
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
        localRadio.setSelected(!multiplayer.lastTcpMode());
        tcpRadio.setSelected(multiplayer.lastTcpMode());

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
        hostRadio.setSelected(multiplayer.lastHostMode());
        guestRadio.setSelected(!multiplayer.lastHostMode());

        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(hostRadio, gbc);
        gbc.gridx = 1;
        panel.add(guestRadio, gbc);

        // Local fields
        pathLabel = new JLabel("Socket Path:");
        pathField = new JTextField(multiplayer.lastLocalPath(), 20);
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
        hostField = new JTextField(multiplayer.lastTcpHost(), 15);
        portLabel = new JLabel("Port:");
        portField = new JTextField(String.valueOf(multiplayer.lastTcpPort()), 5);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(hostLabel, gbc);
        gbc.gridx = 1;
        panel.add(hostField, gbc);
        gbc.gridx = 2;
        panel.add(portLabel, gbc);
        gbc.gridx = 3;
        panel.add(portField, gbc);

        statusLabel = new JLabel();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 4;
        panel.add(statusLabel, gbc);

        // Buttons
        connectButton = new JButton();
        JButton cancelButton = new JButton("Cancel");

        connectButton.addActionListener(e -> {
            try {
                if (multiplayer.isConnected() || multiplayer.isHosting()) {
                    multiplayer.disconnect();
                    onConfigurationChanged.run();
                    refreshStatus();
                    return;
                }
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
                onConfigurationChanged.run();
                refreshStatus();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                refreshStatus();
            }
        });

        cancelButton.addActionListener(e -> frame.dispose());

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        panel.add(connectButton, gbc);
        gbc.gridx = 1;
        panel.add(cancelButton, gbc);

        // Listeners for visibility
        localRadio.addActionListener(e -> updateVisibility());
        tcpRadio.addActionListener(e -> updateVisibility());
        updateVisibility();
        refreshStatus();
        refreshTimer = new javax.swing.Timer(500, event -> refreshStatus());
        refreshTimer.start();
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                refreshTimer.stop();
            }
        });

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(owner);
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

    private void refreshStatus() {
        statusLabel.setText("Status: " + multiplayer.status());
        if (multiplayer.isConnected()) {
            connectButton.setText("Disconnect");
            setConnectionFieldsEnabled(false);
            return;
        }
        if (multiplayer.isHosting()) {
            connectButton.setText("Cancel host");
            setConnectionFieldsEnabled(false);
            return;
        }
        connectButton.setText("Connect");
        setConnectionFieldsEnabled(true);
    }

    private void setConnectionFieldsEnabled(boolean enabled) {
        localRadio.setEnabled(enabled);
        tcpRadio.setEnabled(enabled);
        hostRadio.setEnabled(enabled);
        guestRadio.setEnabled(enabled);
        pathField.setEnabled(enabled);
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        browseButton.setEnabled(enabled);
    }
}
