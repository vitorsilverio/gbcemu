package dev.vitorsilverio.gbcemu.link;

import java.util.Optional;

public final class LinkCableFrame {

    public static final byte MAGIC = 0x4C;
    public static final byte TYPE_HELLO = 0x01;
    public static final byte TYPE_SERIAL_STATE = 0x02;
    public static final byte TYPE_CLOCK_PULSE = 0x10;
    public static final byte TYPE_CLOCK_RESPONSE = 0x11;
    public static final byte TYPE_CLOCK_TRANSFER = TYPE_CLOCK_PULSE;
    public static final byte TYPE_TRANSFER_RESULT = TYPE_CLOCK_RESPONSE;
    public static final byte TYPE_COLLISION = 0x20;

    private static final int HEADER_SIZE = 4;

    private LinkCableFrame() {
    }

    public static byte[] hello() {
        return frame(TYPE_HELLO);
    }

    public static byte[] serialState(SerialLinkState state) {
        SerialLinkState normalized = state == null ? SerialLinkState.idle() : state;
        return frame(
                TYPE_SERIAL_STATE,
                normalized.sc() & 0xFF,
                normalized.outgoingByte() & 0xFF,
                normalized.transferActive() ? 1 : 0,
                normalized.internalClock() ? 1 : 0,
                normalized.masterWaitingResponse() ? 1 : 0
        );
    }

    public static byte[] clockTransfer(int transferId, int txByte) {
        return clockPulse(transferId, txByte);
    }

    public static byte[] transferResult(int transferId, int rxByte) {
        return clockResponse(transferId, rxByte);
    }

    public static byte[] clockPulse(int transferId, int value) {
        return frame(TYPE_CLOCK_PULSE, transferId & 0xFF, value & 0xFF);
    }

    public static byte[] clockResponse(int transferId, int value) {
        return frame(TYPE_CLOCK_RESPONSE, transferId & 0xFF, value & 0xFF);
    }

    public static byte[] collision(int transferId) {
        return frame(TYPE_COLLISION, transferId & 0xFF);
    }

    public static Optional<ParsedFrame> tryParse(byte[] data, int length) {
        if (data == null || length < HEADER_SIZE || data[0] != MAGIC) {
            return Optional.empty();
        }
        int payloadLength = data[2] & 0xFF;
        int frameLength = HEADER_SIZE + payloadLength;
        if (length < frameLength) {
            return Optional.empty();
        }
        int checksum = data[3] & 0xFF;
        for (int i = 0; i < payloadLength; i++) {
            checksum = (checksum - (data[HEADER_SIZE + i] & 0xFF)) & 0xFF;
        }
        if (checksum != ((data[1] & 0xFF) ^ payloadLength)) {
            return Optional.empty();
        }
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, HEADER_SIZE, payload, 0, payloadLength);
        return Optional.of(new ParsedFrame(data[1], payload, frameLength));
    }

    public static boolean hasCompleteFrameHeader(byte[] data, int length) {
        if (data == null || length < HEADER_SIZE || data[0] != MAGIC) {
            return false;
        }
        int payloadLength = data[2] & 0xFF;
        return length >= HEADER_SIZE + payloadLength;
    }

    public static SerialLinkState stateFromSerialStatePayload(byte[] payload) {
        if (payload == null || payload.length < 4) {
            return SerialLinkState.idle();
        }
        return new SerialLinkState(
                (payload[2] & 0xFF) != 0,
                (payload[3] & 0xFF) != 0,
                payload.length >= 5 && (payload[4] & 0xFF) != 0,
                payload[1] & 0xFF,
                payload[0] & 0xFF
        );
    }

    private static byte[] frame(byte type, int... payload) {
        byte[] frame = new byte[HEADER_SIZE + payload.length];
        frame[0] = MAGIC;
        frame[1] = type;
        frame[2] = (byte) payload.length;
        int checksum = (type & 0xFF) ^ payload.length;
        for (int i = 0; i < payload.length; i++) {
            int value = payload[i] & 0xFF;
            frame[HEADER_SIZE + i] = (byte) value;
            checksum = (checksum + value) & 0xFF;
        }
        frame[3] = (byte) checksum;
        return frame;
    }

    public record ParsedFrame(byte type, byte[] payload, int frameLength) {
        public int payloadByte(int index, int defaultValue) {
            if (index < 0 || index >= payload.length) {
                return defaultValue & 0xFF;
            }
            return payload[index] & 0xFF;
        }
    }
}
