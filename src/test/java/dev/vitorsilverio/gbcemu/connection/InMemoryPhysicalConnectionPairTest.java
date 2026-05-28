package dev.vitorsilverio.gbcemu.connection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPhysicalConnectionPairTest {

    @Test
    void sendsFramesBetweenBothEndpoints() {
        InMemoryPhysicalConnectionPair pair = new InMemoryPhysicalConnectionPair();
        List<byte[]> leftFrames = new ArrayList<>();
        List<byte[]> rightFrames = new ArrayList<>();
        pair.left().setListener(leftFrames::add);
        pair.right().setListener(rightFrames::add);

        pair.left().send(new byte[]{0x01, 0x02});
        pair.right().send(new byte[]{0x03});

        assertArrayEquals(new byte[]{0x01, 0x02}, rightFrames.get(0));
        assertArrayEquals(new byte[]{0x03}, leftFrames.get(0));
    }

    @Test
    void disconnectsBothEndpoints() {
        InMemoryPhysicalConnectionPair pair = new InMemoryPhysicalConnectionPair();

        assertTrue(pair.left().isConnected());
        assertTrue(pair.right().isConnected());

        pair.left().disconnect();

        assertFalse(pair.left().isConnected());
        assertFalse(pair.right().isConnected());
    }
}
