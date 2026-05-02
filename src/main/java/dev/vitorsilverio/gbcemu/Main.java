package dev.vitorsilverio.gbcemu;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        var emulator = new Emulator(
                new File("cgb_bios.bin"),
                new File("test-roms/cpu_instrs.gb"),
                new File("save.sav")
        );
        //emulator.skipBios();
        emulator.start();

    }
}