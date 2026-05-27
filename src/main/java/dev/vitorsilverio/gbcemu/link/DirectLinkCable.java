package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.connection.DisconnectedPhysicalConnection;

import java.util.OptionalInt;

/**
 * In-process link cable used by local two-player sessions.
 *
 * <p>This bypasses the old frame transport completely: both endpoints share state in memory and
 * exchange the byte at the moment the effective clock source finishes a serial transfer.</p>
 */
public final class DirectLinkCable extends LinkCable {

    private static final int PENDING_CLOCK_TIMEOUT_CYCLES = 8192;

    private DirectLinkCable peer;
    private final boolean primary;
    private final Object lock;
    private LinkCableListener serialListener;
    private LinkPortState localState = LinkPortState.idle();
    private PendingClockPulse pendingClockPulse;
    private long sequenceCounter;
    private long localTransferSequence;
    private long clockPulsesSent;
    private long clockPulsesReceived;
    private long clockResponsesSent;
    private long clockResponsesReceived;
    private long clockPulsesDiscarded;
    private int lastClockPulseSent = 0xFF;
    private int lastClockPulseReceived = 0xFF;
    private int lastClockResponseSent = 0xFF;
    private int lastClockResponseReceived = 0xFF;

    private DirectLinkCable(boolean primary, Object lock) {
        super(new DisconnectedPhysicalConnection());
        this.primary = primary;
        this.lock = lock;
    }

    public static Pair createPair() {
        Object lock = new Object();
        DirectLinkCable left = new DirectLinkCable(true, lock);
        DirectLinkCable right = new DirectLinkCable(false, lock);
        left.connectPeerLocked(right);
        return new Pair(left, right);
    }

    public static DirectLinkCable createStandalone() {
        return new DirectLinkCable(true, new Object());
    }

    public DirectLinkCable createPeer() {
        synchronized (lock) {
            DirectLinkCable linkedPeer = new DirectLinkCable(false, lock);
            connectPeerLocked(linkedPeer);
            return linkedPeer;
        }
    }

    private void connectPeerLocked(DirectLinkCable linkedPeer) {
        this.peer = linkedPeer;
        linkedPeer.peer = this;
        pendingClockPulse = null;
        linkedPeer.pendingClockPulse = null;
    }

    @Override
    public void attachSerial(LinkCableListener listener) {
        synchronized (lock) {
            this.serialListener = listener;
        }
    }

    @Override
    public void reportLocalState(SerialLinkState state) {
        synchronized (lock) {
            LinkPortState previousState = localState;
            localState = new LinkPortState(state);
            if (!previousState.transferActive() && localState.transferActive()) {
                DirectLinkCable linkedPeer = peer;
                long nextSequence = linkedPeer == null
                        ? sequenceCounter + 1
                        : Math.max(sequenceCounter, linkedPeer.sequenceCounter) + 1;
                sequenceCounter = nextSequence;
                if (linkedPeer != null) {
                    linkedPeer.sequenceCounter = nextSequence;
                }
                localTransferSequence = nextSequence;
            } else if (!localState.transferActive()) {
                localTransferSequence = 0;
            }
            flushPendingClockPulseLocked();
        }
    }

    @Override
    public SerialLinkState localState() {
        synchronized (lock) {
            return localState.serialState();
        }
    }

    @Override
    public SerialLinkState peerState() {
        synchronized (lock) {
            DirectLinkCable linkedPeer = peer;
            return linkedPeer == null ? SerialLinkState.idle() : linkedPeer.localState.serialState();
        }
    }

    @Override
    public boolean isConnected() {
        return peer != null;
    }

    @Override
    public boolean isActive() {
        return isConnected();
    }

    @Override
    public boolean isHosting() {
        return primary;
    }

    @Override
    public boolean bothSidesReadyForTransfer() {
        synchronized (lock) {
            DirectLinkCable linkedPeer = peer;
            return linkedPeer != null
                    && localState.transferActive()
                    && linkedPeer.localState.transferActive();
        }
    }

    @Override
    public boolean hasDualMasterCollision() {
        synchronized (lock) {
            DirectLinkCable linkedPeer = peer;
            return linkedPeer != null
                    && localState.wantsToDriveClock()
                    && linkedPeer.localState.wantsToDriveClock();
        }
    }

    @Override
    public long clockPulsesSent() {
        synchronized (lock) {
            return clockPulsesSent;
        }
    }

    @Override
    public long clockPulsesReceived() {
        synchronized (lock) {
            return clockPulsesReceived;
        }
    }

    @Override
    public long clockResponsesSent() {
        synchronized (lock) {
            return clockResponsesSent;
        }
    }

    @Override
    public long clockResponsesReceived() {
        synchronized (lock) {
            return clockResponsesReceived;
        }
    }

    @Override
    public long clockPulsesDiscarded() {
        synchronized (lock) {
            return clockPulsesDiscarded;
        }
    }

    @Override
    public int lastClockPulseSent() {
        synchronized (lock) {
            return lastClockPulseSent;
        }
    }

    @Override
    public int lastClockPulseReceived() {
        synchronized (lock) {
            return lastClockPulseReceived;
        }
    }

    @Override
    public int lastClockResponseSent() {
        synchronized (lock) {
            return lastClockResponseSent;
        }
    }

    @Override
    public int lastClockResponseReceived() {
        synchronized (lock) {
            return lastClockResponseReceived;
        }
    }

