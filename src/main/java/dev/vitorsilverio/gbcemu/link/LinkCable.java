package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.connection.PhysicalConnection;
import dev.vitorsilverio.gbcemu.connection.PhysicalConnectionListener;
import dev.vitorsilverio.gbcemu.multiplayer.LinkPollMode;
import dev.vitorsilverio.gbcemu.multiplayer.LinkPollingConnection;

import java.util.ArrayDeque;
import java.util.OptionalInt;

/**
 * Simulates the Game Boy link cable between {@link dev.vitorsilverio.gbcemu.peripherals.Serial}
 * and a physical connection transport.
 */
public class LinkCable implements MachineCycle, PhysicalConnectionListener {

    private final PhysicalConnection connection;
    private final LinkCableFrameParser frameParser = new LinkCableFrameParser();
    private final LinkTransferState transferState = new LinkTransferState();
    private LinkCableListener serialListener;
    private LinkPortState localState = LinkPortState.idle();
    private LinkPortState peerState = LinkPortState.idle();

    private final ArrayDeque<byte[]> pendingFrames = new ArrayDeque<>();
    private boolean processingFrames;

    private boolean helloSent;
    private boolean sessionConnected;
    private long clockPulsesSent;
    private long clockPulsesReceived;
    private long clockResponsesSent;
    private long clockResponsesReceived;
    private long clockPulsesDiscarded;
    private int lastClockPulseSent = 0xFF;
    private int lastClockPulseReceived = 0xFF;
    private int lastClockResponseSent = 0xFF;
    private int lastClockResponseReceived = 0xFF;

    public LinkCable(PhysicalConnection connection) {
        this.connection = connection;
        this.connection.setListener(this);
    }

    public void attachSerial(LinkCableListener listener) {
        this.serialListener = listener;
        flushPendingIncomingByte();
    }

    public void reportLocalState(SerialLinkState state) {
        LinkPortState normalized = new LinkPortState(state);
        boolean changed = !normalized.equals(localState);
        localState = normalized;

        if (!normalized.transferActive()) {
            transferState.cancelLocalTransfer();
        } else {
            flushPendingIncomingByte();
        }

        if (connection.isConnected()) {
            sessionConnected = true;
        }
        if (connection.isConnected() && changed) {
            sendEvent(new LinkCableEvent.PeerState(localState));
        }
        if (connection.isConnected()) {
            tryStartTransfer();
        }
        updatePollMode();
    }

    public SerialLinkState localState() {
        return localState.serialState();
    }

