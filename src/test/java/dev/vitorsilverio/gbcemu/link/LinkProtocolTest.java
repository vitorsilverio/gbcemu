package dev.vitorsilverio.gbcemu.link;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkProtocolTest {

    @Test
    void stateFrameRoundTrip() {
        SerialLinkSnapshot snapshot = new SerialLinkSnapshot(true, false, true, true, 0xAA, 0x81, 123456789L);
        byte[] frame = LinkProtocol.state(snapshot);

        LinkProtocol.ParsedFrame parsed = LinkProtocol.tryParse(frame, frame.length).orElseThrow();
        assertEquals(LinkProtocol.TYPE_STATE, parsed.type());
        assertEquals(snapshot, LinkProtocol.snapshotFromStatePayload(parsed.payload()));
    }

    @Test
    void transferRequestFrameCarriesTxByte() {
        byte[] frame = LinkProtocol.transferRequest(0xC7);
        LinkProtocol.ParsedFrame parsed = LinkProtocol.tryParse(frame, frame.length).orElseThrow();
        assertEquals(LinkProtocol.TYPE_TRANSFER_REQUEST, parsed.type());
        assertEquals(0xC7, parsed.payload()[0] & 0xFF);
    }

    @Test
    void incompleteFrameWaitsForMoreBytes() {
        byte[] frame = LinkProtocol.transferResponse(0x55);
        assertTrue(LinkProtocol.tryParse(frame, frame.length - 1).isEmpty());
        assertArrayEquals(
                new byte[]{(byte) 0x55},
                LinkProtocol.tryParse(frame, frame.length).orElseThrow().payload()
        );
    }
}
