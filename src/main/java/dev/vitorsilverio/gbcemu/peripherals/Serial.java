package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.link.LinkCableListener;
import dev.vitorsilverio.gbcemu.link.SerialLinkState;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.misc.Key1;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

public class Serial implements MemorySpace, MachineCycle, Stateful<SerialState> {

    private final Logger logger = LoggerFactory.getLogger(Serial.class);

    private final int SB_REGISTER = 0xFF01;
    private final int SC_REGISTER = 0xFF02;
    private static final int TRANSFER_START = 0x80;
    private static final int CLOCK_SPEED = 0x02;
    private static final int CLOCK_SELECT = 0x01;
    private static final int UNUSED_READ_BITS = 0x7C;
    private static final int NORMAL_SPEED_CYCLES_PER_TRANSFER = 4096;
    private static final int FAST_SPEED_CYCLES_PER_TRANSFER = 128;
    private static final int TRANSFER_HISTORY_SIZE = 32;

    private final List<Integer> registers = Arrays.asList(SB_REGISTER, SC_REGISTER);
    private final Bus bus;
    private final StringBuilder text = new StringBuilder();
    private final StringBuilder transcript = new StringBuilder();
    private final LinkCable linkCable;

    private int SB = 0;
    private int SC = 0;
    private int transferCyclesRemaining = 0;
    private int outgoingByte = 0;
    private int lastCompletedOutgoingByte = 0xFF;
    private int lastCompletedIncomingByte = 0xFF;
    private long completedTransfers = 0;
    private final int[] transferHistoryOutgoing = new int[TRANSFER_HISTORY_SIZE];
    private final int[] transferHistoryIncoming = new int[TRANSFER_HISTORY_SIZE];
    private final boolean[] transferHistoryInternalClock = new boolean[TRANSFER_HISTORY_SIZE];
    private final boolean[] transferHistoryCompletedAsMaster = new boolean[TRANSFER_HISTORY_SIZE];
    private int transferHistoryCursor = 0;
    private int transferHistoryCount = 0;
    private boolean isTransferActive = false;
    private boolean isMasterWaitingResponse = false;

    public Serial(Bus bus, LinkCable linkCable) {
        this.bus = bus;
        this.linkCable = linkCable;
        if (linkCable != null) {
            linkCable.attachSerial(new LinkCableListener() {
                @Override
                public OptionalInt onExternalClockedByte(int value) {
                    return Serial.this.onExternalClockedByte(value);
                }

                @Override
                public void onInternalClockResult(int value) {
                    Serial.this.onInternalClockResult(value);
                }

                @Override
                public void onLinkDisconnected() {
                    Serial.this.onLinkDisconnected();
                }
            });
        }
    }

    private void onLinkDisconnected() {
        if (isTransferActive && isMasterWaitingResponse) {
            completeMasterTransfer(0xFF);
        }
    }

    private OptionalInt onExternalClockedByte(int received) {
        if (!isTransferActive) {
            return OptionalInt.empty();
        }

        int response = outgoingByte & 0xFF;
        if (usesInternalClock()) {
            if (linkCable != null && !linkCable.isEffectiveMaster()) {
                completeSlaveTransfer(received);
                return OptionalInt.of(response);
            }
            if (linkCable == null || linkCable.shouldCompleteInternalClockTransfer(isMasterWaitingResponse)) {
                completeMasterTransfer(received);
                return OptionalInt.of(response);
            }
            return OptionalInt.empty();
        } else {
            completeSlaveTransfer(received);
            return OptionalInt.of(response);
        }
    }

    private void onInternalClockResult(int received) {
        if (!isTransferActive || !usesInternalClock()) {
            return;
        }

        if (linkCable == null || linkCable.shouldCompleteInternalClockTransfer(isMasterWaitingResponse)) {
            completeMasterTransfer(received);
        }
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
        isTransferActive = false;
        isMasterWaitingResponse = false;
        text.setLength(0);
        text.append(state.pendingText() == null ? "" : state.pendingText());
        transcript.setLength(0);
        transcript.append(text);
        publishLinkState();
    }

    @Override
    public boolean contains(int address) {
        return registers.contains(address);
    }

    @Override
    public byte read(int address) {
        return (byte) switch (address) {
            case (SB_REGISTER) -> SB;
            case (SC_REGISTER) -> visibleSerialControl() | UNUSED_READ_BITS;
            default -> 0;
        };
    }

    @Override
    public void write(int address, byte value) {
        if (address == SB_REGISTER) {
            SB = value & 0xFF;
        } else if (address == SC_REGISTER) {
            int previousSC = SC;
            int written = value & (TRANSFER_START | CLOCK_SPEED | CLOCK_SELECT);
            SC = linkCable == null ? written : linkCable.normalizeSerialControlWrite(written);

            boolean transferStartSet = (SC & TRANSFER_START) != 0;
            boolean wasTransferActive = (previousSC & TRANSFER_START) != 0;
            boolean serialControlChanged = SC != previousSC;

            if (transferStartSet && !wasTransferActive) {
                startTransfer();
            } else if (!transferStartSet && wasTransferActive) {
                transferCyclesRemaining = 0;
                isMasterWaitingResponse = false;
                isTransferActive = false;
                publishLinkState();
            } else if (isTransferActive && serialControlChanged) {
                publishLinkState();
            }
        }
    }

