package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.multiplayer.LinkPollMode;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import dev.vitorsilverio.gbcemu.multiplayer.PacketListener;

import java.util.ArrayDeque;
import java.util.Optional;

/**
 * Simulates the Game Boy link cable between {@link dev.vitorsilverio.gbcemu.peripherals.Serial}
 * and the network transport ({@link Multiplayer}).
 */
public class LinkCable implements MachineCycle, PacketListener {

    private static final int PARSE_BUFFER_CAPACITY = 512;

    private final Multiplayer multiplayer;
    private LinkCableListener serialListener;
    private SerialLinkSnapshot localState = SerialLinkSnapshot.idle();
    private SerialLinkSnapshot peerState = SerialLinkSnapshot.idle();

    private final byte[] parseBuffer = new byte[PARSE_BUFFER_CAPACITY];
    private int parseLength;
    private final ArrayDeque<byte[]> pendingPackets = new ArrayDeque<>();
    private boolean processingPackets;

    private boolean helloSent;
    private boolean peerStateReceived = false;
    private Integer pendingMasterTx;
    private Integer pendingPartnerByte;
    private boolean awaitingTransferResponse;
    private int frameSyncWaitCycles;

    public LinkCable(Multiplayer multiplayer) {
        this.multiplayer = multiplayer;
        this.multiplayer.setListener(this);
    }

    public void attachSerial(LinkCableListener listener) {
        this.serialListener = listener;
    }

    public void reportLocalState(SerialLinkSnapshot state) {
        SerialLinkSnapshot normalized = state == null ? SerialLinkSnapshot.idle() : state;
        boolean changed = !normalized.equals(localState);
        localState = normalized;

        if (!normalized.transferActive()) {
            pendingMasterTx = null;
            awaitingTransferResponse = false;
        } else {
            flushPendingPartnerByte();
        }

        if (multiplayer.isConnected() && changed) {
            sendControlPacket(LinkProtocol.state(localState));
        }
        if (multiplayer.isConnected()) {
            tryStartTransfer();
        }
        updatePollMode();
    }

    public SerialLinkSnapshot localState() {
        return localState;
    }

    public SerialLinkSnapshot peerState() {
        return peerState;
    }

    public boolean isConnected() {
        return multiplayer.isConnected();
    }

    public boolean isHosting() {
        return multiplayer.isHosting();
    }

    public boolean bothSidesReadyForTransfer() {
        return localState.transferActive() && peerState.transferActive();
    }

    public boolean isEffectiveMaster() {
        if (!multiplayer.isConnected()) {
            return localState.internalClock();
        }
        // Arbitration: If both want to be master, the host wins.
        if (localState.internalClock() && peerState.internalClock()) {
            return isHosting();
        }
        return localState.internalClock();
    }

    public Multiplayer transport() {
        return multiplayer;
    }

    public void onInternalClockComplete(int outgoingByte) {
        if (!multiplayer.isConnected()) {
            return;
        }
        pendingMasterTx = outgoingByte;
        tryStartTransfer();
        updatePollMode();
    }

    public void onExternalClockRespond(int outgoingByte) {
        if (!multiplayer.isConnected()) {
            return;
        }
        sendControlPacket(LinkProtocol.transferResponse(outgoingByte));
    }

    @Override
    public void onPacket(byte[] packet) {
        if (packet == null || packet.length == 0) {
            return;
        }
        pendingPackets.addLast(packet);
        drainPacketQueue();
    }

    @Override
    public void tick() {
        if (frameSyncWaitCycles > 0) {
            frameSyncWaitCycles--;
            return;
        }

        boolean connectedBefore = multiplayer.isConnected();
        multiplayer.tick();

        if (multiplayer.isConnected()) {
            if (!helloSent) {
                sendControlPacket(LinkProtocol.hello());
                sendControlPacket(LinkProtocol.state(localState));
                helloSent = true;
            }

            // LinkSync: Prevent drift between emulators (2 frame tolerance)
            if (peerState.frameNumber() > 0 && localState.frameNumber() > peerState.frameNumber() + 2) {
                frameSyncWaitCycles = 200;
                return;
            }
        } else if (connectedBefore) {
            resetSession();
        }
        updatePollMode();
    }

    private void tryStartTransfer() {
        if (!multiplayer.isConnected()) {
            return;
        }
        if (!localState.transferActive() || !localState.internalClock() || !localState.masterWaitingResponse()) {
            return;
        }
        if (pendingMasterTx == null || awaitingTransferResponse) {
            return;
        }

        // Only the effective master sends requests.
        // The effective slave waits for the request.
        if (isEffectiveMaster()) {
            sendControlPacket(LinkProtocol.transferRequest(pendingMasterTx));
            pendingMasterTx = null;
            awaitingTransferResponse = true;
        }

        if (pendingPartnerByte != null) {
            int partnerByte = pendingPartnerByte;
            pendingPartnerByte = null;
            completeMasterTransfer(partnerByte);
            if (!isEffectiveMaster()) {
                onExternalClockRespond(localState.outgoingByte());
            }
        }
    }

