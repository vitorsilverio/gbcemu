package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.controller.KeyboardController;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.*;
import dev.vitorsilverio.gbcemu.misc.DMA;
import dev.vitorsilverio.gbcemu.misc.HDMA;
import dev.vitorsilverio.gbcemu.misc.Key0;
import dev.vitorsilverio.gbcemu.peripherals.Joypad;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import dev.vitorsilverio.gbcemu.ppu.Display;
import dev.vitorsilverio.gbcemu.ppu.Ppu;

import java.io.File;

public class Emulator {

    private final Cpu cpu;
    private final Timer timer;
    private final Ppu ppu;
    private final Apu apu;
    private final HDMA hdma;
    private final DMA dma;
    private final Display display;

    public Emulator(File biosFile, File romFile, File saveFile) {
        Bus bus = new Bus();
        Bios bios = new Bios(biosFile);
        bus.addMemorySpace(bios);
        this.cpu = new Cpu(bus);
        this.timer = new Timer(bus);
        this.ppu = new Ppu(bus);
        this.apu = new Apu();
        this.hdma = new HDMA(bus);
        this.dma = new DMA(bus);
        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        KeyboardController controller = new KeyboardController();
        var joypad = new Joypad(bus, controller);
        bus.addMemorySpace(joypad);
        var workRam = new WorkRam();
        bus.addMemorySpace(workRam);
        var echoRam = new EchoRam(workRam);
        bus.addMemorySpace(echoRam);
        var zeroPage = new ZeroPage();
        bus.addMemorySpace(zeroPage);
        bus.addMemorySpace(new Cart(romFile));
        bus.addMemorySpace(new Key0());
        this.display = new Display(ppu, controller);
        bus.addMemorySpace(new Serial());
    }


    public void start() {
        Thread displayThread = new Thread(display);
        displayThread.start();
        while (true) {
            if (hdma.isActive()) {
                hdma.tick();
            }
            if (dma.isActive()) {
                dma.tick();
            }
            cpu.tick();
            timer.tick();
            ppu.tick();
            apu.tick();
        }
    }

    public void skipBios() {
        cpu.getBus().write(0xFF50, (byte) 0x01);
        cpu.setPc(0x100);
        cpu.setSp(0xfffe);
        cpu.setA((byte) 0x01);
        cpu.setB((byte) 0x00);
        cpu.setC((byte) 0x13);
        cpu.setD((byte) 0x00);
        cpu.setE((byte) 0xD8);
        cpu.setH((byte) 0x01);
        cpu.setL((byte) 0x4D);

    }
}