    public SerialLinkState peerState() {
        return peerState.serialState();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean isActive() {
        return connection.isActive();
    }

    public boolean isHosting() {
        return connection.isHosting();
    }

    public boolean bothSidesReadyForTransfer() {
        return localState.transferActive() && peerState.transferActive();
    }

    public boolean hasDualMasterCollision() {
        return connection.isConnected()
                && localState.wantsToDriveClock()
                && peerState.wantsToDriveClock();
    }

    public long clockPulsesSent() {
        return clockPulsesSent;
    }

    public long clockPulsesReceived() {
        return clockPulsesReceived;
    }

    public long clockResponsesSent() {
        return clockResponsesSent;
    }

    public long clockResponsesReceived() {
        return clockResponsesReceived;
    }

    public long clockPulsesDiscarded() {
        return clockPulsesDiscarded;
    }

    public int lastClockPulseSent() {
        return lastClockPulseSent;
    }

    public int lastClockPulseReceived() {
        return lastClockPulseReceived;
    }

    public int lastClockResponseSent() {
        return lastClockResponseSent;
    }

    public int lastClockResponseReceived() {
        return lastClockResponseReceived;
    }

    public int pendingIncomingClockCount() {
        return transferState.pendingIncomingClockCount();
    }

    public boolean hasPendingInternalClockByte() {
        return transferState.hasPendingInternalClockByte();
    }

    public boolean hasInFlightTransfer() {
        return transferState.hasInFlightTransfer();
    }

    public boolean isEffectiveMaster() {
        if (!connection.isConnected()) {
            return localState.internalClockSelected();
        }
        if (!localState.internalClockSelected()) {
            return false;
        }
        if (peerState.transferActive() && !peerState.wantsToDriveClock()) {
            return true;
        }
        return isHosting();
    }

    public boolean shouldDriveInternalClock() {
        return !connection.isConnected() || isEffectiveMaster();
    }

    public boolean shouldCompleteInternalClockTransfer(boolean waitingForTransferResult) {
        return waitingForTransferResult || !connection.isConnected() || !isEffectiveMaster();
    }

    public int visibleSerialControl(int serialControl) {
        if (connection.isConnected()
                && (serialControl & 0x01) != 0
                && !isEffectiveMaster()) {
            return serialControl & ~0x01;
        }
        return serialControl;
    }

    public int normalizeSerialControlWrite(int serialControl) {
        return serialControl;
    }

    public PhysicalConnection transport() {
        return connection;
    }

    public void disconnect() {
        connection.disconnect();
        resetSession();
    }

    public void onInternalClockComplete(int outgoingByte) {
        if (!connection.isConnected()) {
            return;
        }
        sessionConnected = true;
        transferState.queueInternalClockByte(outgoingByte);
        tryStartTransfer();
        updatePollMode();
    }

    @Override
    public void onFrame(byte[] frame) {
        if (frame == null || frame.length == 0) {
            return;
        }
        pendingFrames.addLast(frame);
        drainFrameQueue();
    }

    @Override
    public void tick() {
        boolean connectedBefore = sessionConnected || connection.isConnected();
        connection.tick();

        if (connection.isConnected()) {
            sessionConnected = true;
            if (!helloSent) {
                sendEvent(new LinkCableEvent.Hello());
                sendEvent(new LinkCableEvent.PeerState(localState));
                helloSent = true;
            }
        } else if (connectedBefore) {
            if (serialListener != null) {
                serialListener.onLinkDisconnected();
            }
            resetSession();
        }
        transferState.agePendingIncomingClocks(this::expireIncomingClockPulse);
        flushPendingIncomingByte();
        updatePollMode();
    }

    private void tryStartTransfer() {
        if (!connection.isConnected()) {
            return;
        }
        if (!canStartInternalClockTransfer()) {
            return;
        }
        if (!transferState.canStartInternalClockTransfer()) {
            return;
        }

        // Only the effective master drives the virtual clock transfer.
        // The effective slave waits for that clocked byte.
        if (isEffectiveMaster()) {
            LinkTransferState.OutgoingClockPulse outgoingPulse = transferState.startInternalClockTransfer();
            sendEvent(new LinkCableEvent.ClockPulse(outgoingPulse.id(), outgoingPulse.outgoingByte()));
        }

        if (transferState.hasPendingIncomingClock() && !isEffectiveMaster()) {
            deliverClockPulse(transferState.takePendingIncomingClock());
        }
    }

    private void handleFrame(LinkCableFrame.ParsedFrame frame) {
        handleEvent(LinkCableEvent.from(frame));
    }

    private void handleEvent(LinkCableEvent event) {
        if (event instanceof LinkCableEvent.Hello) {
            return;
        }
        if (event instanceof LinkCableEvent.PeerState peerStateEvent) {
            peerState = peerStateEvent.state();
            tryStartTransfer();
            flushPendingIncomingByte();
            return;
        }
        if (event instanceof LinkCableEvent.ClockPulse clockPulse) {
            clockPulsesReceived++;
            lastClockPulseReceived = clockPulse.value() & 0xFF;
            deliverClockPulse(new LinkTransferState.IncomingClockPulse(
                    clockPulse.transferId(),
                    clockPulse.value(),
                    0
            ));
            return;
        }
        if (event instanceof LinkCableEvent.ClockResponse clockResponse) {
            clockResponsesReceived++;
            lastClockResponseReceived = clockResponse.value() & 0xFF;
            if (transferState.matchesInFlightTransfer(clockResponse.transferId())) {
                completeInternalClockTransferResponse(clockResponse.value());
            }
            return;
        }
        if (event instanceof LinkCableEvent.Collision) {
            transferState.cancelLocalTransfer();
        }
    }

    private void completeInternalClockTransferResponse(int incomingByte) {
        if (serialListener == null) {
            return;
        }
        if (!canCompleteInternalClockTransfer()) {
            return;
        }
        transferState.completeInFlightTransfer();
        serialListener.onInternalClockResult(incomingByte);
    }

    private void deliverClockPulse(LinkTransferState.IncomingClockPulse clockPulse) {
        if (shouldRejectIncomingClockPulse()) {
            clockPulsesDiscarded++;
            sendClockResponse(clockPulse.transferId(), 0xFF);
            return;
        }

        if (serialListener == null) {
            bufferClockPulse(clockPulse);
            return;
        }

        if (!canReceiveExternalClock()) {
            bufferClockPulse(clockPulse);
            return;
        }

        OptionalInt result = serialListener.onExternalClockedByte(clockPulse.value());
        if (result.isPresent()) {
            sendClockResponse(clockPulse.transferId(), result.getAsInt());
            return;
        }

        bufferClockPulse(clockPulse);
    }

    private boolean shouldRejectIncomingClockPulse() {
        return localState.wantsToDriveClock() && isEffectiveMaster();
    }

    private void expireIncomingClockPulse(LinkTransferState.IncomingClockPulse clockPulse) {
        clockPulsesDiscarded++;
        sendClockResponse(clockPulse.transferId(), 0xFF);
    }

    private void flushPendingIncomingByte() {
        if (!transferState.hasPendingIncomingClock()) {
            return;
        }
        if (!canReceiveExternalClock()) {
            return;
        }
        if (serialListener != null) {
            deliverClockPulse(transferState.takePendingIncomingClock());
        }
    }

    private void bufferClockPulse(LinkTransferState.IncomingClockPulse clockPulse) {
        transferState.bufferIncomingClock(clockPulse.transferId(), clockPulse.value());
    }

    private void sendFrame(byte[] frame) {
        connection.send(frame);
    }

    private void sendEvent(LinkCableEvent event) {
        if (event instanceof LinkCableEvent.ClockPulse) {
            clockPulsesSent++;
            lastClockPulseSent = ((LinkCableEvent.ClockPulse) event).value() & 0xFF;
        } else if (event instanceof LinkCableEvent.ClockResponse) {
            clockResponsesSent++;
            lastClockResponseSent = ((LinkCableEvent.ClockResponse) event).value() & 0xFF;
        }
        sendFrame(event.frame());
    }

    private void sendClockResponse(int transferId, int outgoingByte) {
        if (!connection.isConnected()) {
            return;
        }
        sendEvent(new LinkCableEvent.ClockResponse(transferId, outgoingByte));
    }

    private boolean canStartInternalClockTransfer() {
        return localState.canStartInternalClockTransfer();
    }

    private boolean canCompleteInternalClockTransfer() {
        return localState.canCompleteInternalClockTransfer(isEffectiveMaster());
    }

    private boolean canReceiveExternalClock() {
        return localState.canReceiveExternalClock(isEffectiveMaster());
    }

    private void updatePollMode() {
        if (connection instanceof LinkPollingConnection pollingConnection) {
            if (!connection.isConnected()) {
                pollingConnection.setLinkPollMode(connection.isActive() ? LinkPollMode.CONNECTED : LinkPollMode.IDLE);
                return;
            }
            pollingConnection.setLinkPollMode(isTransferWindow() ? LinkPollMode.TRANSFER : LinkPollMode.CONNECTED);
        }
    }

    private boolean isTransferWindow() {
        return localState.isTransferWindow()
                || peerState.transferActive()
                || transferState.isTransferWindow();
    }

    private void drainFrameQueue() {
        if (processingFrames) {
            return;
        }
        processingFrames = true;
        try {
            while (!pendingFrames.isEmpty()) {
                byte[] frame = pendingFrames.removeFirst();
                frameParser.append(frame, this::handleFrame);
            }
        } finally {
            processingFrames = false;
            updatePollMode();
            if (!pendingFrames.isEmpty()) {
                drainFrameQueue();
            }
        }
    }

    private void resetSession() {
        helloSent = false;
        sessionConnected = false;
        peerState = LinkPortState.idle();
        transferState.reset();
        frameParser.reset();
        pendingFrames.clear();
        processingFrames = false;
        updatePollMode();
    }
}
