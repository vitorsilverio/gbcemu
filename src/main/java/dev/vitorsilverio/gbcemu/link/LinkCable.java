package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.multiplayer.ByteReceivedListener;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;

/**
 * Simulates the Game Boy link cable between {@link dev.vitorsilverio.gbcemu.peripherals.Serial}
 * and the network transport ({@link Multiplayer}).
 * <p>
 * Phase 1 forwards bytes while tracking local serial state for later pairing and protocol work.
 */
public class LinkCable implements MachineCycle, ByteReceivedListener {

    private final Multiplayer multiplayer;
    private LinkCableListener serialListener;
    private SerialLinkSnapshot localState = SerialLinkSnapshot.idle();

    public LinkCable(Multiplayer multiplayer) {
        this.multiplayer = multiplayer;
        this.multiplayer.setListener(this);
    }

    public void attachSerial(LinkCableListener listener) {
        this.serialListener = listener;
    }

    public void reportLocalState(SerialLinkSnapshot state) {
        localState = state == null ? SerialLinkSnapshot.idle() : state;
    }

    public SerialLinkSnapshot localState() {
        return localState;
    }

    public boolean isConnected() {
        return multiplayer.isConnected();
    }

    public boolean isHosting() {
        return multiplayer.isHosting();
    }

    public Multiplayer transport() {
        return multiplayer;
    }

    /**
     * Internal-clock side finished its serial countdown and shifts out {@code outgoingByte}.
     */
    public void onInternalClockComplete(int outgoingByte) {
        if (!multiplayer.isConnected()) {
            return;
        }
        multiplayer.send((byte) outgoingByte);
    }

    /**
     * External-clock side responds after receiving the partner byte.
     */
    public void onExternalClockRespond(int outgoingByte) {
        if (!multiplayer.isConnected()) {
            return;
        }
        multiplayer.send((byte) outgoingByte);
    }

    @Override
    public void onByteReceived(byte value) {
        if (serialListener == null) {
            return;
        }
        serialListener.onPeerByteReceived(value & 0xFF);
    }

    @Override
    public void tick() {
        multiplayer.tick();
    }
}