    @Override
    public void tick() {
        if (!isTransferActive || !usesInternalClock() || isMasterWaitingResponse) return;

        if (linkCable != null && !linkCable.shouldDriveInternalClock()) {
            return;
        }

        transferCyclesRemaining--;
        if (transferCyclesRemaining == 0) {
            if (linkCable != null && linkCable.isConnected()) {
                isMasterWaitingResponse = true;
                publishLinkState();
                linkCable.onInternalClockComplete(outgoingByte);
            } else {
                // Disconnected Master receives 0xFF
                completeMasterTransfer(0xFF);
            }
        }
    }

    private void completeMasterTransfer(int received) {
        recordCompletedTransfer(received, true);
        SB = received & 0xFF;
        SC &= ~TRANSFER_START;
        isTransferActive = false;
        isMasterWaitingResponse = false;
        publishLinkState();
        bus.requestInterrupt(Interrupt.SERIAL);
    }

    private void completeSlaveTransfer(int received) {
        recordCompletedTransfer(received, false);
        SB = received & 0xFF;
        SC &= ~TRANSFER_START;
        isTransferActive = false;
        isMasterWaitingResponse = false;
        publishLinkState();
        bus.requestInterrupt(Interrupt.SERIAL);
    }

    private void recordCompletedTransfer(int received, boolean completedAsMaster) {
        lastCompletedOutgoingByte = outgoingByte & 0xFF;
        lastCompletedIncomingByte = received & 0xFF;
        transferHistoryOutgoing[transferHistoryCursor] = lastCompletedOutgoingByte;
        transferHistoryIncoming[transferHistoryCursor] = lastCompletedIncomingByte;
        transferHistoryInternalClock[transferHistoryCursor] = usesInternalClock();
        transferHistoryCompletedAsMaster[transferHistoryCursor] = completedAsMaster;
        transferHistoryCursor = (transferHistoryCursor + 1) % TRANSFER_HISTORY_SIZE;
        transferHistoryCount = Math.min(transferHistoryCount + 1, TRANSFER_HISTORY_SIZE);
        completedTransfers++;
    }

    private void startTransfer() {
        outgoingByte = SB & 0xFF;
        isTransferActive = true;
        transferCyclesRemaining = cyclesPerTransfer();
        isMasterWaitingResponse = false;
        
        publishLinkState();
    }

    private void publishLinkState() {
        if (linkCable == null) {
            return;
        }
        linkCable.reportLocalState(new SerialLinkState(
                isTransferActive,
                isInternalClockSelected(),
                isMasterWaitingResponse,
                outgoingByte,
                SC
        ));
    }

    private int cyclesPerTransfer() {
        int cycles = (SC & CLOCK_SPEED) == 0
                ? NORMAL_SPEED_CYCLES_PER_TRANSFER
                : FAST_SPEED_CYCLES_PER_TRANSFER;
        if (isInternalClockSelected() && isDoubleSpeed()) {
            cycles /= 2;
        }
        return cycles;
    }

    private boolean isDoubleSpeed() {
        return bus.findMemorySpace(Key1.class)
                .map(Key1::isDoubleSpeed)
                .orElse(false);
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

    public boolean isMaster() {
        return usesInternalClock() && (linkCable == null || linkCable.isEffectiveMaster());
    }

    public boolean isTransferActive() {
        return isTransferActive;
    }

    public boolean isMasterWaitingResponse() {
        return isMasterWaitingResponse;
    }

    public boolean isInternalClockSelected() {
        return usesInternalClock();
    }

    private boolean usesInternalClock() {
        return (SC & CLOCK_SELECT) != 0;
    }

    private int visibleSerialControl() {
        return linkCable == null ? SC : linkCable.visibleSerialControl(SC);
    }

    public boolean isFastClockSelected() {
        return (SC & CLOCK_SPEED) != 0;
    }

    public int lastCompletedOutgoingByte() {
        return lastCompletedOutgoingByte;
    }

    public int lastCompletedIncomingByte() {
        return lastCompletedIncomingByte;
    }

    public long completedTransfers() {
        return completedTransfers;
    }

    public int transferHistoryCount() {
        return transferHistoryCount;
    }

    public int transferHistoryOutgoing(int index) {
        return transferHistoryOutgoing[historySlot(index)];
    }

    public int transferHistoryIncoming(int index) {
        return transferHistoryIncoming[historySlot(index)];
    }

    public boolean transferHistoryInternalClock(int index) {
        return transferHistoryInternalClock[historySlot(index)];
    }

    public boolean transferHistoryCompletedAsMaster(int index) {
        return transferHistoryCompletedAsMaster[historySlot(index)];
    }

    private int historySlot(int index) {
        if (index < 0 || index >= transferHistoryCount) {
            throw new IndexOutOfBoundsException(index);
        }
        return Math.floorMod(transferHistoryCursor - transferHistoryCount + index, TRANSFER_HISTORY_SIZE);
    }
}
