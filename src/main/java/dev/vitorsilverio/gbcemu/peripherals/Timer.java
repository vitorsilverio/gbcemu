package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;

import java.util.List;

public class Timer implements MemorySpace, MachineCycle {

    private static final int DIVIDER_REG = 0xFF04;
    private static final int TIMER_COUNTER_REG = 0xFF05;
    private static final int TIMER_MODULO_REG = 0xFF06;
    private static final int TIMER_CONTROL_REG = 0xFF07;
    private static final List<Integer> REGISTERS = List.of(DIVIDER_REG, TIMER_COUNTER_REG, TIMER_MODULO_REG, TIMER_CONTROL_REG);
    private static final int[] CLOCK_SPEEDS = {1024, 16, 64, 256};


    private int divider;
    private byte timerCounter;
    private byte timerModulo;

    private int clockSpeed = 1024;
    private boolean timerEnabled = false;

    private final Bus bus;

    public Timer(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void tick() {
        divider++;
        if (timerEnabled) {
            if ((divider & (clockSpeed - 1)) == 0) {
                timerCounter++;
                if (timerCounter == 0) { // Overflow
                    timerCounter = timerModulo;
                    bus.requestInterrupt(Interrupt.TIMER);
                }
            }
        }
    }

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address);
    }

    @Override
    public byte read(int address) {
        switch (address) {
            case DIVIDER_REG:
                return (byte)((divider >> 8) & 0xFF);
            case TIMER_COUNTER_REG:
                return timerCounter;
            case TIMER_MODULO_REG:
                return timerModulo;
            case TIMER_CONTROL_REG:
                var value = 0;
                if (timerEnabled) {
                    value |= 0b100;
                }
                value |= (clockSpeed == 16 ? 0b01 : clockSpeed == 64 ? 0b10 : 0b11);
                return (byte) value;
            default:
                throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }
    }

    @Override
    public void write(int address, byte value) {
        switch (address) {
            case DIVIDER_REG:
                divider = 0;
                break;
            case TIMER_COUNTER_REG:
                timerCounter = value;
                break;
            case TIMER_MODULO_REG:
                timerModulo = value;
                break;
            case TIMER_CONTROL_REG:
                timerEnabled = (value & 0b100) != 0;
                clockSpeed = CLOCK_SPEEDS[value & 0b11];
                break;
            default:
                throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }

    }
}
