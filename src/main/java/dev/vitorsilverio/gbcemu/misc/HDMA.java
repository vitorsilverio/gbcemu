package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.snapshot.Savable;
import dev.vitorsilverio.gbcemu.snapshot.Snapshottable;

import java.util.List;

public class HDMA implements MachineCycle, MemorySpace, Snapshottable {

    private final int HDMA1 = 0xFF51;
    private final int HDMA2 = 0xFF52;
    private final int HDMA3 = 0xFF53;
    private final int HDMA4 = 0xFF54;
    private final int HDMA5 = 0xFF55;
    private final List<Integer> HDMA_REGISTERS = List.of(
            HDMA1, HDMA2, HDMA3, HDMA4, HDMA5
    );


    private final Bus bus;
    @Savable private boolean active;
    @Savable private int total;
    @Savable private int sourceAddress;
    @Savable private int destinationAddress;
    @Savable private int mode;
    @Savable private int cycles;
    @Savable private int counter;
    @Savable private boolean completed = true;

    public HDMA(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void tick() {
        if (!active) {
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
            case HDMA1 -> {
                return (byte) ((sourceAddress >> 8) & 0xFF);
            }
            case HDMA2 -> {
                return (byte) (sourceAddress & 0xF0);
            }
            case HDMA3 -> {
                return (byte) ((destinationAddress >> 8) & 0xFF);
            }
            case HDMA4 -> {
                return (byte) (destinationAddress & 0xFF);
            }
            case HDMA5 -> {
                if (completed) {
                    return (byte) 0xFF;
                }
                int remainingBlocks = Math.max(0, ((total - counter) / 0x10) - 1);
                return (byte) (remainingBlocks | (active ? 0x00 : 0x80));
            }
        }
        return 0;
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
                completed = false;
            }
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
}
