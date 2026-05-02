package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.cartridge.CartFactory;
import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.EchoRam;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.memory.UnusedIoRegisters;
import dev.vitorsilverio.gbcemu.memory.WorkRam;
import dev.vitorsilverio.gbcemu.memory.ZeroPage;
import dev.vitorsilverio.gbcemu.misc.DMA;
import dev.vitorsilverio.gbcemu.misc.HDMA;
import dev.vitorsilverio.gbcemu.misc.InfraredPort;
import dev.vitorsilverio.gbcemu.misc.Key0;
import dev.vitorsilverio.gbcemu.misc.Key1;
import dev.vitorsilverio.gbcemu.peripherals.Joypad;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import dev.vitorsilverio.gbcemu.ppu.Ppu;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

class InstrTimingTraceTest {

    @Test
    @Disabled("Diagnostic trace for the blargg instr_timing ROM; requires local test-roms/instr_timing.gb.")
    void traceInstrTimingFailure() {
        Bus bus = new Bus();
        Cpu cpu = new Cpu(bus);
        TraceTimer timer = new TraceTimer(new Timer(bus), cpu::toString);
        Ppu ppu = new Ppu(bus);
        HDMA hdma = new HDMA(bus);
        DMA dma = new DMA(bus);
        TraceSerial serial = new TraceSerial();
        WorkRam workRam = new WorkRam();

        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        bus.addMemorySpace(new Joypad(bus, new IdleController()));
        bus.addMemorySpace(workRam);
        bus.addMemorySpace(new EchoRam(workRam));
        bus.addMemorySpace(new ZeroPage());
        bus.addMemorySpace(CartFactory.fromFile(new File("test-roms/instr_timing.gb"), null));
        bus.addMemorySpace(new Key0());
        bus.addMemorySpace(new Key1());
        bus.addMemorySpace(new InfraredPort());
        bus.addMemorySpace(new UnusedIoRegisters());
        bus.addMemorySpace(serial);

        cpu.setPc(0x100);
        cpu.setSp(0xFFFE);

        Queue<String> pcs = new ArrayDeque<>();
        for (int tick = 0; tick < 5_000_000 && !serial.text().contains("Failed") && !serial.text().contains("Passed"); tick++) {
            if (pcs.size() == 200) {
                pcs.remove();
            }
            pcs.add(String.format("%07d %s IF=%02X IE=%02X",
                    tick,
                    cpu,
                    bus.read(0xFF0F) & 0xFF,
                    bus.read(0xFFFF) & 0xFF));

            if (hdma.isActive()) {
                if (hdma.isHBlankMode()) {
                    if (ppu.isHBlank()) {
                        hdma.tick();
                    }
                }
                if (hdma.isGeneralPurposeMode()) {
                    hdma.tick();
                }
            }
            if (dma.isActive()) {
                dma.tick();
            }
            if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                cpu.tick();
            }
            timer.tick();
            ppu.tick();
        }

        pcs.forEach(System.out::println);
        timer.events().forEach(System.out::println);
        dump(bus, 0xC000, 0xC0);
        dump(bus, 0xC880, 0xC0);
        dump(bus, 0xD7F0, 0x30);
        System.out.println(serial.text());
    }

    private void dump(Bus bus, int address, int length) {
        StringBuilder text = new StringBuilder(String.format("%04X:", address));
        for (int i = 0; i < length; i++) {
            if ((i & 0x0F) == 0) {
                text.append(System.lineSeparator()).append(String.format("%04X:", address + i));
            }
            text.append(String.format(" %02X", bus.read(address + i) & 0xFF));
        }
        System.out.println(text);
    }

    private static class TraceTimer implements MemorySpace, MachineCycle {
        private final Timer timer;
        private final Supplier<String> cpuState;
        private final Queue<String> events = new ArrayDeque<>();
        private int tick;
        private int lastTima;
        private int lastIf;

        private TraceTimer(Timer timer, Supplier<String> cpuState) {
            this.timer = timer;
            this.cpuState = cpuState;
        }

        @Override
        public boolean contains(int address) {
            return timer.contains(address);
        }

        @Override
        public byte read(int address) {
            byte value = timer.read(address);
            return value;
        }

        @Override
        public void write(int address, byte value) {
            add(String.format("%07d %s WRITE %04X <- %02X", tick, cpuState.get(), address, value & 0xFF));
            timer.write(address, value);
        }

        @Override
        public void tick() {
            tick++;
            timer.tick();
            int tima = timer.read(0xFF05) & 0xFF;
            if (tima != lastTima) {
                add(String.format("%07d %s TIMA %02X -> %02X", tick, cpuState.get(), lastTima, tima));
                lastTima = tima;
            }
        }

        private Queue<String> events() {
            return events;
        }

        private void add(String event) {
            if (events.size() == 500) {
                events.remove();
            }
            events.add(event);
        }
    }

    private static class TraceSerial implements MemorySpace {
        private final StringBuilder text = new StringBuilder();
        private int data;

        @Override
        public boolean contains(int address) {
            return address == 0xFF01 || address == 0xFF02;
        }

        @Override
        public byte read(int address) {
            return address == 0xFF01 ? (byte) data : 0x01;
        }

        @Override
        public void write(int address, byte value) {
            if (address == 0xFF01) {
                data = value & 0xFF;
            } else if ((value & 0x80) != 0) {
                text.append((char) data);
            }
        }

        private String text() {
            return text.toString();
        }
    }

    private static class IdleController implements Controller {
        @Override
        public boolean isButtonA_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonB_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonStart_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonSelect_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonUp_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonDown_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonLeft_Pressed() {
            return false;
        }

        @Override
        public boolean isButtonRight_Pressed() {
            return false;
        }

        @Override
        public void eventEmitter(java.util.function.Consumer<ButtonType> onButtonPress) {
        }
    }
}
