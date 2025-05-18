package dev.vitorsilverio.gbcemu.util;

public class Debug {
    public static boolean isDebugging() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean()
                .getInputArguments().toString().contains("jdwp");
    }
}
