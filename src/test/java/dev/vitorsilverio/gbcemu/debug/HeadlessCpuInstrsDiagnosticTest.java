package dev.vitorsilverio.gbcemu.debug;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.*;
import dev.vitorsilverio.gbcemu.misc.*;
import dev.vitorsilverio.gbcemu.peripherals.Joypad;
import dev.vitorsilverio.gbcemu.peripherals.Serial;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.function.Consumer;

class HeadlessCpuInstrsDiagnosticTest {

    @Test
    void traceInjectedInstructions() {
        Bus bus = new Bus();
        bus.addMemorySpace(new Bios(new File("dmg_bios.bin")));
        Cpu cpu = new Cpu(bus);
        Timer timer = new Timer(bus);
        Ppu ppu = new Ppu(bus);
        Apu apu = new Apu();
        HDMA hdma = new HDMA(bus);
        DMA dma = new DMA(bus);
        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        bus.addMemorySpace(new Joypad(bus, new DummyController()));
        WorkRam workRam = new WorkRam();
        bus.addMemorySpace(workRam);
        bus.addMemorySpace(new EchoRam(workRam));
        bus.addMemorySpace(new ZeroPage());
        bus.addMemorySpace(new Cart(new File("test-roms/cpu_instrs.gb")));
        bus.addMemorySpace(new Key0());
        bus.addMemorySpace(new Key1());
        bus.addMemorySpace(new Serial(bus));

        int def8Count = 0;
        for (long ticks = 0; ticks < 200_000_000L && def8Count < 80; ticks++) {
            if (hdma.isActive()) {
                hdma.tick();
            }
            if (dma.isActive()) {
                dma.tick();
            }
            int pc = cpu.getPc();
            if (pc == 0xDEF8) {
                int bc = cpu.getBc();
                int de = cpu.getDe();
                int hl = cpu.getHl();
                System.out.printf("before[%02d] op=%02X %02X %02X sp=%04X af=%04X bc=%04X de=%04X hl=%04X [bc]=%02X [de]=%02X [hl]=%02X bank=%02X%n",
                        def8Count,
                        bus.read(0xDEF8) & 0xFF, bus.read(0xDEF9) & 0xFF, bus.read(0xDEFA) & 0xFF,
                        cpu.getSp(), cpu.getAf(), bc, de, hl,
                        bus.read(bc) & 0xFF, bus.read(de) & 0xFF, bus.read(hl) & 0xFF,
                        bus.read(0xFF70) & 0xFF);
                cpu.tick();
                System.out.printf("after [%02d] pc=%04X sp=%04X af=%04X bc=%04X de=%04X hl=%04X [bc]=%02X [de]=%02X [hl]=%02X%n",
                        def8Count, cpu.getPc(), cpu.getSp(), cpu.getAf(), cpu.getBc(), cpu.getDe(), cpu.getHl(),
                        bus.read(cpu.getBc()) & 0xFF, bus.read(cpu.getDe()) & 0xFF, bus.read(cpu.getHl()) & 0xFF);
                def8Count++;
            } else {
                cpu.tick();
            }
            timer.tick();
            ppu.tick();
            apu.tick();
        }
        org.junit.jupiter.api.Assertions.assertTrue(def8Count > 0);
    }

    private static class DummyController implements Controller {
        public boolean isButtonA_Pressed() { return false; }
        public boolean isButtonB_Pressed() { return false; }
        public boolean isButtonStart_Pressed() { return false; }
        public boolean isButtonSelect_Pressed() { return false; }
        public boolean isButtonUp_Pressed() { return false; }
        public boolean isButtonDown_Pressed() { return false; }
        public boolean isButtonLeft_Pressed() { return false; }
        public boolean isButtonRight_Pressed() { return false; }
        public void eventEmitter(Consumer<ButtonType> onButtonPress) {}
    }
}
