package dev.vitorsilverio.gbcemu.link;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkTransferStateTest {

    @Test
    void startsInternalClockTransferWithMonotonicNonZeroId() {
        LinkTransferState state = new LinkTransferState();

        state.queueInternalClockByte(0xAA);
        LinkTransferState.OutgoingClockPulse first = state.startInternalClockTransfer();
        state.completeInFlightTransfer();
        state.queueInternalClockByte(0x55);
        LinkTransferState.OutgoingClockPulse second = state.startInternalClockTransfer();

        assertEquals(1, first.id());
        assertEquals(0xAA, first.outgoingByte());
        assertEquals(2, second.id());
        assertEquals(0x55, second.outgoingByte());
    }

    @Test
    void pendingIncomingClockCanBeTakenOnce() {
        LinkTransferState state = new LinkTransferState();

        state.bufferIncomingClock(7, 0x42);
        LinkTransferState.IncomingClockPulse pending = state.takePendingIncomingClock();

        assertEquals(7, pending.transferId());
        assertEquals(0x42, pending.value());
        assertFalse(state.hasPendingIncomingClock());
    }

    @Test
    void resetClearsTransferWindow() {
        LinkTransferState state = new LinkTransferState();

        state.queueInternalClockByte(0xAA);
        state.bufferIncomingClock(3, 0x55);
        assertTrue(state.isTransferWindow());

        state.reset();

        assertFalse(state.isTransferWindow());
    }
}
