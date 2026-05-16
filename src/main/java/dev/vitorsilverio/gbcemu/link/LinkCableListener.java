package dev.vitorsilverio.gbcemu.link;

@FunctionalInterface
public interface LinkCableListener {
    void onPeerByteReceived(int value);
}
