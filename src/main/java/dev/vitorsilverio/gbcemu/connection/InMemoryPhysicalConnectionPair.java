package dev.vitorsilverio.gbcemu.connection;

public final class InMemoryPhysicalConnectionPair {

    private final Endpoint left = new Endpoint(true);
    private final Endpoint right = new Endpoint(false);

    public InMemoryPhysicalConnectionPair() {
        left.peer = right;
        right.peer = left;
    }

    public PhysicalConnection left() {
        return left;
    }

    public PhysicalConnection right() {
        return right;
    }

    private static final class Endpoint implements PhysicalConnection {
        private final boolean hosting;
        private Endpoint peer;
        private PhysicalConnectionListener listener;

        private Endpoint(boolean hosting) {
            this.hosting = hosting;
        }

        @Override
        public void setListener(PhysicalConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void send(byte[] frame) {
            if (peer != null && peer.listener != null && frame != null && frame.length > 0) {
                peer.listener.onFrame(frame.clone());
            }
        }

        @Override
        public void hostLocal(String path) {
            // Already connected in memory.
        }

        @Override
        public void joinLocal(String path) {
            // Already connected in memory.
        }

        @Override
        public void hostTcp(String host, int port) {
            // Already connected in memory.
        }

        @Override
        public void joinTcp(String host, int port) {
            // Already connected in memory.
        }

        @Override
        public void disconnect() {
            Endpoint oldPeer = peer;
            peer = null;
            if (oldPeer != null && oldPeer.peer == this) {
                oldPeer.peer = null;
            }
        }

        @Override
        public boolean isConnected() {
            return peer != null;
        }

        @Override
        public boolean isHosting() {
            return hosting;
        }

        @Override
        public boolean isActive() {
            return isConnected();
        }

        @Override
        public String status() {
            return isConnected() ? "Connected in memory" : "Disconnected";
        }

        @Override
        public String lastLocalPath() {
            return "in-memory";
        }

        @Override
        public String lastTcpHost() {
            return "localhost";
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
            return hosting;
        }

        @Override
        public void tick() {
            // In-memory delivery is synchronous in send().
        }

        @Override
        public void close() {
            disconnect();
        }
    }
}
