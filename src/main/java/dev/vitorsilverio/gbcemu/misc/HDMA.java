package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;

import java.util.Arrays;
import java.util.List;

public class HDMA implements MachineCycle, MemorySpace, Stateful<HdmaState> {

    private final int HDMA1 = 0xFF51;
    private final int HDMA2 = 0xFF52;
    private final int HDMA3 = 0xFF53;
    private final int HDMA4 = 0xFF54;
    private final int HDMA5 = 0xFF55;
    private final List<Integer> HDMA_REGISTERS = Arrays.asList(
            HDMA1, HDMA2, HDMA3, HDMA4, HDMA5
    );


    private final Bus bus;
    private boolean active;
    private int total;
    private int sourceAddress;
    private int destinationAddress;
    private int mode;
    private int cycles;
    private int counter;
    private boolean completed = true;
    private boolean hblankBlockTransferred;

    public HDMA(Bus bus) {
        this.bus = bus;
    }

    @Override
    public HdmaState saveState() {
        return new HdmaState(
                active,
                total,
                sourceAddress,
                destinationAddress,
                mode,
                cycles,
                counter,
                completed,
                hblankBlockTransferred
        );
    }

    @Override
    public void loadState(HdmaState state) {
        active = state.active();
        total = Math.max(0, state.total());
        sourceAddress = state.sourceAddress() & 0xFFFF;
        destinationAddress = 0x8000 | (state.destinationAddress() & 0x1FFF);
        mode = state.mode() & 0x01;
        cycles = Math.max(0, state.cycles());
        counter = Math.max(0, state.counter());
        completed = state.completed();
        hblankBlockTransferred = state.hblankBlockTransferred();
    }

    @Override
    public void tick() {
        if (!active) {
            return;
        }
        if (mode == 1 && hblankBlockTransferred) {
            return;
        }

        cycles++;

        if (cycles < 8) {
            return;
        }

        for (int i = 0; i < 0x10 && counter < total; i++) {
            bus.write(destinationAddress, bus.read(sourceAddress));
            sourceAddress = (sourceAddress + 1) & 0xFFFF;
            destinationAddress = 0x8000 | ((destinationAddress + 1) & 0x1FFF);
            counter++;
        }
        cycles = 0;
        hblankBlockTransferred = mode == 1;

        if (counter >= total) {
            active = false;
            completed = true;
        }
    }

    @Override
    public boolean contains(int address) {
        return HDMA_REGISTERS.contains(address);
    }

    @Override
    public byte read(int address) {
        switch (address) {
            case HDMA5 -> {
                if (completed) {
                    return (byte) 0xFF;
                }
                return (byte) (remainingBlocksMinusOne() | (active ? 0x00 : 0x80));
            }
            default -> {
                return (byte) 0xFF;
            }
        }
    }

    @Override
    public void write(int address, byte value) {
        switch (address) {
            case HDMA1 -> sourceAddress = (sourceAddress & 0x00FF) | ((value & 0xFF) << 8);
            case HDMA2 -> sourceAddress = (sourceAddress & 0xFF00) | (value & 0xF0);
            case HDMA3 -> destinationAddress = 0x8000 | ((value & 0x1F) << 8) | (destinationAddress & 0x00F0);
            case HDMA4 -> destinationAddress = 0x8000 | (destinationAddress & 0x1F00) | (value & 0xF0);
            case HDMA5 -> {
                if (active && mode == 1 && (value & 0x80) == 0) {
                    active = false;
                    completed = false;
                    return;
                }
                active = true;
                mode = (value & 0x80) >> 7;
                total = ((value & 0x7F) + 1) * 0x10;
                counter = 0;
                cycles = 0;
                hblankBlockTransferred = false;
                completed = false;
            }
        }

    }

    public void leaveHBlank() {
        if (mode == 1) {
            hblankBlockTransferred = false;
            cycles = 0;
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isHBlankMode() {
        return mode == 1;
    }

    public boolean isGeneralPurposeMode() {
        return mode == 0;
    }

    public boolean hblankBlockTransferred() {
        return hblankBlockTransferred;
    }

    public int remainingBlocksMinusOne() {
        return Math.max(0, ((total - counter) / 0x10) - 1);
    }
}