    private void handleFrame(LinkProtocol.ParsedFrame frame) {
        switch (frame.type()) {
            case LinkProtocol.TYPE_HELLO -> { /* session marker */ }
            case LinkProtocol.TYPE_STATE -> {
                peerState = LinkProtocol.snapshotFromStatePayload(frame.payload());
                peerStateReceived = true;
                tryStartTransfer();
                flushPendingPartnerByte();
            }
            case LinkProtocol.TYPE_TRANSFER_REQUEST -> {
                if (frame.payload().length == 0) {
                    return;
                }
                deliverPartnerByte(frame.payload()[0] & 0xFF);
            }
            case LinkProtocol.TYPE_TRANSFER_RESPONSE -> {
                completeMasterTransfer(frame.payload().length == 0 ? 0xFF : frame.payload()[0] & 0xFF);
            }
            default -> { /* ignore unknown frame types */ }
        }
    }

    private void completeMasterTransfer(int partnerByte) {
        if (serialListener == null) {
            return;
        }
        if (!localState.internalClock()) {
            return;
        }
        // In a dual-master conflict, the effective slave might not have reached masterWaitingResponse yet.
        // We allow completion if we are the effective slave OR if we are actually waiting for the response.
        if (!localState.masterWaitingResponse() && isEffectiveMaster()) {
            return;
        }
        awaitingTransferResponse = false;
        pendingPartnerByte = null; // Clear buffered byte as it is now consumed
        serialListener.onPeerByteReceived(partnerByte);
    }

    private void deliverPartnerByte(int partnerTx) {
        if (serialListener == null) {
            return;
        }

        if (localState.internalClock() && localState.masterWaitingResponse()) {
            completeMasterTransfer(partnerTx);
            return;
        }

        // Reactive Slave: If we are the effective slave in a conflict, 
        // we should complete immediately and send response.
        if (localState.internalClock() && !isEffectiveMaster()) {
            completeMasterTransfer(partnerTx);
            onExternalClockRespond(localState.outgoingByte());
            return;
        }

        if (!localState.transferActive() || (localState.internalClock() && isEffectiveMaster())) {
            pendingPartnerByte = partnerTx;
            return;
        }
        pendingPartnerByte = null;
        serialListener.onPeerByteReceived(partnerTx);
    }

    private void flushPendingPartnerByte() {
        if (pendingPartnerByte == null) {
            return;
        }
        if (!localState.transferActive() || (localState.internalClock() && isEffectiveMaster())) {
            return;
        }
        int partnerTx = pendingPartnerByte;
        pendingPartnerByte = null;
        if (serialListener != null) {
            serialListener.onPeerByteReceived(partnerTx);
        }
    }

    private void sendControlPacket(byte[] packet) {
        multiplayer.sendControlPacket(packet);
    }

    private void updatePollMode() {
        if (!multiplayer.isConnected()) {
            multiplayer.setLinkPollMode(LinkPollMode.IDLE);
            return;
        }
        multiplayer.setLinkPollMode(isTransferWindow() ? LinkPollMode.TRANSFER : LinkPollMode.CONNECTED);
    }

    private boolean isTransferWindow() {
        return localState.transferActive()
                || localState.masterWaitingResponse()
                || peerState.transferActive()
                || pendingMasterTx != null
                || pendingPartnerByte != null
                || awaitingTransferResponse;
    }

    private void drainPacketQueue() {
        if (processingPackets) {
            return;
        }
        processingPackets = true;
        try {
            while (!pendingPackets.isEmpty()) {
                byte[] packet = pendingPackets.removeFirst();
                appendToParseBuffer(packet, 0, packet.length);
                drainParseBuffer();
            }
        } finally {
            processingPackets = false;
            updatePollMode();
            if (!pendingPackets.isEmpty()) {
                drainPacketQueue();
            }
        }
    }

    private void appendToParseBuffer(byte[] data, int offset, int length) {
        if (length <= 0) {
            return;
        }
        if (length > parseBuffer.length) {
            parseLength = 0;
            return;
        }
        if (parseLength + length > parseBuffer.length) {
            parseLength = 0;
        }
        System.arraycopy(data, offset, parseBuffer, parseLength, length);
        parseLength += length;
    }

    private void drainParseBuffer() {
        while (parseLength > 0) {
            Optional<LinkProtocol.ParsedFrame> parsed = LinkProtocol.tryParse(parseBuffer, parseLength);
            if (parsed.isPresent()) {
                LinkProtocol.ParsedFrame frame = parsed.get();
                if (frame.frameLength() > parseLength) {
                    parseLength = 0;
                    return;
                }
                handleFrame(frame);
                int remaining = parseLength - frame.frameLength();
                if (remaining > 0) {
                    System.arraycopy(parseBuffer, frame.frameLength(), parseBuffer, 0, remaining);
                }
                parseLength = Math.max(remaining, 0);
                continue;
            }

            if (parseBuffer[0] != LinkProtocol.MAGIC) {
                // Discard invalid byte if not MAGIC
                int remaining = parseLength - 1;
                if (remaining > 0) {
                    System.arraycopy(parseBuffer, 1, parseBuffer, 0, remaining);
                }
                parseLength = Math.max(remaining, 0);
                continue;
            }

            return;
        }
    }

    private void resetSession() {
        helloSent = false;
        peerStateReceived = false;
        peerState = SerialLinkSnapshot.idle();
        pendingMasterTx = null;
        pendingPartnerByte = null;
        awaitingTransferResponse = false;
        frameSyncWaitCycles = 0;
        parseLength = 0;
        pendingPackets.clear();
        processingPackets = false;
        updatePollMode();
    }
}
