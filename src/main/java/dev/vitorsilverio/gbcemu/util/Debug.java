package dev.vitorsilverio.gbcemu.util;

public class Debug {
    public static boolean isDebugging() {
        String jdwpTransport = System.getProperty("jdwp.transport", "");
        String javaCommand = System.getProperty("sun.java.command", "");
        return jdwpTransport.contains("dt_socket") || javaCommand.contains("jdwp");
    }
}
