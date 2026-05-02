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
    public void tick() {
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

    public byte readAfterTicks(int address, int ticks) {
        State state = stateAfterTicks(ticks);
        return switch (address) {
            case DIVIDER_REG -> (byte)((state.systemCounter() >> 8) & 0xFF);
            case TIMER_COUNTER_REG -> state.timerCounter();
            case TIMER_MODULO_REG -> state.timerModulo();
            case TIMER_CONTROL_REG -> (byte) (0xF8 | (state.timerControl() & 0x07));
            default -> throw new IllegalArgumentException("Address " + address + " not found in any memory space");
        };
    }

    public boolean requestsInterruptAfterTicks(int ticks) {
        return stateAfterTicks(ticks).interruptRequested();
    }

    private State stateAfterTicks(int ticks) {
        State state = new State(systemCounter, timerCounter, timerModulo, timerControl, overflowDelay, false);
        for (int i = 0; i < ticks; i++) {
            state = tick(state);
        }
        return state;
    }

    private State tick(State state) {
        int overflowDelay = state.overflowDelay();
        byte timerCounter = state.timerCounter();
        boolean interruptRequested = state.interruptRequested();
        if (overflowDelay > 0) {
            overflowDelay--;
            if (overflowDelay == 0) {
                timerCounter = state.timerModulo();
                interruptRequested = true;
            }
        }

        boolean oldSignal = timerSignal(state.systemCounter(), state.timerControl());
        int systemCounter = (state.systemCounter() + 1) & 0xFFFF;
        if (oldSignal && !timerSignal(systemCounter, state.timerControl())) {
            int value = (timerCounter & 0xFF) + 1;
            timerCounter = (byte) value;
            if (value > 0xFF) {
                timerCounter = 0;
                overflowDelay = 4;
            }
        }
        return new State(systemCounter, timerCounter, state.timerModulo(), state.timerControl(), overflowDelay, interruptRequested);
    }

    private boolean timerSignal(int systemCounter, byte timerControl) {
        if ((timerControl & 0x04) == 0) {
            return false;
        }
        int bit = TIMER_BITS[timerControl & 0x03];
        return (systemCounter & (1 << bit)) != 0;
    }

    private record State(int systemCounter, byte timerCounter, byte timerModulo, byte timerControl, int overflowDelay, boolean interruptRequested) {
    }
}
