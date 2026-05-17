package dev.vitorsilverio.gbcemu.peripherals;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.interrupt.Interrupt;
import dev.vitorsilverio.gbcemu.link.LinkCable;
import dev.vitorsilverio.gbcemu.link.SerialLinkSnapshot;
import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.memory.MemorySpace;
import dev.vitorsilverio.gbcemu.misc.Key1;
import dev.vitorsilverio.gbcemu.snapshot.Stateful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final LinkCable linkCable;

    private int SB = 0;
    private int SC = 0;
    private int transferCyclesRemaining = 0;
    private int outgoingByte = 0;
    private boolean isTransferActive = false;
    private boolean isMasterWaitingResponse = false;

    public Serial(Bus bus, LinkCable linkCable) {
        this.bus = bus;
        this.linkCable = linkCable;
        if (linkCable != null) {
            linkCable.attachSerial(this::onPeerByteReceived);
        }
    }

    private void onPeerByteReceived(int received) {
        if (!isTransferActive) {
            return;
        }

        if (isMaster()) {
            boolean isEffectiveMaster = linkCable == null || !linkCable.isConnected() || linkCable.isEffectiveMaster();
            if (isMasterWaitingResponse || !isEffectiveMaster) {
                completeMasterTransfer(received);
            }
        } else {
            if (linkCable != null && linkCable.isConnected()) {
                linkCable.onExternalClockRespond(outgoingByte);
            }
            completeSlaveTransfer(received);
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
            case (SC_REGISTER) -> {
                int value = SC;
                // Arbitration override: If both want to be Master, 
                // the effective slave sees itself as Slave.
                if (linkCable != null && linkCable.isConnected() && isInternalClockSelected()) {
                    if (!linkCable.isEffectiveMaster()) {
                        value &= ~CLOCK_SELECT;
                    }
                }
                yield value;
            }
            default -> 0;
        };
    }

    @Override
    public void write(int address, byte value) {
        if (address == SB_REGISTER) {
            SB = value & 0xFF;
        } else if (address == SC_REGISTER) {
            int previousSC = SC;
            SC = value & (TRANSFER_START | CLOCK_SPEED | CLOCK_SELECT);

            boolean transferStartSet = (SC & TRANSFER_START) != 0;
            boolean wasTransferActive = (previousSC & TRANSFER_START) != 0;

            if (transferStartSet && !wasTransferActive) {
                startTransfer();
            } else if (!transferStartSet && wasTransferActive) {
                transferCyclesRemaining = 0;
                isMasterWaitingResponse = false;
                isTransferActive = false;
                publishLinkState();
            }
        }
    }

    @Override
    public void tick() {
        if (!isTransferActive || !isMaster() || isMasterWaitingResponse) return;

        // Effective Slave does not drive the clock
        if (linkCable != null && linkCable.isConnected() && !linkCable.isEffectiveMaster()) {
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
        SB = received & 0xFF;
        SC &= ~TRANSFER_START;
        isTransferActive = false;
        isMasterWaitingResponse = false;
        publishLinkState();
        bus.requestInterrupt(Interrupt.SERIAL);
    }

    private void completeSlaveTransfer(int received) {
        SB = received & 0xFF;
        SC &= ~TRANSFER_START;
        isTransferActive = false;
        publishLinkState();
        bus.requestInterrupt(Interrupt.SERIAL);
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
        long frameNumber = bus.findMemorySpace(dev.vitorsilverio.gbcemu.ppu.Ppu.class)
                .map(dev.vitorsilverio.gbcemu.ppu.Ppu::getFrameNumber)
                .orElse(0L);
        linkCable.reportLocalState(new SerialLinkSnapshot(
                isTransferActive,
                isInternalClockSelected(),
                isMasterWaitingResponse,
                isMaster(),
                outgoingByte,
                SC,
                frameNumber
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
        return isInternalClockSelected();
    }

    public boolean isTransferActive() {
        return isTransferActive;
    }

    public boolean isMasterWaitingResponse() {
        return isMasterWaitingResponse;
    }

    public boolean isInternalClockSelected() {
        return (SC & CLOCK_SELECT) != 0;
    }

    public boolean isFastClockSelected() {
        return (SC & CLOCK_SPEED) != 0;
    }
}
