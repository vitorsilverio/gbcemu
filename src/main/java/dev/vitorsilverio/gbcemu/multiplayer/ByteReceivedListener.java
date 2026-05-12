package dev.vitorsilverio.gbcemu.multiplayer;

@FunctionalInterface
public interface ByteReceivedListener {
    void onByteReceived(byte value);
}