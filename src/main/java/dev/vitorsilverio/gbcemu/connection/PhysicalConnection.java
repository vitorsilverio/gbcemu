package dev.vitorsilverio.gbcemu.connection;

import dev.vitorsilverio.gbcemu.core.MachineCycle;

public interface PhysicalConnection extends MachineCycle, AutoCloseable {

    void setListener(PhysicalConnectionListener listener);

    void send(byte[] frame);

    void hostLocal(String path);

    void joinLocal(String path);

    void hostTcp(String host, int port);

    void joinTcp(String host, int port);

    void disconnect();

    boolean isConnected();

    boolean isHosting();

    default boolean isActive() {
        return isConnected() || isHosting();
    }

    String status();

    String lastLocalPath();

    String lastTcpHost();

    int lastTcpPort();

    boolean lastTcpMode();

    boolean lastHostMode();
}
