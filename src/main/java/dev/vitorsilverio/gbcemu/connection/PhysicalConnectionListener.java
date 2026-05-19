package dev.vitorsilverio.gbcemu.connection;

@FunctionalInterface
public interface PhysicalConnectionListener {
    void onFrame(byte[] frame);
}
