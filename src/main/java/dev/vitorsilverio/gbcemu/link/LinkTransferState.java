package dev.vitorsilverio.gbcemu.link;

import java.util.ArrayDeque;

/**
 * Tracks the transient byte exchange state carried by the virtual link cable.
 */
final class LinkTransferState {

    private static final int MAX_PENDING_INCOMING_PULSES = 1;
    private static final int PENDING_INCOMING_PULSE_TIMEOUT_CYCLES = 8192;

    private Integer pendingInternalClockByte;
    private final ArrayDeque<IncomingClockPulse> pendingIncomingPulses = new ArrayDeque<>();
    private OutgoingClockPulse outgoingClockPulse;
    private int nextTransferId;

    void queueInternalClockByte(int outgoingByte) {
        pendingInternalClockByte = outgoingByte & 0xFF;
    }

    boolean canStartInternalClockTransfer() {
        return pendingInternalClockByte != null && outgoingClockPulse == null;
    }

    OutgoingClockPulse startInternalClockTransfer() {
        if (!canStartInternalClockTransfer()) {
            throw new IllegalStateException("No internal clock transfer can be started");
        }
        outgoingClockPulse = new OutgoingClockPulse(nextTransferId(), pendingInternalClockByte);
        pendingInternalClockByte = null;
        return outgoingClockPulse;
    }

    boolean matchesInFlightTransfer(int transferId) {
        return outgoingClockPulse != null && outgoingClockPulse.id() == (transferId & 0xFF);
    }

    void completeInFlightTransfer() {
        outgoingClockPulse = null;
    }

    void cancelLocalTransfer() {
        pendingInternalClockByte = null;
        outgoingClockPulse = null;
    }

    void cancelAll() {
        pendingInternalClockByte = null;
        pendingIncomingPulses.clear();
        outgoingClockPulse = null;
    }

    void bufferIncomingClock(int transferId, int value) {
        if (pendingIncomingPulses.size() >= MAX_PENDING_INCOMING_PULSES) {
            pendingIncomingPulses.removeFirst();
        }
        pendingIncomingPulses.addLast(new IncomingClockPulse(transferId & 0xFF, value & 0xFF, 0));
    }

    void agePendingIncomingClocks(ExpiredIncomingClockHandler handler) {
        int count = pendingIncomingPulses.size();
        for (int index = 0; index < count; index++) {
            IncomingClockPulse pulse = pendingIncomingPulses.removeFirst();
            int age = pulse.ageCycles() + 1;
            if (age >= PENDING_INCOMING_PULSE_TIMEOUT_CYCLES) {
                handler.onExpired(pulse);
            } else {
                pendingIncomingPulses.addLast(new IncomingClockPulse(pulse.transferId(), pulse.value(), age));
            }
        }
    }

    boolean hasPendingIncomingClock() {
        return !pendingIncomingPulses.isEmpty();
    }

    int pendingIncomingClockCount() {
        return pendingIncomingPulses.size();
    }

    boolean hasPendingInternalClockByte() {
        return pendingInternalClockByte != null;
    }

    boolean hasInFlightTransfer() {
        return outgoingClockPulse != null;
    }

    IncomingClockPulse takePendingIncomingClock() {
        return pendingIncomingPulses.removeFirst();
    }

    boolean isTransferWindow() {
        return pendingInternalClockByte != null
                || !pendingIncomingPulses.isEmpty()
                || outgoingClockPulse != null;
    }

    void reset() {
        cancelAll();
        nextTransferId = 0;
    }

    private int nextTransferId() {
        nextTransferId = (nextTransferId + 1) & 0xFF;
        if (nextTransferId == 0) {
            nextTransferId = 1;
        }
        return nextTransferId;
    }

    record IncomingClockPulse(int transferId, int value, int ageCycles) {
    }

    record OutgoingClockPulse(int id, int outgoingByte) {
    }

    interface ExpiredIncomingClockHandler {
        void onExpired(IncomingClockPulse pulse);
    }
}
