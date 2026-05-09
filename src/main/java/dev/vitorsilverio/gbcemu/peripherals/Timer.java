package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.misc.Key1;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.List;

public class Timer implements MemorySpace, MachineCycle, Stateful<TimerState> {

    private static final int DIVIDER_REG = 0xFF04;
    private static final int TIMER_COUNTER_REG = 0xFF05;
    private static final int TIMER_MODULO_REG = 0xFF06;
    private static final int TIMER_CONTROL_REG = 0xFF07;
    private static final List<Integer> REGISTERS = List.of(DIVIDER_REG, TIMER_COUNTER_REG, TIMER_MODULO_REG, TIMER_CONTROL_REG);
    private static final int[] TIMER_BITS = {9, 3, 5, 7};


    private int systemCounter;
    private byte timerCounter;
    private byte timerModulo;
    private byte timerControl;

    private int overflowDelay;

    private final Bus bus;

    public Timer(Bus bus) {
        this.bus = bus;
    }

    @Override
    public TimerState saveState() {
        return new TimerState(systemCounter, timerCounter, timerModulo, timerControl, overflowDelay);
    }

    @Override
    public void loadState(TimerState state) {
        systemCounter = state.systemCounter() & 0xFFFF;
        timerCounter = state.timerCounter();
        timerModulo = state.timerModulo();
        timerControl = (byte) (state.timerControl() & 0x07);
        overflowDelay = state.overflowDelay();
    }

    @Override
    public void tick() {
        int increments = bus.findMemorySpace(Key1.class)
                .filter(Key1::isDoubleSpeed)
                .map(key1 -> 2)
                .orElse(1);
        for (int i = 0; i < increments; i++) {
            tickSystemCounter();
        }
    }

    private void tickSystemCounter() {
        if (overflowDelay > 0) {
            overflowDelay--;
            if (overflowDelay == 0) {
                timerCounter = timerModulo;
                bus.requestInterrupt(Interrupt.TIMER);
            }
        }

        boolean oldSignal = timerSignal();
        systemCounter = (systemCounter + 1) & 0xFFFF;
        incrementOnFallingEdge(oldSignal);
    }

    @Override
    public boolean contains(int address) {
        return REGISTERS.contains(address);
    }

    @Override
    public byte read(int address) {
        return switch (address) {
            case DIVIDER_REG -> (byte) ((systemCounter >> 8) & 0xFF);
            case TIMER_COUNTER_REG -> timerCounter;
            case TIMER_MODULO_REG -> timerModulo;
            case TIMER_CONTROL_REG -> (byte) (0xF8 | (timerControl & 0x07));
            default -> throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        };
    }

    @Override
    public void write(int address, byte value) {
        boolean oldSignal;
        switch (address) {
            case DIVIDER_REG:
                oldSignal = timerSignal();
                systemCounter = 0;
                incrementOnFallingEdge(oldSignal);
                break;
            case TIMER_COUNTER_REG:
                timerCounter = value;
                overflowDelay = 0;
                break;
            case TIMER_MODULO_REG:
                timerModulo = value;
                if (overflowDelay == 1) {
                    timerCounter = value;
                }
                break;
            case TIMER_CONTROL_REG:
                oldSignal = timerSignal();
                timerControl = (byte) (value & 0x07);
                incrementOnFallingEdge(oldSignal);
                break;
            default:
                throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        }

    }

    private void incrementOnFallingEdge(boolean oldSignal) {
        if (oldSignal && !timerSignal()) {
            incrementTimerCounter();
        }
    }

    private boolean timerSignal() {
        if ((timerControl & 0x04) == 0) {
            return false;
        }
        int bit = TIMER_BITS[timerControl & 0x03];
        return (systemCounter & (1 << bit)) != 0;
    }

    private void incrementTimerCounter() {
        int value = (timerCounter & 0xFF) + 1;
        timerCounter = (byte) value;
        if (value > 0xFF) {
            timerCounter = 0;
            overflowDelay = 4;
        }
    }

}
