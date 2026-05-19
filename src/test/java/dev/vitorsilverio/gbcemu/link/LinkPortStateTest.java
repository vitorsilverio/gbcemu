package dev.vitorsilverio.gbcemu.link;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkPortStateTest {

    @Test
    void internalClockTransferCanStartOnlyWhenWaitingForResult() {
        LinkPortState ready = new LinkPortState(new SerialLinkState(true, true, true, 0x55, 0x81));
        LinkPortState armedButNotClockedYet = new LinkPortState(new SerialLinkState(true, true, false, 0x55, 0x81));

        assertTrue(ready.canStartInternalClockTransfer());
        assertFalse(armedButNotClockedYet.canStartInternalClockTransfer());
    }

    @Test
    void effectiveMasterCannotConsumeExternalClockWhileDrivingClock() {
        LinkPortState state = new LinkPortState(new SerialLinkState(true, true, true, 0x55, 0x81));

        assertFalse(state.canReceiveExternalClock(true));
        assertTrue(state.canReceiveExternalClock(false));
    }

    @Test
    void externalClockPortCanReceiveClockWhenTransferIsActive() {
        LinkPortState state = new LinkPortState(new SerialLinkState(true, false, false, 0xAA, 0x80));

        assertTrue(state.canReceiveExternalClock(false));
        assertTrue(state.canReceiveExternalClock(true));
    }
}
