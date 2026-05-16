package dev.vitorsilverio.gbcemu.multiplayer;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.config.AppSettings;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class Multiplayer implements MachineCycle, AutoCloseable {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Multiplayer.class);
    private static final int CHECK_INTERVAL = 4096;

    private boolean connected = false;
    private boolean hosting = false;
    private SocketChannel channel;
    private ServerSocketChannel serverChannel;
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(1);
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(1);
    private SocketAddress address;
    private ProtocolFamily protocolFamily;
    private ByteReceivedListener listener;
    private int ticks = 0;
    private String status = "Disconnected";
    private String lastLocalPath = Path.of(System.getProperty("user.dir"), "gbcemu.sock").toString();
    private String lastTcpHost = "localhost";
    private int lastTcpPort = 26803;
    private boolean lastTcpMode = false;
    private boolean lastHostMode = true;

    public Multiplayer() {
        this(AppSettings.defaults());
    }

    public Multiplayer(AppSettings settings) {
        AppSettings normalized = settings.normalized();
        lastLocalPath = normalized.multiplayerLocalPath();
        lastTcpHost = normalized.multiplayerTcpHost();
        lastTcpPort = normalized.multiplayerTcpPort();
        lastTcpMode = normalized.multiplayerTcpMode();
        lastHostMode = normalized.multiplayerHostMode();
    }

    public void setListener(ByteReceivedListener listener) {
        this.listener = listener;
    }

    public void hostLocal(String pathStr) {
        rememberLocal(pathStr, true);
        this.protocolFamily = StandardProtocolFamily.UNIX;
        Path socketFile = Path.of(pathStr);
        try {
            if (socketFile.toFile().exists()) {
                socketFile.toFile().delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        address = UnixDomainSocketAddress.of(socketFile);
        host();
    }

    public void hostTcp(String host, int port) {
        rememberTcp(host, port, true);
        this.protocolFamily = StandardProtocolFamily.INET;
        try {
            address = new InetSocketAddress(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        host();
    }

    public void joinLocal(String pathStr) {
        rememberLocal(pathStr, false);
        this.protocolFamily = StandardProtocolFamily.UNIX;
        Path socketFile = Path.of(pathStr);
        try {
            if (!socketFile.toFile().exists()) {
                throw new IOException("Socket file does not exist: " + socketFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to join local socket", e);
        }
        address = UnixDomainSocketAddress.of(socketFile);
        join();
    }

    public void joinTcp(String host, int port) {
        rememberTcp(host, port, false);
        this.protocolFamily = StandardProtocolFamily.INET;
        try {
            address = new InetSocketAddress(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        join();
    }

    private void host() {
        disconnect();
        try {
            serverChannel = ServerSocketChannel.open(protocolFamily);
            serverChannel.configureBlocking(false);
            serverChannel.bind(address);
            hosting = true;
            status = "Hosting on " + address;
            logger.info("Hosting on {}", address);
        } catch (IOException e) {
            status = "Failed to host: " + e.getMessage();
            throw new RuntimeException("Failed to host multiplayer", e);
        }
    }

    private void join() {
        disconnect();
        try {
            channel = SocketChannel.open(protocolFamily);
            channel.connect(address);
            channel.configureBlocking(false);
            connected = true;
            hosting = false;
            status = "Connected to " + address;
            logger.info("Joined {}", address);
        } catch (IOException e) {
            status = "Failed to join: " + e.getMessage();
            throw new RuntimeException("Failed to join multiplayer", e);
        }
    }

    @Override
    public void tick() {
        try {
            ticks++;
            if (ticks < CHECK_INTERVAL) return;
            ticks = 0;

            acceptPendingConnection();
            if (!connected || listener == null) return;

            receiveBuffer.clear();
            int bytesRead = channel.read(receiveBuffer);
            if (bytesRead > 0) {
                receiveBuffer.flip();
                listener.onByteReceived(receiveBuffer.get());
            }
        } catch (IOException e) {
            checkConnection(e);
            e.printStackTrace();
        }
    }

    private void acceptPendingConnection() throws IOException {
        if (!hosting || serverChannel == null) {
            return;
        }
        SocketChannel accepted = serverChannel.accept();
        if (accepted == null) {
            return;
        }
        channel = accepted;
        channel.configureBlocking(false);
        connected = true;
        hosting = false;
        status = "Connected to " + channel.getRemoteAddress();
        serverChannel.close();
        serverChannel = null;
    }

    private void checkConnection(Exception e) {
        if (channel == null || !channel.isConnected() || "Connection reset".equals(e.getMessage())) {
            logger.warn("Connection lost to {}", address);
            status = "Connection lost";
            disconnect();
        }
    }


    public void send(byte data) {
        if (!connected) return;

        try {
            sendBuffer.clear();
            sendBuffer.put(data);
            sendBuffer.flip();
            channel.write(sendBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void disconnect() {
        connected = false;
        hosting = false;
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            channel = null;
            serverChannel = null;
            status = "Disconnected";
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isHosting() {
        return hosting;
    }

    public String status() {
        return status;
    }

    public String lastLocalPath() {
        return lastLocalPath;
    }

    public String lastTcpHost() {
        return lastTcpHost;
    }

    public int lastTcpPort() {
        return lastTcpPort;
    }

    public boolean lastTcpMode() {
        return lastTcpMode;
    }

    public boolean lastHostMode() {
        return lastHostMode;
    }

    private void rememberLocal(String path, boolean hostMode) {
        lastTcpMode = false;
        lastHostMode = hostMode;
        if (path != null && !path.isBlank()) {
            lastLocalPath = path;
        }
    }

    private void rememberTcp(String host, int port, boolean hostMode) {
        lastTcpMode = true;
        lastHostMode = hostMode;
        if (host != null && !host.isBlank()) {
            lastTcpHost = host;
        }
        lastTcpPort = port;
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}
