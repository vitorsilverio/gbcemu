package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.misc.GameSharkDevice;

import javax.swing.*;
import java.awt.*;

public class CheatsWindow {

    private final GameSharkDevice gameSharkDevice;

    public CheatsWindow(GameSharkDevice gameSharkDevice){
        this.gameSharkDevice = gameSharkDevice;
        initializeWindow();
    }

    private void initializeWindow() {
        final var frame = new JFrame("GameShark Codes");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(400,400);
        frame.setLocationRelativeTo(null);
        final var textArea = new JTextArea(gameSharkDevice.getCheats());
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new JLabel("Cheats"), BorderLayout.WEST);
        contentPane.add(textArea, BorderLayout.CENTER);
        var buttonPanel = new JPanel(new BorderLayout());
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        var buttonOK = new JButton("OK");
        var buttonCancel = new JButton("Cancel");
        buttonPanel.add(buttonOK, BorderLayout.WEST);
        buttonPanel.add(buttonCancel, BorderLayout.EAST);
        buttonOK.addActionListener(event -> {
            gameSharkDevice.setCheats(textArea.getText());
            frame.dispose();
        });
        buttonCancel.addActionListener(event -> frame.dispose());
        frame.setVisible(true);
        frame.show();
    }


}
