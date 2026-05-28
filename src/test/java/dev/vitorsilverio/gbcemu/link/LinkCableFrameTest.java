package dev.vitorsilverio.gbcemu.link;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinkCableFrameTest {

    @Test
    void parsesClockTransferFrame() {
        byte[] frame = LinkCableFrame.clockTransfer(7, 0xAA);

        LinkCableFrame.ParsedFrame parsed = LinkCableFrame.tryParse(frame, frame.length).orElseThrow();

        assertEquals(LinkCableFrame.TYPE_CLOCK_TRANSFER, parsed.type());
        assertEquals(7, parsed.payloadByte(0, 0));
        assertEquals(0xAA, parsed.payloadByte(1, 0));
        assertEquals(frame.length, parsed.frameLength());
    }

    @Test
    void waitsForCompleteFrame() {
        byte[] frame = LinkCableFrame.transferResult(3, 0x55);

        assertTrue(LinkCableFrame.tryParse(frame, frame.length - 1).isEmpty());
        assertFalse(LinkCableFrame.hasCompleteFrameHeader(frame, frame.length - 1));
    }

    @Test
    void rejectsInvalidChecksum() {
        byte[] frame = LinkCableFrame.serialState(new SerialLinkState(true, true, false, 0x42, 0x81));
        frame[3] ^= 0x01;

        assertTrue(LinkCableFrame.tryParse(frame, frame.length).isEmpty());
        assertTrue(LinkCableFrame.hasCompleteFrameHeader(frame, frame.length));
    }

    @Test
    void serialStateRoundTripsState() {
        SerialLinkState state = new SerialLinkState(true, true, true, 0xC7, 0x83);
        byte[] frame = LinkCableFrame.serialState(state);

        LinkCableFrame.ParsedFrame parsed = LinkCableFrame.tryParse(frame, frame.length).orElseThrow();

        assertEquals(state, LinkCableFrame.stateFromSerialStatePayload(parsed.payload()));
    }

    @Test
    void parserSkipsCompleteInvalidFrameAndContinuesWithNextFrame() {
        LinkCableFrameParser parser = new LinkCableFrameParser();
        byte[] invalid = LinkCableFrame.hello();
        invalid[3] ^= 0x01;
        byte[] valid = LinkCableFrame.serialState(new SerialLinkState(true, false, false, 0x42, 0x80));
        List<LinkCableFrame.ParsedFrame> frames = new ArrayList<>();

        parser.append(concat(invalid, valid), frames::add);

        assertEquals(1, frames.size());
        assertEquals(LinkCableFrame.TYPE_SERIAL_STATE, frames.getFirst().type());
    }

    @Test
    void parserWaitsForPartialFrameContinuation() {
        LinkCableFrameParser parser = new LinkCableFrameParser();
        byte[] frame = LinkCableFrame.clockTransfer(7, 0xAA);
        List<LinkCableFrame.ParsedFrame> frames = new ArrayList<>();

        parser.append(Arrays.copyOf(frame, frame.length - 1), frames::add);
        assertTrue(frames.isEmpty());

        parser.append(new byte[] { frame[frame.length - 1] }, frames::add);

        assertEquals(1, frames.size());
        assertEquals(LinkCableFrame.TYPE_CLOCK_TRANSFER, frames.getFirst().type());
    }

    private static byte[] concat(byte[] left, byte[] right) {
        byte[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }
}
