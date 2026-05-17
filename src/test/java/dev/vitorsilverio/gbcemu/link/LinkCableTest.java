package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import dev.vitorsilverio.gbcemu.multiplayer.PacketListener;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkCableTest {

    @Test
    void constructorRegistersAsTransportListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        new LinkCable(multiplayer);
        verify(multiplayer).setListener(any(PacketListener.class));
    }

    @Test
    void transferRequestSentWhenMasterClockCompletesWithoutWaitingForPeer() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(true); // Host can always send requests
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.reportLocalState(new SerialLinkSnapshot(true, true, true, true, 0xAA, 0x81, 0));
        clearInvocations(multiplayer);
        linkCable.onInternalClockComplete(0xAA);

        verify(multiplayer).sendControlPacket(LinkProtocol.transferRequest(0xAA));
    }

    @Test
    void transferResponseCompletesMasterListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        LinkCable linkCable = new LinkCable(multiplayer);
        int[] received = new int[1];
        linkCable.attachSerial(value -> received[0] = value);
        linkCable.reportLocalState(new SerialLinkSnapshot(true, true, true, true, 0xAA, 0x81, 0));

        linkCable.onPacket(LinkProtocol.transferResponse(0x55));

        assertEquals(0x55, received[0]);
    }

    @Test
    void reportLocalStateExposesPeerSnapshotFromStateFrames() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        LinkCable linkCable = new LinkCable(multiplayer);
        SerialLinkSnapshot peer = new SerialLinkSnapshot(true, false, false, false, 0x42, 0x80, 0);
        linkCable.onPacket(LinkProtocol.state(peer));
        assertEquals(peer, linkCable.peerState());
    }
}
