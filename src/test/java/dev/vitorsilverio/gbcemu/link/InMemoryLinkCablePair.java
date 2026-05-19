package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.connection.InMemoryPhysicalConnectionPair;
import dev.vitorsilverio.gbcemu.memory.Bus;
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
        InMemoryPhysicalConnectionPair connectionPair = new InMemoryPhysicalConnectionPair();
        leftCable = new LinkCable(connectionPair.left());
        rightCable = new LinkCable(connectionPair.right());
        leftSerial = new Serial(leftBus, leftCable);
        rightSerial = new Serial(rightBus, rightCable);
    }

    public Serial left() {
        return leftSerial;
    }

    public Serial right() {
        return rightSerial;
    }

    public void disconnectLeft() {
        leftCable.transport().disconnect();
    }

    public void tickLeftCable() {
        leftCable.tick();
    }

    public void tickRightCable() {
        rightCable.tick();
    }
}
