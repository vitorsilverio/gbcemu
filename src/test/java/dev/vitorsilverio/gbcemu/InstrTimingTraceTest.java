package dev.vitorsilverio.gbcemu;

import dev.vitorsilverio.gbcemu.audio.Apu;
import dev.vitorsilverio.gbcemu.cartridge.Cart;
import dev.vitorsilverio.gbcemu.cartridge.CartFactory;
import dev.vitorsilverio.gbcemu.controller.ButtonType;
import dev.vitorsilverio.gbcemu.controller.Controller;
import dev.vitorsilverio.gbcemu.cpu.Cpu;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.Bios;
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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

class InstrTimingTraceTest {

    @Test
    @Disabled("Diagnostic trace for the blargg instr_timing ROM; requires local test-roms/instr_timing.gb.")
    void traceInstrTimingFailure() {
        Bus bus = new Bus();
        bus.addMemorySpace(new Bios(new File("cgb_bios.bin")));
        Cpu cpu = new Cpu(bus);
        TraceTimer timer = new TraceTimer(bus, cpu::toString);
        Ppu ppu = new Ppu(bus);
        Apu apu = Apu.muted();
        HDMA hdma = new HDMA(bus);
        DMA dma = new DMA(bus);
        TraceSerial serial = new TraceSerial(bus);
        WorkRam workRam = new WorkRam();

        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
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
        cpu.setCycleCallback(() -> tickSystem(bus, cpu, timer, ppu, apu, hdma, dma, serial));

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

            if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                cpu.tick();
            } else {
                tickSystem(bus, cpu, timer, ppu, apu, hdma, dma, serial);
            }
        }

        pcs.forEach(System.out::println);
        timer.events().forEach(System.out::println);
        dump(bus, 0xC000, 0xC0);
        dump(bus, 0xC880, 0xC0);
        dump(bus, 0xD7F0, 0x30);
        System.out.println(serial.text());
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderHaltBugScreen() throws IOException {
        renderRomScreen("test-roms/halt_bug.gb", "target/halt_bug-screen.png", 15_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderInterruptTimeScreen() throws IOException {
        renderRomScreen("test-roms/interrupt_time/interrupt_time.gb", "target/interrupt_time-screen.png", 15_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/cgb_sound.gb", "target/cgb_sound-screen.png", 60_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundRegisterScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/01-registers.gb", "target/cgb_sound-01-registers-screen.png", 20_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundPowerScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/11-regs after power.gb", "target/cgb_sound-11-regs-after-power-screen.png", 20_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundLengthScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/02-len ctr.gb", "target/cgb_sound-02-len-ctr-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundTriggerScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/03-trigger.gb", "target/cgb_sound-03-trigger-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundSweepScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/04-sweep.gb", "target/cgb_sound-04-sweep-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundSweepDetailsScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/05-sweep details.gb", "target/cgb_sound-05-sweep-details-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundOverflowOnTriggerScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/06-overflow on trigger.gb", "target/cgb_sound-06-overflow-on-trigger-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundLengthSweepPeriodSyncScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/07-len sweep period sync.gb", "target/cgb_sound-07-len-sweep-period-sync-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundLengthDuringPowerScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/08-len ctr during power.gb", "target/cgb_sound-08-len-ctr-during-power-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundWaveReadWhileOnScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/09-wave read while on.gb", "target/cgb_sound-09-wave-read-while-on-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundWaveTriggerWhileOnScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/10-wave trigger while on.gb", "target/cgb_sound-10-wave-trigger-while-on-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void renderCgbSoundWaveScreen() throws IOException {
        renderRomScreen("test-roms/cgb_sound/rom_singles/12-wave.gb", "target/cgb_sound-12-wave-screen.png", 30_000_000);
    }

    @Test
    @EnabledIfSystemProperty(named = "gbcemu.diagnostics", matches = "true")
    void traceInterruptTimeFailure() {
        Bus bus = new Bus();
        Cpu cpu = new Cpu(bus);
        Timer timer = new Timer(bus);
        Ppu ppu = new Ppu(bus);
        Apu apu = Apu.muted();
        HDMA hdma = new HDMA(bus);
        DMA dma = new DMA(bus);
        TraceSerial serial = new TraceSerial(bus);
        WorkRam workRam = new WorkRam();

        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        bus.addMemorySpace(new Joypad(bus, new IdleController()));
        bus.addMemorySpace(workRam);
        bus.addMemorySpace(new EchoRam(workRam));
        bus.addMemorySpace(new ZeroPage());
        TraceKey1 key1 = new TraceKey1(cpu::toString);
        bus.addMemorySpace(CartFactory.fromFile(new File("test-roms/interrupt_time/interrupt_time.gb"), null));
        bus.addMemorySpace(new Key0(ppu::setCgbMode));
        bus.addMemorySpace(key1);
        bus.addMemorySpace(new InfraredPort());
        bus.addMemorySpace(new UnusedIoRegisters());
        bus.addMemorySpace(serial);
        cpu.setPc(0x0100);
        cpu.setSp(0xFFFE);
        cpu.setA((byte) 0x11);
        cpu.setB((byte) 0x00);
        cpu.setC((byte) 0x13);
        cpu.setD((byte) 0x00);
        cpu.setE((byte) 0xD8);
        cpu.setH((byte) 0x01);
        cpu.setL((byte) 0x4D);
        cpu.setCycleCallback(() -> tickSystem(bus, cpu, timer, ppu, apu, hdma, dma, serial));

        Queue<String> pcs = new ArrayDeque<>();
        for (int tick = 0; tick < 15_000_000; tick++) {
            if (pcs.size() == 300) {
                pcs.remove();
            }
            pcs.add(String.format("%07d %s IF=%02X IE=%02X DIV=%02X TIMA=%02X TMA=%02X TAC=%02X",
                    tick,
                    cpu,
                    bus.read(0xFF0F) & 0xFF,
                    bus.read(0xFFFF) & 0xFF,
                    bus.read(0xFF04) & 0xFF,
                    bus.read(0xFF05) & 0xFF,
                    bus.read(0xFF06) & 0xFF,
                    bus.read(0xFF07) & 0xFF));

            if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                cpu.tick();
            } else {
                tickSystem(bus, cpu, timer, ppu, apu, hdma, dma, serial);
            }
        }

        pcs.forEach(System.out::println);
        key1.events().forEach(System.out::println);
        dump(bus, 0xC000, 0x500);
        dump(bus, 0xD800, 0x100);
        dump(bus, 0xFF80, 0x80);
    }

    private void renderRomScreen(String romPath, String outputPath, int ticks) throws IOException {
        Bus bus = new Bus();
        Cpu cpu = new Cpu(bus);
        Timer timer = new Timer(bus);
        Cart cart = CartFactory.fromFile(new File(romPath), null);
        Ppu ppu = new Ppu(bus, cart.getHeader().isCgbCompatible());
        Apu apu = Apu.muted();
        HDMA hdma = new HDMA(bus);
        DMA dma = new DMA(bus);
        TraceSerial serial = new TraceSerial(bus);
        WorkRam workRam = new WorkRam();

        bus.addMemorySpace(timer);
        bus.addMemorySpace(ppu);
        bus.addMemorySpace(apu);
        bus.addMemorySpace(hdma);
        bus.addMemorySpace(dma);
        bus.addMemorySpace(new Joypad(bus, new IdleController()));
        bus.addMemorySpace(workRam);
        bus.addMemorySpace(new EchoRam(workRam));
        bus.addMemorySpace(new ZeroPage());
        bus.addMemorySpace(cart);
        bus.addMemorySpace(new Key0(ppu::setCgbMode));
        bus.addMemorySpace(new Key1());
        bus.addMemorySpace(new InfraredPort());
        bus.addMemorySpace(new UnusedIoRegisters());
        bus.addMemorySpace(serial);
        cpu.setPc(0x0100);
        cpu.setSp(0xFFFE);
        cpu.setA((byte) 0x11);
        cpu.setB((byte) 0x00);
        cpu.setC((byte) 0x13);
        cpu.setD((byte) 0x00);
        cpu.setE((byte) 0xD8);
        cpu.setH((byte) 0x01);
        cpu.setL((byte) 0x4D);
        cpu.setCycleCallback(() -> tickSystem(bus, cpu, timer, ppu, apu, hdma, dma, serial));

        for (int tick = 0; tick < ticks; tick++) {
            if (!hdma.isActive() || !hdma.isGeneralPurposeMode()) {
                cpu.tick();
            } else {
                tickSystem(bus, cpu, timer, ppu, apu, hdma, dma, serial);
            }
        }

        File output = new File(outputPath);
        output.getParentFile().mkdirs();
        ImageIO.write((BufferedImage) ppu.getFrameBuffer(), "png", output);
        System.out.println(output.getAbsolutePath());
        System.out.println(serial.text());
    }

    private void tickSystem(Bus bus, Cpu cpu, Timer timer, Ppu ppu, Apu apu, HDMA hdma, DMA dma, TraceSerial serial) {
        if (hdma.isActive()) {
            if (hdma.isHBlankMode() && ppu.isHBlank()) {
                hdma.tick();
            }
            if (hdma.isGeneralPurposeMode()) {
                hdma.tick();
            }
        }
        if (dma.isActive()) {
            dma.tick();
        }
        timer.tick();
        serial.tick();
        ppu.tick();
        apu.tick();
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

    private static class TraceTimer extends Timer {
        private final Supplier<String> cpuState;
        private final Queue<String> events = new ArrayDeque<>();
        private int tick;
        private int lastTima;

        private TraceTimer(Bus bus, Supplier<String> cpuState) {
            super(bus);
            this.cpuState = cpuState;
        }

        @Override
        public byte read(int address) {
            return super.read(address);
        }

        @Override
        public void write(int address, byte value) {
            add(String.format("%07d %s WRITE %04X <- %02X", tick, cpuState.get(), address, value & 0xFF));
            super.write(address, value);
        }

        @Override
        public void tick() {
            tick++;
            super.tick();
            int tima = super.read(0xFF05) & 0xFF;
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

    private static class TraceSerial implements MemorySpace, MachineCycle {
        private static final int TRANSFER_START = 0x80;
        private static final int CLOCK_SELECT = 0x01;
        private final Bus bus;
        private final StringBuilder text = new StringBuilder();
        private int data;
        private int transferCyclesRemaining;
        private int outgoingByte;

        private TraceSerial(Bus bus) {
            this.bus = bus;
        }

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
            } else if ((value & TRANSFER_START) != 0 && (value & CLOCK_SELECT) != 0) {
                outgoingByte = data;
                transferCyclesRemaining = 4096;
            }
        }

        @Override
        public void tick() {
            if (transferCyclesRemaining <= 0) {
                return;
            }
            transferCyclesRemaining--;
            if (transferCyclesRemaining == 0) {
                text.append((char) outgoingByte);
                data = 0xFF;
                bus.requestInterrupt(dev.vitorsilverio.gbcemu.interrupt.Interrupt.SERIAL);
            }
        }

        private String text() {
            return text.toString();
        }
    }

    private static class TraceKey1 extends Key1 {
        private final Supplier<String> cpuState;
        private final Queue<String> events = new ArrayDeque<>();

        private TraceKey1(Supplier<String> cpuState) {
            this.cpuState = cpuState;
        }

        @Override
        public byte read(int address) {
            byte value = super.read(address);
            add(String.format("%s READ KEY1 -> %02X", cpuState.get(), value & 0xFF));
            return value;
        }

        @Override
        public void write(int address, byte value) {
            add(String.format("%s WRITE KEY1 <- %02X", cpuState.get(), value & 0xFF));
            super.write(address, value);
        }

        @Override
        public boolean switchSpeedIfPrepared() {
            boolean switched = super.switchSpeedIfPrepared();
            add(String.format("%s STOP KEY1 switched=%s value=%02X", cpuState.get(), switched, super.read(0xFF4D) & 0xFF));
            return switched;
        }

        private Queue<String> events() {
            return events;
        }

        private void add(String event) {
            if (events.size() == 200) {
                events.remove();
            }
            events.add(event);
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
