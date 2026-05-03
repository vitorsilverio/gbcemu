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
    void pushRegisterPairConsumesSixteenCycles() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = countCpuCycles(cpu);
        cpu.setSp(0xFFFE);
        cpu.setBc(0x1234);
        bus.write(0x0000, (byte) 0xC5);

        cpu.tick();

        assertEquals(0xFFFC, cpu.getSp());
        assertEquals(0x1234, bus.readWord(0xFFFC));
        assertEquals(16, ticks[0]);
    }

    @Test
    void notTakenRelativeJumpConsumesEightCyclesEvenWithNegativeOffset() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = countCpuCycles(cpu);
        cpu.setZeroFlag(true);
        bus.write(0x0000, (byte) 0x20);
        bus.write(0x0001, (byte) 0xFE);

        cpu.tick();

        assertEquals(0x0002, cpu.getPc());
        assertEquals(8, ticks[0]);
    }

    @Test
    void takenRelativeJumpConsumesTwelveCyclesWithSingleImmediateRead() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = countCpuCycles(cpu);
        cpu.setZeroFlag(false);
        bus.write(0x0000, (byte) 0x20);
        bus.write(0x0001, (byte) 0xFE);

        cpu.tick();

        assertEquals(0x0000, cpu.getPc());
        assertEquals(12, ticks[0]);
    }

    @Test
    void cbBitReferenceHlConsumesTwelveCycles() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        int[] ticks = countCpuCycles(cpu);
        cpu.setHl(0xC000);
        bus.write(0x0000, (byte) 0xCB);
        bus.write(0x0001, (byte) 0x46);
        bus.write(0xC000, (byte) 0x01);

        cpu.tick();

        assertEquals(0x0002, cpu.getPc());
        assertEquals(12, ticks[0]);
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

        assertEquals(Interrupt.TIMER.getMask(), cpu.getA() & Interrupt.TIMER.getMask());
    }

    @Test
    void eiEnablesInterruptsAfterFollowingInstruction() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        bus.write(0x0000, (byte) 0xFB);
        bus.write(0x0001, (byte) 0x00);
        bus.write(0x0002, (byte) 0x00);
        bus.write(0xFFFF, (byte) Interrupt.VBLANK.getMask());
        bus.write(0xFF0F, (byte) Interrupt.VBLANK.getMask());

        cpu.tick();

        assertEquals(0x0001, cpu.getPc());
        assertEquals(false, cpu.isIme());

        cpu.tick();

        assertEquals(0x0002, cpu.getPc());
        assertEquals(true, cpu.isIme());

        cpu.tick();

        assertEquals(Interrupt.VBLANK.getVectorAddress(), cpu.getPc());
    }

    @Test
    void haltBugOnlyTriggersWhenImeIsDisabledAndInterruptIsPending() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        bus.write(0x0000, (byte) 0x76);
        bus.write(0x0001, (byte) 0x3C);
        bus.write(0xFFFF, (byte) Interrupt.VBLANK.getMask());
        bus.write(0xFF0F, (byte) Interrupt.VBLANK.getMask());

        cpu.tick();
        cpu.tick();

        assertEquals(0x0001, cpu.getPc());
        assertEquals(1, cpu.getA() & 0xFF);
    }

    @Test
    void haltBugReadsNextOpcodeByteTwiceForImmediateInstructions() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        bus.write(0x0000, (byte) 0x76);
        bus.write(0x0001, (byte) 0x11);
        bus.write(0x0002, (byte) 0x04);
        bus.write(0x0003, (byte) 0x0C);
        bus.write(0xFFFF, (byte) Interrupt.VBLANK.getMask());
        bus.write(0xFF0F, (byte) Interrupt.VBLANK.getMask());

        cpu.tick();
        cpu.tick();

        assertEquals(0x0411, cpu.getDe());
        assertEquals(0x0003, cpu.getPc());
    }

    @Test
    void haltWithImeEnabledServicesInterruptBeforeNextInstruction() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        cpu.setIme(true);
        cpu.setSp(0xFFFE);
        bus.write(0x0000, (byte) 0x76);
        bus.write(0x0001, (byte) 0x3C);
        bus.write(0xFFFF, (byte) Interrupt.VBLANK.getMask());
        bus.write(0xFF0F, (byte) Interrupt.VBLANK.getMask());

        cpu.tick();

        assertEquals(Interrupt.VBLANK.getVectorAddress(), cpu.getPc());
        assertEquals(0, cpu.getA() & 0xFF);
    }

    @Test
    void eiBeforeBuggedHaltServicesInterruptWithHaltAsReturnAddress() {
        Bus bus = busWithMemory();
        Cpu cpu = new Cpu(bus);
        cpu.setSp(0xFFFE);
        bus.write(0x0000, (byte) 0xFB);
        bus.write(0x0001, (byte) 0x76);
        bus.write(0x0002, (byte) 0x00);
        bus.write(0xFFFF, (byte) Interrupt.VBLANK.getMask());
        bus.write(0xFF0F, (byte) Interrupt.VBLANK.getMask());

        cpu.tick();
        cpu.tick();

        assertEquals(0x0001, cpu.getPc());
        assertEquals(true, cpu.isIme());

        cpu.tick();

        assertEquals(Interrupt.VBLANK.getVectorAddress(), cpu.getPc());
        assertEquals(0x0001, bus.readWord(0xFFFC));
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
