package dev.vitorsilverio.gbcemu.link;

/**
 * Typed cable event decoded from the transport frame stream.
 */
interface LinkCableEvent {

    byte[] frame();

    static LinkCableEvent from(LinkCableFrame.ParsedFrame frame) {
        return switch (frame.type()) {
            case LinkCableFrame.TYPE_HELLO -> new Hello();
            case LinkCableFrame.TYPE_SERIAL_STATE -> new PeerState(
                    new LinkPortState(LinkCableFrame.stateFromSerialStatePayload(frame.payload()))
            );
            case LinkCableFrame.TYPE_CLOCK_PULSE -> {
                if (frame.payload().length < 2) {
                    yield new Ignored();
                }
                yield new ClockPulse(frame.payloadByte(0, 0), frame.payloadByte(1, 0xFF));
            }
            case LinkCableFrame.TYPE_CLOCK_RESPONSE -> new ClockResponse(
                    frame.payloadByte(0, -1),
                    frame.payloadByte(1, 0xFF)
            );
            case LinkCableFrame.TYPE_COLLISION -> new Collision(frame.payloadByte(0, 0));
            default -> new Ignored();
        };
    }

    record Hello() implements LinkCableEvent {
        @Override
        public byte[] frame() {
            return LinkCableFrame.hello();
        }
    }

    record PeerState(LinkPortState state) implements LinkCableEvent {
        @Override
        public byte[] frame() {
            return LinkCableFrame.serialState(state.serialState());
        }
    }

    record ClockPulse(int transferId, int value) implements LinkCableEvent {
        @Override
        public byte[] frame() {
            return LinkCableFrame.clockPulse(transferId, value);
        }
    }

    record ClockResponse(int transferId, int value) implements LinkCableEvent {
        @Override
        public byte[] frame() {
            return LinkCableFrame.clockResponse(transferId, value);
        }
    }

    record Collision(int transferId) implements LinkCableEvent {
        @Override
        public byte[] frame() {
            return LinkCableFrame.collision(transferId);
        }
    }

    record Ignored() implements LinkCableEvent {
        @Override
        public byte[] frame() {
            return new byte[0];
        }
    }
}
