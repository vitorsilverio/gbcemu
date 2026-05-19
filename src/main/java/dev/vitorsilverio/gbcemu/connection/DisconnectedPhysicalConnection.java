package dev.vitorsilverio.gbcemu.connection;

/**
 * Default disconnected link transport used while local link cable is being rebuilt.
 */
public class DisconnectedPhysicalConnection implements PhysicalConnection {

    @Override
    public void setListener(PhysicalConnectionListener listener) {
    }

    @Override
    public void send(byte[] frame) {
    }

    @Override
    public void hostLocal(String path) {
        throw new UnsupportedOperationException("Socket link cable was removed from the UI.");
    }

    @Override
    public void joinLocal(String path) {
        throw new UnsupportedOperationException("Socket link cable was removed from the UI.");
    }

    @Override
    public void hostTcp(String host, int port) {
        throw new UnsupportedOperationException("TCP link cable is reserved for future netplay.");
    }

    @Override
    public void joinTcp(String host, int port) {
        throw new UnsupportedOperationException("TCP link cable is reserved for future netplay.");
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isHosting() {
        return false;
    }

    @Override
    public String status() {
        return "Disconnected";
    }

    @Override
    public String lastLocalPath() {
        return "";
    }

    @Override
    public String lastTcpHost() {
        return "";
    }

    @Override
    public int lastTcpPort() {
        return 0;
    }

    @Override
    public boolean lastTcpMode() {
        return false;
    }

    @Override
    public boolean lastHostMode() {
        return false;
    }

    @Override
    public void tick() {
    }

    @Override
    public void close() {
    }
}
