package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.misc.Key1;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HexFormat;
import java.util.List;

public class Serial implements MemorySpace, MachineCycle, Stateful<SerialState> {

    private final Logger logger = LoggerFactory.getLogger(Serial.class);

    private final int SB_REGISTER = 0xFF01;
    private final int SC_REGISTER = 0xFF02;
    private static final int TRANSFER_START = 0x80;
    private static final int CLOCK_SPEED = 0x02;
    private static final int CLOCK_SELECT = 0x01;
    private static final int NORMAL_SPEED_CYCLES_PER_TRANSFER = 4096;
    private static final int FAST_SPEED_CYCLES_PER_TRANSFER = 128;
    private final List<Integer> registers = List.of(SB_REGISTER, SC_REGISTER);
    private final Bus bus;
    private final StringBuilder text = new StringBuilder();
    private final StringBuilder transcript = new StringBuilder();
    private int SB = 0;
    private int SC = 0;
    private int transferCyclesRemaining;
    private int outgoingByte;

    public Serial(Bus bus) {
        this.bus = bus;
    }

    @Override
    public SerialState saveState() {
        return new SerialState(SB, SC, transferCyclesRemaining, outgoingByte, text.toString());
    }

    @Override
    public void loadState(SerialState state) {
        SB = state.sb() & 0xFF;
        SC = state.sc() & (TRANSFER_START | CLOCK_SPEED | CLOCK_SELECT);
        transferCyclesRemaining = state.transferCyclesRemaining();
        outgoingByte = state.outgoingByte() & 0xFF;
        text.setLength(0);
        text.append(state.pendingText() == null ? "" : state.pendingText());
        transcript.setLength(0);
        transcript.append(text);
    }

    @Override
    public boolean contains(int address) {
        return registers.contains(address);
    }

    @Override
    public byte read(int address) {
        return (byte) switch (address) {
            case (SB_REGISTER) -> SB;
            case (SC_REGISTER) -> SC;
            default -> 0;
        };
    }

    @Override
    public void write(int address, byte value) {
        if (address == SB_REGISTER) {
            SB = value;
        } else if (address == SC_REGISTER) {
            SC = value & (TRANSFER_START | CLOCK_SPEED | CLOCK_SELECT);
            if ((SC & TRANSFER_START) != 0 && (SC & CLOCK_SELECT) != 0) {
                startInternalTransfer();
            } else {
                transferCyclesRemaining = 0;
            }
        }

    }

    @Override
    public void tick() {
        if (transferCyclesRemaining <= 0) {
            return;
        }
        transferCyclesRemaining--;
        if (transferCyclesRemaining == 0) {
            finishTransfer();
        }
    }

    private void startInternalTransfer() {
        outgoingByte = SB & 0xFF;
        transferCyclesRemaining = cyclesPerTransfer();
    }

    private int cyclesPerTransfer() {
        int cycles = (SC & CLOCK_SPEED) == 0
                ? NORMAL_SPEED_CYCLES_PER_TRANSFER
                : FAST_SPEED_CYCLES_PER_TRANSFER;
        boolean doubleSpeed = bus.findMemorySpace(Key1.class)
                .map(Key1::isDoubleSpeed)
                .orElse(false);
        return doubleSpeed ? cycles / 2 : cycles;
    }

    private void finishTransfer() {
        byte[] primitiveData = new byte[]{(byte) outgoingByte};
        logger.info("Serial data: " + HexFormat.of().formatHex(primitiveData));
        appendText((byte) outgoingByte);
        SB = 0xFF;
        SC &= ~TRANSFER_START;
        bus.requestInterrupt(Interrupt.SERIAL);
    }

    private void appendText(byte value) {
        int unsignedValue = value & 0xFF;
        if (unsignedValue == '\n') {
            transcript.append('\n');
            logger.info("Serial text: {}", text);
            text.setLength(0);
        } else if (unsignedValue >= 0x20 && unsignedValue <= 0x7E) {
            char character = (char) unsignedValue;
            text.append(character);
            transcript.append(character);
        }
    }

    public String transcript() {
        return transcript.toString();
    }
}
