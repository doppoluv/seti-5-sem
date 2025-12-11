package SOCKS;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientConnection {
    final SocketChannel clientChannel;
    SocketChannel remoteChannel;
    ConnectionState state = ConnectionState.GREETING;
    static final int BUFFER_SIZE = 8192;
    final ByteBuffer clientToRemoteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    final ByteBuffer remoteToClientBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    String targetHost;
    int targetPort;

    public ClientConnection(SocketChannel clientChannel) {
        this.clientChannel = clientChannel;
    }
}