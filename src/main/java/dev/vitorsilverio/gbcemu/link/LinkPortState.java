package dev.vitorsilverio.gbcemu.link;

/**
 * Semantic view of a Game Boy serial port state as seen by the link cable.
 */
record LinkPortState(SerialLinkState serialState) {

    LinkPortState {
        serialState = serialState == null ? SerialLinkState.idle() : serialState;
    }

    static LinkPortState idle() {
        return new LinkPortState(SerialLinkState.idle());
    }

    boolean transferActive() {
        return serialState.transferActive();
    }

    boolean internalClockSelected() {
        return serialState.internalClock();
    }

    boolean waitingForInternalClockResult() {
        return serialState.masterWaitingResponse();
    }

    boolean wantsToDriveClock() {
        return transferActive() && internalClockSelected();
    }

    boolean canStartInternalClockTransfer() {
        return wantsToDriveClock() && waitingForInternalClockResult();
    }

    boolean canCompleteInternalClockTransfer(boolean effectiveMaster) {
        return internalClockSelected() && (waitingForInternalClockResult() || !effectiveMaster);
    }

    boolean canReceiveExternalClock(boolean effectiveMaster) {
        return transferActive() && (!internalClockSelected() || !effectiveMaster);
    }

    boolean isTransferWindow() {
        return transferActive() || waitingForInternalClockResult();
    }
}
