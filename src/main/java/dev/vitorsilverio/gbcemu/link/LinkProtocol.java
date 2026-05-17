package dev.vitorsilverio.gbcemu.link;

import java.util.Optional;

/**
 * Framed link protocol between two emulator instances (v1).
 * <p>
 * Frame layout: {@code 0x7C | type | payloadLength | payload...}
 */
public final class LinkProtocol {

    public static final byte MAGIC = 0x7C;
    public static final byte VERSION = 1;

    public static final byte TYPE_HELLO = 0x01;
    public static final byte TYPE_STATE = 0x02;
    public static final byte TYPE_TRANSFER_REQUEST = 0x10;
    public static final byte TYPE_TRANSFER_RESPONSE = 0x11;

    private LinkProtocol() {
    }

    public static byte[] hello() {
        return frame(TYPE_HELLO, new byte[]{VERSION});
    }

    public static byte[] state(SerialLinkSnapshot snapshot) {
        byte flags = 0;
        if (snapshot.transferActive()) {
            flags |= 0x01;
        }
        if (snapshot.internalClock()) {
            flags |= 0x02;
        }
        if (snapshot.masterWaitingResponse()) {
            flags |= 0x04;
        }
        if (snapshot.master()) {
            flags |= 0x08;
        }
        
        byte[] payload = new byte[12];
        payload[0] = flags;
        payload[1] = (byte) snapshot.outgoingByte();
        payload[2] = (byte) snapshot.sc();
        // frameNumber (long = 8 bytes)
        long frame = snapshot.frameNumber();
        for (int i = 0; i < 8; i++) {
            payload[3 + i] = (byte) (frame >> (i * 8));
        }
        return frame(TYPE_STATE, payload);
    }

    public static byte[] transferRequest(int txByte) {
        return frame(TYPE_TRANSFER_REQUEST, new byte[]{(byte) txByte});
    }

    public static byte[] transferResponse(int rxByte) {
        return frame(TYPE_TRANSFER_RESPONSE, new byte[]{(byte) rxByte});
    }

    public static Optional<ParsedFrame> tryParse(byte[] buffer, int length) {
        if (length < 3 || buffer[0] != MAGIC) {
            return Optional.empty();
        }
        int payloadLength = buffer[2] & 0xFF;
        int frameLength = 3 + payloadLength;
        if (length < frameLength) {
            return Optional.empty();
        }
        byte type = buffer[1];
        byte[] payload = new byte[payloadLength];
        if (payloadLength > 0) {
            System.arraycopy(buffer, 3, payload, 0, payloadLength);
        }
        return Optional.of(new ParsedFrame(type, payload, frameLength));
    }

    public static SerialLinkSnapshot snapshotFromStatePayload(byte[] payload) {
        if (payload.length < 3) {
            return SerialLinkSnapshot.idle();
        }
        int flags = payload[0] & 0xFF;
        long frame = 0;
        if (payload.length >= 11) {
            for (int i = 0; i < 8; i++) {
                frame |= ((long) (payload[3 + i] & 0xFF)) << (i * 8);
            }
        }
        return new SerialLinkSnapshot(
                (flags & 0x01) != 0,
                (flags & 0x02) != 0,
                (flags & 0x04) != 0,
                (flags & 0x08) != 0,
                payload[1] & 0xFF,
                payload[2] & 0xFF,
                frame
        );
    }

    private static byte[] frame(byte type, byte[] payload) {
        byte[] frame = new byte[3 + payload.length];
        frame[0] = MAGIC;
        frame[1] = type;
        frame[2] = (byte) payload.length;
        System.arraycopy(payload, 0, frame, 3, payload.length);
        return frame;
    }

    public record ParsedFrame(byte type, byte[] payload, int frameLength) {
    }
}
