package SOCKS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Socks5Proxy {
    private final int port;
    private final Selector selector;
    private final Map<SocketChannel, ClientConnection> connections = new HashMap<>();
    private final DNSResolver dnsResolver;

    public Socks5Proxy(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
        this.dnsResolver = new DNSResolver(selector, this);
    }

    public void start() throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("SOCKS5 proxy started on port " + port);

        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid())
                    continue;

                try {
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isReadable()) {
                        if (key.channel() instanceof DatagramChannel) {
                            dnsResolver.processPendingResponses();
                        } else {
                            handleRead(key);
                        }
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    System.err.println("Error handling key: " + e.getMessage());
                    closeConnection(key);
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        ClientConnection conn = new ClientConnection(clientChannel);
        connections.put(clientChannel, conn);
        clientChannel.register(selector, SelectionKey.OP_READ);

        System.out.println("New client connected");
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection conn = connections.get(channel);

        if (conn == null)
            return;

        try {
            if (channel.finishConnect()) {
                System.out.println("Connected to remote: " + conn.targetHost + ":" + conn.targetPort);
                sendSuccessResponse(conn);
                conn.state = ConnectionState.CONNECTED;
                channel.register(selector, SelectionKey.OP_READ);
                conn.clientChannel.register(selector, SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to remote: " + e.getMessage());
            sendErrorResponse(conn, (byte) 5);
            closeConnection(conn);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection conn = connections.get(channel);

        if (conn == null)
            return;

        if (channel == conn.clientChannel) {
            handleClientRead(conn);
        } else if (channel == conn.remoteChannel) {
            handleRemoteRead(conn);
        }
    }

    private void handleClientRead(ClientConnection conn) throws IOException {
        int bytesRead = conn.clientChannel.read(conn.clientToRemoteBuffer);

        if (bytesRead == -1) {
            System.out.println("Client closed connection");
            closeConnection(conn);
            return;
        }

        if (bytesRead == 0)
            return;

        switch (conn.state) {
            case GREETING:
                if (processGreeting(conn)) {
                    conn.state = ConnectionState.REQUEST;
                }
                break;
            case REQUEST:
                if (processRequest(conn)) {

                }
                break;
            case CONNECTED:
                conn.clientToRemoteBuffer.flip();
                if (conn.remoteChannel != null && conn.remoteChannel.isConnected()) {
                    while (conn.clientToRemoteBuffer.hasRemaining()) {
                        conn.remoteChannel.write(conn.clientToRemoteBuffer);
                    }
                }
                conn.clientToRemoteBuffer.clear();
                break;
            default:
                break;
        }
    }

    private void handleRemoteRead(ClientConnection conn) throws IOException {
        int bytesRead = conn.remoteChannel.read(conn.remoteToClientBuffer);

        if (bytesRead == -1) {
            System.out.println("Remote closed connection");
            closeConnection(conn);
            return;
        }

        if (bytesRead > 0) {
            conn.remoteToClientBuffer.flip();
            while (conn.remoteToClientBuffer.hasRemaining()) {
                conn.clientChannel.write(conn.remoteToClientBuffer);
            }
            conn.remoteToClientBuffer.clear();
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection conn = connections.get(channel);

        if (conn == null)
            return;

        key.interestOps(SelectionKey.OP_READ);
    }

    private boolean processGreeting(ClientConnection conn) throws IOException {
        ByteBuffer buf = conn.clientToRemoteBuffer;
        if (buf.position() < 2)
            return false;

        buf.flip();
        byte version = buf.get();
        byte nMethods = buf.get();

        if (version != 5) {
            System.err.println("Invalid SOCKS version: " + version);
            closeConnection(conn);
            return false;
        }

        if (buf.remaining() < nMethods) {
            buf.compact();
            return false;
        }

        for (int i = 0; i < nMethods; i++) {
            buf.get();
        }
        buf.compact();

        ByteBuffer response = ByteBuffer.allocate(2);
        response.put((byte) 5);
        response.put((byte) 0);
        response.flip();
        conn.clientChannel.write(response);

        System.out.println("Greeting processed");
        return true;
    }

    private boolean processRequest(ClientConnection conn) throws IOException {
        ByteBuffer buf = conn.clientToRemoteBuffer;
        if (buf.position() < 4)
            return false;

        buf.flip();
        byte version = buf.get();
        byte cmd = buf.get();
        buf.get();
        byte addrType = buf.get();

        if (version != 5) {
            System.err.println("Invalid SOCKS version in request: " + version);
            buf.compact();
            return false;
        }

        if (cmd != 1) {
            System.err.println("Unsupported command: " + cmd);
            sendErrorResponse(conn, (byte) 7);
            closeConnection(conn);
            return true;
        }

        String host = null;
        int port = 0;

        try {
            if (addrType == 1) {
                if (buf.remaining() < 6) {
                    buf.rewind();
                    buf.compact();
                    return false;
                }
                byte[] addr = new byte[4];
                buf.get(addr);
                host = InetAddress.getByAddress(addr).getHostAddress();
                port = buf.getShort() & 0xFFFF;
                buf.compact();

                System.out.println("Request: IPv4 " + host + ":" + port);
                connectToRemote(conn, host, port);
                return true;

            } else if (addrType == 3) {
                if (buf.remaining() < 1) {
                    buf.rewind();
                    buf.compact();
                    return false;
                }
                int len = buf.get() & 0xFF;
                if (buf.remaining() < len + 2) {
                    buf.rewind();
                    buf.compact();
                    return false;
                }
                byte[] domain = new byte[len];
                buf.get(domain);
                host = new String(domain);
                port = buf.getShort() & 0xFFFF;
                buf.compact();

                System.out.println("Request: Domain " + host + ":" + port);
                conn.targetHost = host;
                conn.targetPort = port;
                conn.state = ConnectionState.RESOLVING;
                dnsResolver.resolve(host, conn);
                return true;

            } else {
                System.err.println("Unsupported address type: " + addrType);
                sendErrorResponse(conn, (byte) 8);
                closeConnection(conn);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            sendErrorResponse(conn, (byte) 1);
            closeConnection(conn);
            return true;
        }
    }

    void connectToRemote(ClientConnection conn, String host, int port) throws IOException {
        try {
            SocketChannel remoteChannel = SocketChannel.open();
            remoteChannel.configureBlocking(false);

            conn.remoteChannel = remoteChannel;
            conn.targetHost = host;
            conn.targetPort = port;
            connections.put(remoteChannel, conn);

            boolean connected = remoteChannel.connect(new InetSocketAddress(host, port));

            if (connected) {
                System.out.println("Connected immediately to " + host + ":" + port);
                sendSuccessResponse(conn);
                conn.state = ConnectionState.CONNECTED;
                remoteChannel.register(selector, SelectionKey.OP_READ);
                conn.clientChannel.register(selector, SelectionKey.OP_READ);
            } else {
                conn.state = ConnectionState.CONNECTING;
                remoteChannel.register(selector, SelectionKey.OP_CONNECT);
            }

        } catch (IOException e) {
            System.err.println("Failed to connect to " + host + ":" + port + " - " + e.getMessage());
            sendErrorResponse(conn, (byte) 5);
            closeConnection(conn);
            throw e;
        }
    }

    private void sendSuccessResponse(ClientConnection conn) throws IOException {
        ByteBuffer response = ByteBuffer.allocate(10);
        response.put((byte) 5);
        response.put((byte) 0);
        response.put((byte) 0);
        response.put((byte) 1);
        response.putInt(0);
        response.putShort((short) 0);
        response.flip();
        conn.clientChannel.write(response);
        System.out.println("Sent success response");
    }

    private void sendErrorResponse(ClientConnection conn, byte errorCode) throws IOException {
        ByteBuffer response = ByteBuffer.allocate(10);
        response.put((byte) 5);
        response.put(errorCode);
        response.put((byte) 0);
        response.put((byte) 1);
        response.putInt(0);
        response.putShort((short) 0);
        response.flip();
        conn.clientChannel.write(response);
        System.err.println("Sent error response: " + errorCode);
    }

    private void closeConnection(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection conn = connections.get(channel);
        if (conn != null) {
            closeConnection(conn);
        }
    }

    private void closeConnection(ClientConnection conn) {
        try {
            if (conn.clientChannel != null) {
                connections.remove(conn.clientChannel);
                conn.clientChannel.close();
            }
            if (conn.remoteChannel != null) {
                connections.remove(conn.remoteChannel);
                conn.remoteChannel.close();
            }
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}