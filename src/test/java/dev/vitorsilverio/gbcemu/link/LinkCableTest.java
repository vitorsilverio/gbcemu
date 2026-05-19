package dev.vitorsilverio.gbcemu.link;

import dev.vitorsilverio.gbcemu.connection.PhysicalConnectionListener;
import dev.vitorsilverio.gbcemu.multiplayer.Multiplayer;
import dev.vitorsilverio.gbcemu.multiplayer.LinkPollMode;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkCableTest {

    @Test
    void constructorRegistersAsTransportListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        new LinkCable(multiplayer);
        verify(multiplayer).setListener(any(PhysicalConnectionListener.class));
    }

    @Test
    void clockTransferSentWhenMasterClockCompletesWithoutWaitingForPeer() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(true); // Host drives dual-master collisions.
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.reportLocalState(new SerialLinkState(true, true, true, 0xAA, 0x81));
        clearInvocations(multiplayer);
        linkCable.onInternalClockComplete(0xAA);

        verify(multiplayer).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.clockTransfer(1, 0xAA))));
    }

    @Test
    void transferResultCompletesMasterListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        int[] received = new int[1];
        linkCable.attachSerial(new ListenerProbe() {
            @Override
            public void onInternalClockResult(int value) {
                received[0] = value;
            }
        });
        linkCable.reportLocalState(new SerialLinkState(true, true, true, 0xAA, 0x81));
        linkCable.onInternalClockComplete(0xAA);

        linkCable.onFrame(LinkCableFrame.transferResult(1, 0x55));

        assertEquals(0x55, received[0]);
    }

    @Test
    void staleTransferResultDoesNotCompleteCurrentClockTransfer() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        int[] received = new int[] { -1 };
        linkCable.attachSerial(new ListenerProbe() {
            @Override
            public void onInternalClockResult(int value) {
                received[0] = value;
            }
        });
        linkCable.reportLocalState(new SerialLinkState(true, true, true, 0xAA, 0x81));
        linkCable.onInternalClockComplete(0xAA);

        linkCable.onFrame(LinkCableFrame.transferResult(2, 0x44));
        linkCable.onFrame(LinkCableFrame.transferResult(1, 0x55));

        assertEquals(0x55, received[0]);
    }

    @Test
    void clockTransferAsksSerialForClockedByteAndSendsResult() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        int[] received = new int[1];
        linkCable.attachSerial(new ListenerProbe() {
            @Override
            public OptionalInt onExternalClockedByte(int value) {
                received[0] = value;
                return OptionalInt.of(0x55);
            }
        });
        linkCable.reportLocalState(new SerialLinkState(true, false, false, 0x55, 0x80));
        clearInvocations(multiplayer);

        linkCable.onFrame(LinkCableFrame.clockTransfer(3, 0xAA));

        assertEquals(0xAA, received[0]);
        verify(multiplayer).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0x55))));
    }

    @Test
    void clockTransferStaysBufferedWhenSerialCannotConsumeItYet() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.attachSerial(new ListenerProbe());
        linkCable.reportLocalState(new SerialLinkState(true, false, false, 0x55, 0x80));

        linkCable.onFrame(LinkCableFrame.clockTransfer(3, 0xAA));
        clearInvocations(multiplayer);

        linkCable.attachSerial(new ListenerProbe() {
            @Override
            public OptionalInt onExternalClockedByte(int value) {
                return OptionalInt.of(0x55);
            }
        });
        linkCable.reportLocalState(new SerialLinkState(true, false, false, 0x55, 0x80));

        verify(multiplayer).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0x55))));
    }

    @Test
    void clockTransferReceivedBeforeSerialAttachIsDeliveredAfterAttach() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.reportLocalState(new SerialLinkState(true, false, false, 0x55, 0x80));
        linkCable.onFrame(LinkCableFrame.clockTransfer(3, 0xAA));
        clearInvocations(multiplayer);

        linkCable.attachSerial(new ListenerProbe() {
            @Override
            public OptionalInt onExternalClockedByte(int value) {
                return OptionalInt.of(0x55);
            }
        });

        verify(multiplayer).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0x55))));
    }

    @Test
    void incomingClockPulseIsRejectedWhileLocalSideIsEffectiveMaster() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.attachSerial(new ListenerProbe() {
            @Override
            public OptionalInt onExternalClockedByte(int value) {
                return OptionalInt.of(0x55);
            }
        });

        linkCable.reportLocalState(new SerialLinkState(true, true, false, 0x55, 0x81));
        clearInvocations(multiplayer);

        linkCable.onFrame(LinkCableFrame.clockTransfer(3, 0xAA));
        linkCable.reportLocalState(new SerialLinkState(true, false, false, 0x55, 0x80));

        assertEquals(1, linkCable.clockPulsesDiscarded());
        verify(multiplayer).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0xFF))));
        verify(multiplayer, never()).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0x55))));
    }

    @Test
    void incomingClockPulseWaitsBrieflyWhenLocalTransferIsNotArmed() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.reportLocalState(SerialLinkState.idle());
        clearInvocations(multiplayer);

        linkCable.onFrame(LinkCableFrame.clockTransfer(3, 0xAA));

        assertEquals(0, linkCable.clockPulsesDiscarded());
        assertEquals(1, linkCable.pendingIncomingClockCount());
        verify(multiplayer, never()).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0xFF))));
    }

    @Test
    void incomingClockPulseExpiresWhenLocalTransferIsNotArmed() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        linkCable.reportLocalState(SerialLinkState.idle());
        clearInvocations(multiplayer);

        linkCable.onFrame(LinkCableFrame.clockTransfer(3, 0xAA));
        for (int tick = 0; tick < 8192; tick++) {
            linkCable.tick();
        }

        assertEquals(1, linkCable.clockPulsesDiscarded());
        assertEquals(0, linkCable.pendingIncomingClockCount());
        verify(multiplayer).send(argThat(packet -> Arrays.equals(packet, LinkCableFrame.transferResult(3, 0xFF))));
    }

    @Test
    void reportLocalStateExposesPeerSnapshotFromStateFrames() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        LinkCable linkCable = new LinkCable(multiplayer);
        SerialLinkState peer = new SerialLinkState(true, false, false, 0x42, 0x80);
        linkCable.onFrame(LinkCableFrame.serialState(peer));
        assertEquals(peer, linkCable.peerState());
    }

    @Test
    void hotTransferStateUsesTransferPolling() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        clearInvocations(multiplayer);

        linkCable.reportLocalState(new SerialLinkState(true, true, false, 0xAA, 0x81));

        verify(multiplayer).setLinkPollMode(LinkPollMode.TRANSFER);
    }

    @Test
    void idleStateUsesConnectedPolling() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);
        clearInvocations(multiplayer);

        linkCable.reportLocalState(SerialLinkState.idle());

        verify(multiplayer).setLinkPollMode(LinkPollMode.CONNECTED);
    }

    @Test
    void dualMasterCollisionIsVisibleWithoutChangingHostWinsPolicy() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(true);
        LinkCable linkCable = new LinkCable(multiplayer);

        linkCable.reportLocalState(new SerialLinkState(true, true, false, 0xAA, 0x81));
        linkCable.onFrame(LinkCableFrame.serialState(new SerialLinkState(true, true, false, 0x55, 0x81)));

        assertTrue(linkCable.hasDualMasterCollision());
        assertTrue(linkCable.isEffectiveMaster());
    }

    @Test
    void nonHostDoesNotDriveInternalClockWhenPeerStateIsStillIdle() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(false);
        LinkCable linkCable = new LinkCable(multiplayer);

        linkCable.reportLocalState(new SerialLinkState(true, true, false, 0x29, 0x81));

        assertTrue(!linkCable.isEffectiveMaster());
        assertTrue(!linkCable.shouldDriveInternalClock());
    }

    @Test
    void nonHostDrivesInternalClockWhenPeerIsWaitingForExternalClock() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true);
        when(multiplayer.isHosting()).thenReturn(false);
        LinkCable linkCable = new LinkCable(multiplayer);

        linkCable.reportLocalState(new SerialLinkState(true, true, false, 0x29, 0x81));
        linkCable.onFrame(LinkCableFrame.serialState(new SerialLinkState(true, false, false, 0x55, 0x80)));

        assertTrue(linkCable.isEffectiveMaster());
        assertTrue(linkCable.shouldDriveInternalClock());
    }

    @Test
    void disconnectNotifiesSerialListener() {
        Multiplayer multiplayer = mock(Multiplayer.class);
        when(multiplayer.isConnected()).thenReturn(true, false, false);
        LinkCable linkCable = new LinkCable(multiplayer);
        ListenerProbe listener = new ListenerProbe();
        linkCable.attachSerial(listener);

        linkCable.tick();

        assertEquals(1, listener.disconnects);
    }

    private static class ListenerProbe implements LinkCableListener {
        private int disconnects;

        @Override
        public OptionalInt onExternalClockedByte(int value) {
            return OptionalInt.empty();
        }

        @Override
        public void onInternalClockResult(int value) {
        }

        @Override
        public void onLinkDisconnected() {
            disconnects++;
        }
    }

}
