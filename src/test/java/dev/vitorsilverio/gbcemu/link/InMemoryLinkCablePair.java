package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.memory.Bus;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import dev.vitorsilverio.gbcemu.multiplayer.PacketListener;
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
        BridgingMultiplayer leftTransport = new BridgingMultiplayer(true);
        BridgingMultiplayer rightTransport = new BridgingMultiplayer(false);
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
        private PacketListener listener;
        private final boolean hosting;

        public BridgingMultiplayer(boolean hosting) {
            this.hosting = hosting;
        }

        @Override
        public void setListener(PacketListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isConnected() {
            return peer != null;
        }

        @Override
        public boolean isHosting() {
            return hosting;
        }

        @Override
        public void sendControlPacket(byte[] packet) {
            forward(packet);
        }

        @Override
        public void sendTransferByte(byte data) {
            forward(new byte[]{data});
        }

        private void forward(byte[] packet) {
            if (peer != null && peer.listener != null) {
                peer.listener.onPacket(packet);
            }
        }

        @Override
        public void drainReceiveBounded() {
            // In-memory delivery is synchronous in send*.
        }
    }
}
