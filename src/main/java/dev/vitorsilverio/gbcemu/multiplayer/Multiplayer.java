package dev.vitorsilverio.gbcemu.multiplayer;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class Multiplayer {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Multiplayer.class);

    private boolean connected = false;
    private SocketChannel channel;
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(1);
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(1);
    private SocketAddress address;
    private ProtocolFamily protocolFamily;



    private final Path path = Path.of("gbc.sock.path");

    public void hostLocal(String pathStr) {
        this.protocolFamily = StandardProtocolFamily.UNIX;
        Path socketFile = Path.of(pathStr);
        // Ensure the socket file does not already exist
        try {
            if (socketFile.toFile().exists()) {
                socketFile.toFile().delete();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        address = UnixDomainSocketAddress.of(socketFile);


        host();
    }

    public void hostTcp(String host, int port) {
        this.protocolFamily = StandardProtocolFamily.INET;
        try {
            address = new InetSocketAddress(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        host();
    }

    public void joinLocal(String pathStr) {
        this.protocolFamily = StandardProtocolFamily.UNIX;
        Path socketFile = Path.of(pathStr);
        // Ensure the socket file does already exist
        try {
            if (!socketFile.toFile().exists()) {
                throw new IOException("Socket file does not exist: " + socketFile);
            }
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to join local socket", e);
        }
        address = UnixDomainSocketAddress.of(socketFile);
        join();
    }

    public void joinTcp(String host, int port) {
        this.protocolFamily = StandardProtocolFamily.INET;
        try {
            address = new InetSocketAddress(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        join();
    }

    private void host() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open(protocolFamily)) {
            serverChannel.bind(address);
            logger.info("Hosting on {}", address);
            channel = serverChannel.accept();
            channel.configureBlocking(false);
            connected = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void join(){
        try {
            channel = SocketChannel.open(protocolFamily);
            channel.connect(address);
            channel.configureBlocking(false);
            connected = true;
            logger.info("Joined {}", address);
        } catch (IOException e) {
            e.printStackTrace();
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



    public byte read() {
        if (!connected) return (byte) 0xFF;

        byte lastReceivedByte = (byte) 0xFF;

        try {
            receiveBuffer.clear();
            int bytesRead = channel.read(receiveBuffer);

            if (bytesRead > 0) {
                receiveBuffer.flip();
                lastReceivedByte = receiveBuffer.get();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return lastReceivedByte;
    }

    public void disconnect() {
        connected = false;
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
