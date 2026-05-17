package dev.vitorsilverio.gbcemu.multiplayer;

@FunctionalInterface
public interface PacketListener {
    void onPacket(byte[] packet);
}
