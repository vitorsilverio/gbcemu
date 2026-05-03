package dev.vitorsilverio.gbcemu;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class LauncherWindow {

    private final EmulatorMenuActions menuActions;

    public LauncherWindow(EmulatorMenuActions menuActions) {
        this.menuActions = menuActions;
    }

    public void show() {
        JFrame window = new JFrame("GBC EMU");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(640, 576));
        window.setContentPane(panel);
        window.setJMenuBar(createMenuBar());
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu emulatorMenu = new JMenu("Emulator");

        JMenuItem start = new JMenuItem("Start ROM...");
        start.addActionListener(event -> menuActions.openRom().run());
        emulatorMenu.add(start);

        JMenuItem configureBios = new JMenuItem("Set default BIOS...");
        configureBios.addActionListener(event -> menuActions.configureDefaultBios().run());
        emulatorMenu.add(configureBios);

        menuBar.add(emulatorMenu);
        return menuBar;
    }
}
