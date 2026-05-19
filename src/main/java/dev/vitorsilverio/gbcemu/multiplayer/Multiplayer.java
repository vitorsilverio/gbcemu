package dev.vitorsilverio.gbcemu.multiplayer;

import dev.vitorsilverio.gbcemu.core.MachineCycle;
import dev.vitorsilverio.gbcemu.config.AppSettings;
import dev.vitorsilverio.gbcemu.connection.PhysicalConnectionListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayDeque;

public class Multiplayer implements LinkPollingConnection {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Multiplayer.class);
    private static final int MAX_DRAIN_READS_PER_CALL = 32;
    private static final int MAX_DRAIN_WRITES_PER_CALL = 32;
    private static final int RECEIVE_BUFFER_CAPACITY = 2048;

    private boolean connected = false;
    private boolean hosting = false;
    private boolean connecting = false;
    private SocketChannel channel;
    private ServerSocketChannel serverChannel;
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_CAPACITY);
    private final ArrayDeque<ByteBuffer> outboundFrames = new ArrayDeque<>();
    private SocketAddress address;
    private ProtocolFamily protocolFamily;
    private PhysicalConnectionListener listener;
    private int ticks = 0;
    private LinkPollMode pollMode = LinkPollMode.IDLE;
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

    @Override
    public void setListener(PhysicalConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void setLinkPollMode(LinkPollMode mode) {
        pollMode = mode == null ? LinkPollMode.IDLE : mode;
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
            configureSocket(channel);
            connected = channel.connect(address);
            hosting = false;
            connecting = !connected;
            status = connected ? "Connected to " + address : "Connecting to " + address;
            logger.info("{} {}", connected ? "Joined" : "Connecting to", address);
        } catch (IOException e) {
            connected = false;
            connecting = false;
            status = "Failed to join: " + e.getMessage();
            throw new RuntimeException("Failed to join multiplayer", e);
        }
    }

    @Override
    public void tick() {
        try {
            ticks++;
            if (ticks < pollMode.interval()) {
                return;
            }
            ticks = 0;

            acceptPendingConnection();
            finishPendingConnection();
            if (!connected || listener == null) {
                return;
            }

            drainOutboundBounded();
            drainReceiveBounded();
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
        configureSocket(channel);
        connected = true;
        connecting = false;
        status = "Connected to " + channel.getRemoteAddress();
        serverChannel.close();
        serverChannel = null;
    }

    private void finishPendingConnection() throws IOException {
        if (!connecting || channel == null) {
            return;
        }
        if (!channel.finishConnect()) {
            return;
        }
        connected = true;
        connecting = false;
        status = "Connected to " + address;
        logger.info("Joined {}", address);
    }

    private void configureSocket(SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);
        if (protocolFamily == StandardProtocolFamily.INET) {
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        }
    }

    private void checkConnection(IOException e) {
        logger.warn("Connection lost to {}", address, e);
        disconnect("Connection lost");
    }

    @Override
    public void send(byte[] packet) {
        enqueueOutbound(packet);
        try {
            drainOutboundBounded();
            drainReceiveBounded();
        } catch (IOException e) {
            checkConnection(e);
            e.printStackTrace();
        }
    }

    private void enqueueOutbound(byte[] packet) {
        if (!connected || packet == null || packet.length == 0) {
            return;
        }
        outboundFrames.addLast(ByteBuffer.wrap(packet.clone()));
    }

    private void drainOutboundBounded() throws IOException {
        if (!connected || channel == null) {
            return;
        }
        for (int write = 0; write < MAX_DRAIN_WRITES_PER_CALL && !outboundFrames.isEmpty(); write++) {
            ByteBuffer frame = outboundFrames.peekFirst();
            int written = channel.write(frame);
            if (written == 0) {
                return;
            }
            if (!frame.hasRemaining()) {
                outboundFrames.removeFirst();
            }
        }
    }

    public void drainReceiveBounded() throws IOException {
        if (!connected || listener == null || channel == null) {
            return;
        }

        for (int read = 0; read < MAX_DRAIN_READS_PER_CALL; read++) {
            receiveBuffer.clear();
            int bytesRead = channel.read(receiveBuffer);
            if (bytesRead < 0) {
                logger.warn("Connection closed by peer {}", address);
                disconnect("Connection closed by peer");
                return;
            }
            if (bytesRead == 0) {
                return;
            }
            byte[] packet = new byte[bytesRead];
            receiveBuffer.flip();
            receiveBuffer.get(packet);
            listener.onFrame(packet);
        }
    }

    public void disconnect() {
        disconnect("Disconnected");
    }

    private void disconnect(String finalStatus) {
        connected = false;
        hosting = false;
        connecting = false;
        setLinkPollMode(LinkPollMode.IDLE);
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
            outboundFrames.clear();
            status = finalStatus;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isHosting() {
        return hosting;
    }

    @Override
    public boolean isActive() {
        return connected || hosting || connecting;
    }

    public boolean isConnecting() {
        return connecting;
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
