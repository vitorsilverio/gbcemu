package dev.vitorsilverio.gbcemu.gui;

import dev.vitorsilverio.gbcemu.misc.GameSharkDevice;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.regex.Pattern;

public class CheatsWindow {

    private static final Pattern SUPPORTED_CODE = Pattern.compile("#?01[A-Fa-f0-9]{6}");

    private final GameSharkDevice gameSharkDevice;
    private final JFrame frame = new JFrame("GameShark Codes");
    private final JTextArea textArea;
    private final JTextField status = new JTextField();

    public CheatsWindow(GameSharkDevice gameSharkDevice) {
        this.gameSharkDevice = gameSharkDevice;
        this.textArea = new JTextArea(gameSharkDevice.getCheats(), 18, 48);
        initializeWindow();
    }

    private void initializeWindow() {
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(680, 460));
        frame.setLocationRelativeTo(null);

        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setLineWrap(false);
        textArea.setTabSize(4);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(new JScrollPane(textArea), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        frame.setContentPane(content);
        updateStatus();
        frame.pack();
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("Paste one GameShark code per line");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(title, BorderLayout.WEST);

        JLabel hint = new JLabel("# disables a line", SwingConstants.RIGHT);
        panel.add(hint, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        status.setEditable(false);
        status.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        footer.add(status, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton validate = new JButton("Validate");
        validate.addActionListener(event -> validateCodes(true));
        buttons.add(validate);

        JButton apply = new JButton("Apply");
        apply.addActionListener(event -> applyChanges());
        buttons.add(apply);

        JButton close = new JButton("Close");
        close.addActionListener(event -> frame.dispose());
        buttons.add(close);

        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void applyChanges() {
        if (!validateCodes(true)) {
            return;
        }
        gameSharkDevice.setCheats(textArea.getText());
        updateStatus();
        JOptionPane.showMessageDialog(frame,
                "Cheats updated.",
                "GameShark Codes",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean validateCodes(boolean showDialog) {
        StringBuilder errors = new StringBuilder();
        int valid = 0;
        String[] lines = textArea.getText().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) {
                continue;
            }
            if (!SUPPORTED_CODE.matcher(line).matches()) {
                errors.append("Line ").append(i + 1).append(": ").append(line).append('\n');
            } else {
                valid++;
            }
        }

        if (!errors.isEmpty()) {
            status.setText(valid + " valid code(s), invalid lines found.");
            if (showDialog) {
                JOptionPane.showMessageDialog(frame,
                        "Unsupported or invalid codes:\n" + errors,
                        "Invalid cheats",
                        JOptionPane.WARNING_MESSAGE);
            }
            return false;
        }

        status.setText(valid + " valid code(s).");
        if (showDialog) {
            JOptionPane.showMessageDialog(frame,
                    valid + " valid code(s).",
                    "GameShark Codes",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        return true;
    }

    private void updateStatus() {
        validateCodes(false);
    }
}
