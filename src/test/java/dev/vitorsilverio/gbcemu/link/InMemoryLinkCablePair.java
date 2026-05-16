package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.multiplayer.ByteReceivedListener;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import dev.vitorsilverio.gbcemu.peripherals.Serial;

/**
 * Two {@link Serial} ports connected through a minimal in-memory link for unit tests.
 */
public final class InMemoryLinkCablePair {

    private final LinkCable leftCable;
    private final LinkCable rightCable;
    private final Serial leftSerial;
    private final Serial rightSerial;

    public InMemoryLinkCablePair(Bus leftBus, Bus rightBus) {
        BridgingMultiplayer leftTransport = new BridgingMultiplayer();
        BridgingMultiplayer rightTransport = new BridgingMultiplayer();
        leftTransport.peer = rightTransport;
        rightTransport.peer = leftTransport;

        leftCable = new LinkCable(leftTransport);
        rightCable = new LinkCable(rightTransport);
        leftSerial = new Serial(leftBus, leftCable);
        rightSerial = new Serial(rightBus, rightCable);
    }

    public Serial left() {
        return leftSerial;
    }

    public Serial right() {
        return rightSerial;
    }

    private static final class BridgingMultiplayer extends Multiplayer {
        private BridgingMultiplayer peer;
        private ByteReceivedListener listener;

        @Override
        public void setListener(ByteReceivedListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isConnected() {
            return peer != null;
        }

        @Override
        public void send(byte data) {
            if (peer != null && peer.listener != null) {
                peer.listener.onByteReceived(data);
            }
        }

        @Override
        public void tick() {
            // No socket polling in memory tests.
        }
    }
}
