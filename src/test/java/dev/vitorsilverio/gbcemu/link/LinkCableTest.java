package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.multiplayer.ByteReceivedListener;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkCableTest {

    @Test
    void constructorRegistersAsTransportListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        new LinkCable(multiplayer);
        verify(multiplayer).setListener(any(ByteReceivedListener.class));
    }

    @Test
    void onInternalClockCompleteSendsWhenConnected() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);

        linkCable.onInternalClockComplete(0xC7);

        verify(multiplayer).send((byte) 0xC7);
    }

    @Test
    void onInternalClockCompleteDoesNotSendWhenDisconnected() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(false);
        LinkCable linkCable = new LinkCable(multiplayer);

        linkCable.onInternalClockComplete(0xC7);

        verify(multiplayer, never()).send(anyByte());
    }

    @Test
    void onByteReceivedForwardsToSerialListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        LinkCable linkCable = new LinkCable(multiplayer);
        int[] received = new int[1];
        linkCable.attachSerial(value -> received[0] = value);

        ArgumentCaptor<ByteReceivedListener> captor = ArgumentCaptor.forClass(ByteReceivedListener.class);
        verify(multiplayer).setListener(captor.capture());
        captor.getValue().onByteReceived((byte) 0x55);

        assertEquals(0x55, received[0]);
    }

    @Test
    void reportLocalStateIsExposedInDebugSnapshot() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        LinkCable linkCable = new LinkCable(multiplayer);
        SerialLinkSnapshot snapshot = new SerialLinkSnapshot(true, true, false, true, 0x42, 0x81);
        linkCable.reportLocalState(snapshot);
        assertEquals(snapshot, linkCable.localState());
    }
}
