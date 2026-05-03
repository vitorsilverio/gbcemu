package dev.vitorsilverio.gbcemu.cpu;

import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.peripherals.Timer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpuTimingTest {

    @Test
    void fourCycleInstructionConsumesFourTicks() {
        Cpu cpu = new Cpu(busWithMemory());
        int[] ticks = countCpuCycles(cpu);

        cpu.tick();

        assertEquals(0x0001, cpu.getPc());
        assertEquals(4, ticks[0]);

        cpu.tick();

        assertEquals(0x0002, cpu.getPc());
        assertEquals(8, ticks[0]);
    }

    @Test
    void interruptConsumesTwentyTicksBeforeExecutingVectorInstruction() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = countCpuCycles(cpu);
        cpu.setPc(0x1234);
        cpu.setSp(0xFFFE);
        cpu.setIme(true);
        bus.write(0xFFFF, (byte) Interrupt.VBLANK.getMask());
        bus.write(0xFF0F, (byte) Interrupt.VBLANK.getMask());

        cpu.tick();

        assertEquals(Interrupt.VBLANK.getVectorAddress(), cpu.getPc());
        assertEquals(20, ticks[0]);

        cpu.tick();

        assertEquals(Interrupt.VBLANK.getVectorAddress() + 1, cpu.getPc());
        assertEquals(24, ticks[0]);
    }

    @Test
    void cpuMemoryWritesBecomeVisibleAtEndOfInstructionCycles() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = new int[1];
        cpu.setA((byte) 0x05);
        bus.write(0x0000, (byte) 0xE0);
        bus.write(0x0001, (byte) 0x07);
        cpu.setCycleCallback(() -> {
            ticks[0]++;
            assertEquals(0x00, bus.read(0xFF07) & 0xFF);
        });

        cpu.tick();

        assertEquals(0x05, bus.read(0xFF07) & 0xFF);
        assertEquals(12, ticks[0]);
    }

    @Test
    void takenConditionalReturnConsumesTwentyCycles() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = countCpuCycles(cpu);
        cpu.setSp(0xFFFC);
        cpu.setZeroFlag(false);
        bus.write(0x0000, (byte) 0xC0);
        bus.writeWord(0xFFFC, 0x1234);

        cpu.tick();

        assertEquals(0x1234, cpu.getPc());
        assertEquals(20, ticks[0]);

        cpu.tick();

        assertEquals(0x1235, cpu.getPc());
        assertEquals(24, ticks[0]);
    }

    @Test
    void highIoReadObservesTimerInterruptAtMemoryReadCycle() {
        Bus bus = new Bus();
        Timer timer = new Timer(bus);
        bus.addMemorySpace(timer);
        bus.addMemorySpace(new MemorySpace() {
            private final byte[] bytes = new byte[0x10000];

            {
                bytes[0x0000] = (byte) 0xF0;
                bytes[0x0001] = (byte) 0x0F;
            }

            @Override
            public boolean contains(int address) {
                return true;
            }

            @Override
            public byte read(int address) {
                return bytes[address & 0xFFFF];
            }

            @Override
            public void write(int address, byte value) {
                bytes[address & 0xFFFF] = value;
            }
        });
        Cpu cpu = new Cpu(bus);
        timer.write(0xFF05, (byte) 0xFF);
        timer.write(0xFF07, (byte) 0x05);
        for (int i = 0; i < 12; i++) {
            timer.tick();
        }
        cpu.setCycleCallback(timer::tick);

        cpu.tick();

        assertEquals(Interrupt.TIMER.getMask(), cpu.getA() & 0xFF);
    }

    private int[] countCpuCycles(Cpu cpu) {
        int[] ticks = new int[1];
        cpu.setCycleCallback(() -> ticks[0]++);
        return ticks;
    }

    private Bus busWithMemory() {
        Bus bus = new Bus();
        bus.addMemorySpace(new MemorySpace() {
            private final byte[] bytes = new byte[0x10000];

            @Override
            public boolean contains(int address) {
                return true;
            }

            @Override
            public byte read(int address) {
                return bytes[address & 0xFFFF];
            }

            @Override
            public void write(int address, byte value) {
                bytes[address & 0xFFFF] = value;
            }
        });
        return bus;
    }
}