    @Override
    public int pendingIncomingClockCount() {
        synchronized (lock) {
            return pendingClockPulse == null ? 0 : 1;
        }
    }

    @Override
    public boolean isEffectiveMaster() {
        synchronized (lock) {
            return isEffectiveMasterLocked();
        }
    }

    @Override
    public boolean shouldDriveInternalClock() {
        synchronized (lock) {
            return isEffectiveMasterLocked();
        }
    }

    @Override
    public boolean shouldCompleteInternalClockTransfer(boolean waitingForTransferResult) {
        synchronized (lock) {
            return waitingForTransferResult || !isEffectiveMasterLocked();
        }
    }

    @Override
    public int visibleSerialControl(int serialControl) {
        synchronized (lock) {
            if ((serialControl & 0x01) != 0 && !isEffectiveMasterLocked()) {
                return serialControl & ~0x01;
            }
            return serialControl;
        }
    }

    @Override
    public int normalizeSerialControlWrite(int serialControl) {
        return serialControl;
    }

    @Override
    public void disconnect() {
        synchronized (lock) {
            DirectLinkCable linkedPeer = peer;
            peer = null;
            pendingClockPulse = null;
            if (serialListener != null) {
                serialListener.onLinkDisconnected();
            }
            if (linkedPeer != null && linkedPeer.peer == this) {
                linkedPeer.peer = null;
                linkedPeer.pendingClockPulse = null;
                if (linkedPeer.serialListener != null) {
                    linkedPeer.serialListener.onLinkDisconnected();
                }
            }
        }
    }

    @Override
    public void onInternalClockComplete(int outgoingByte) {
        synchronized (lock) {
            DirectLinkCable linkedPeer = peer;
            if (linkedPeer == null || serialListener == null) {
                completeInternalClockLocked(0xFF);
                return;
            }

            clockPulsesSent++;
            lastClockPulseSent = outgoingByte & 0xFF;
            OptionalInt peerResponse = linkedPeer.receiveClockedByteLocked(outgoingByte);
            peerResponse.ifPresent(this::completeInternalClockLocked);
        }
    }

    @Override
    public void tick() {
        synchronized (lock) {
            if (pendingClockPulse == null) {
                return;
            }
            if (flushPendingClockPulseLocked()) {
                return;
            }
            pendingClockPulse.age++;
            if (pendingClockPulse.age >= PENDING_CLOCK_TIMEOUT_CYCLES) {
                PendingClockPulse expired = pendingClockPulse;
                pendingClockPulse = null;
                clockPulsesDiscarded++;
                expired.sender.completeInternalClockLocked(0xFF);
            }
        }
    }

    private OptionalInt receiveClockedByteLocked(int value) {
        clockPulsesReceived++;
        lastClockPulseReceived = value & 0xFF;
        if (serialListener == null) {
            return OptionalInt.of(0xFF);
        }
        if (!canReceiveExternalClock()) {
            if (canHoldPendingClockPulse()) {
                pendingClockPulse = new PendingClockPulse(peer, value & 0xFF);
                return OptionalInt.empty();
            }
            return OptionalInt.of(0xFF);
        }
        OptionalInt response = serialListener.onExternalClockedByte(value & 0xFF);
        response.ifPresent(this::recordClockResponseSentLocked);
        return response;
    }

    private void completeInternalClockLocked(int value) {
        clockResponsesReceived++;
        lastClockResponseReceived = value & 0xFF;
        if (serialListener != null && localState.canCompleteInternalClockTransfer(isEffectiveMasterLocked())) {
            serialListener.onInternalClockResult(value & 0xFF);
        }
    }

    private void recordClockResponseSentLocked(int value) {
        clockResponsesSent++;
        lastClockResponseSent = value & 0xFF;
    }

    private boolean canReceiveExternalClock() {
        return localState.canReceiveExternalClock(isEffectiveMasterLocked());
    }

    private boolean canHoldPendingClockPulse() {
        return pendingClockPulse == null
                && localState.internalClockSelected()
                && !localState.waitingForInternalClockResult();
    }

    private boolean flushPendingClockPulseLocked() {
        if (pendingClockPulse == null || serialListener == null || !canReceiveExternalClock()) {
            return false;
        }
        PendingClockPulse pending = pendingClockPulse;
        pendingClockPulse = null;
        OptionalInt response = serialListener.onExternalClockedByte(pending.value);
        if (response.isPresent()) {
            recordClockResponseSentLocked(response.getAsInt());
            pending.sender.completeInternalClockLocked(response.getAsInt());
            return true;
        }
        pendingClockPulse = pending;
        return false;
    }

    private boolean isEffectiveMasterLocked() {
        if (!localState.internalClockSelected()) {
            return false;
        }
        DirectLinkCable linkedPeer = peer;
        if (linkedPeer == null) {
            return true;
        }

        LinkPortState remote = linkedPeer.localState;
        if (remote.transferActive() && !remote.wantsToDriveClock()) {
            return true;
        }
        if (remote.wantsToDriveClock()) {
            return localTransferSequence <= linkedPeer.localTransferSequence
                    || linkedPeer.localTransferSequence == 0;
        }
        return primary;
    }

    public record Pair(DirectLinkCable player1, DirectLinkCable player2) {
    }

    private static final class PendingClockPulse {
        private final DirectLinkCable sender;
        private final int value;
        private int age;

        private PendingClockPulse(DirectLinkCable sender, int value) {
            this.sender = sender;
            this.value = value & 0xFF;
        }
    }
}
