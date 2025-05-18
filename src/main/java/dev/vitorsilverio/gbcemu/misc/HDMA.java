package dev.vitorsilverio.gbcemu.misc;

import dev.vitorsilverio.gbcemu.MachineCycle;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;

import java.util.List;

public class HDMA implements MachineCycle, MemorySpace {


    private final int HDMA1 = 0xFF51;
    private final int HDMA2 = 0xFF52;
    private final int HDMA3 = 0xFF53;
    private final int HDMA4 = 0xFF54;
    private final int HDMA5 = 0xFF55;
    private final List<Integer> HDMA_REGISTERS = List.of(
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

    public HDMA(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void tick() {
        cycles++;

        if (cycles <= 8) {
            return;
        }
        bus.write(destinationAddress + counter, bus.read(sourceAddress + counter));
        counter++;
        cycles = 0;
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
                return (byte) (sourceAddress & 0xF);
            }
            case HDMA3 -> {
                return (byte) ((destinationAddress >> 8) & 0xFF);
            }
            case HDMA4 -> {
                return (byte) (destinationAddress & 0xFF);
            }
            case HDMA5 -> {
                return (byte) ((byte) (((total - counter)/16) -1) | (active ? 0x80 : 0));
            }
        }
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        switch (address) {
            case HDMA1 -> sourceAddress = (sourceAddress & 0x00FF) | ((value & 0xFF) << 8);
            case HDMA2 -> sourceAddress = (sourceAddress & 0xFF00) | (value & 0xFC);
            case HDMA3 -> destinationAddress = (destinationAddress & 0x00FF) | ((value & 0xFF) << 8);
            case HDMA4 -> destinationAddress = (destinationAddress & 0xFF00) | (value & 0xFC);
            case HDMA5 -> {
                active = true;
                mode = (value & 0x80) >> 7;
                total = (value & 0x7F) * 16 - 1 ;
                cycles = 0;
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
        return mode == 1;
    }
}
