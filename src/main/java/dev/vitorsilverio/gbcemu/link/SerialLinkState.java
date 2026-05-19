package dev.vitorsilverio.gbcemu.link;

/**
 * Local serial port state reported by {@link dev.vitorsilverio.gbcemu.peripherals.Serial} to the link hub.
 */
public record SerialLinkState(
        boolean transferActive,
        boolean internalClock,
        boolean masterWaitingResponse,
        int outgoingByte,
        int sc
) {
    public static SerialLinkState idle() {
        return new SerialLinkState(false, false, false, 0, 0);
    }
}
