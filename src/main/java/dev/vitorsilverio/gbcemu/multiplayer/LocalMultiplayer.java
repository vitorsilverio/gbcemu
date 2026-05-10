package dev.vitorsilverio.gbcemu.multiplayer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalMultiplayer implements Multiplayer{

    private SocketChannel channel;
    private final ByteBuffer buffer;
    private Path socketPath;
    private boolean isServer;
    private Path pathFile;


    public LocalMultiplayer() {
        buffer = ByteBuffer.allocate(1);
        connect();

    }

    private void connect() {
        UnixDomainSocketAddress address = null;
        Path tempDir = null;
        try {
            tempDir = Files.createTempFile("gbc", ".tmp").getParent();
            pathFile = tempDir.resolve("gbc.sock.path");
            this.pathFile = pathFile;

            if (Files.exists(pathFile)) {
                // Client
                isServer = false;
                String socketPathStr = Files.readString(pathFile);
                socketPath = Path.of(socketPathStr);
                address = UnixDomainSocketAddress.of(socketPath);
                channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                channel.connect(address);
                System.out.println("client connected");
            } else {
                // Server
                isServer = true;
                socketPath = Files.createTempFile("gbc", ".sock");
                Files.deleteIfExists(socketPath);
                address = UnixDomainSocketAddress.of(socketPath);
                try (ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
                    serverChannel.bind(address);
                    System.out.println("Server created");
                    // Write the path file
                    Files.writeString(pathFile, socketPath.toString());
                    channel = serverChannel.accept();
                }
            }
        } catch (Exception e) {
            if (e instanceof ConnectException) {
                try {
                    Files.deleteIfExists(pathFile);
                    connect();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            }else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void send(byte data) {
        try {
            channel.write(ByteBuffer.allocateDirect(data));
            channel.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte read() {
        try {
            return buffer.get();
        } catch (BufferUnderflowException e) {
            return  (byte) 0xff;
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
            if (isServer) {
                if (socketPath != null) {
                    Files.deleteIfExists(socketPath);
                }
                if (pathFile != null) {
                    Files.deleteIfExists(pathFile);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
