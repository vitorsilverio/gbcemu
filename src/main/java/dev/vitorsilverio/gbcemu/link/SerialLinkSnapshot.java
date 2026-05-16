package dev.vitorsilverio.gbcemu.link;

/**
 * Local serial port state reported by {@link dev.vitorsilverio.gbcemu.peripherals.Serial} to the link hub.
 */
public record SerialLinkSnapshot(
        boolean transferActive,
        boolean internalClock,
        boolean masterWaitingResponse,
        boolean master,
        int outgoingByte,
        int sc
) {
    public static SerialLinkSnapshot idle() {
        return new SerialLinkSnapshot(false, false, false, false, 0, 0);
    }
}
