package SOCKS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DNSResolver {
    private final DatagramChannel dnsChannel;
    private final Map<Integer, ClientConnection> pendingRequests = new HashMap<>();
    private final Random random = new Random();
    private final Socks5Proxy proxy;
    private static final String DNS_SERVER = "8.8.8.8";
    private static final int DNS_PORT = 53;

    public DNSResolver(Selector selector, Socks5Proxy proxy) throws IOException {
        this.dnsChannel = DatagramChannel.open();
        this.dnsChannel.configureBlocking(false);
        this.dnsChannel.connect(new InetSocketAddress(DNS_SERVER, DNS_PORT));
        this.dnsChannel.register(selector, SelectionKey.OP_READ);
        this.proxy = proxy;
        System.out.println("DNS resolver initialized with " + DNS_SERVER);
    }

    public void resolve(String domain, ClientConnection conn) throws IOException {
        int txId = random.nextInt(65536);
        ByteBuffer query = buildDnsQuery(domain, txId);
        dnsChannel.write(query);
        pendingRequests.put(txId, conn);
        System.out.println("DNS query sent for " + domain + " (txId: " + txId + ")");
    }

    public void processPendingResponses() throws IOException {
        ByteBuffer response = ByteBuffer.allocate(512);
        int bytesRead = dnsChannel.read(response);

        if (bytesRead > 0) {
            response.flip();
            parseDnsResponse(response);
        }
    }

    private ByteBuffer buildDnsQuery(String domain, int txId) {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.putShort((short) txId);
        buffer.putShort((short) 0x0100);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);

        for (String label : domain.split("\\.")) {
            buffer.put((byte) label.length());
            buffer.put(label.getBytes());
        }
        buffer.put((byte) 0);

        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.flip();
        return buffer;
    }

    private void parseDnsResponse(ByteBuffer response) throws IOException {
        if (response.remaining() < 12) {
            return;
        }

        int txId = response.getShort() & 0xFFFF;
        ClientConnection conn = pendingRequests.remove(txId);
        if (conn == null) {
            System.err.println("Received DNS response for unknown txId: " + txId);
            return;
        }

        response.position(response.position() + 2);
        int qdCount = response.getShort() & 0xFFFF;
        int anCount = response.getShort() & 0xFFFF;
        response.position(response.position() + 4);

        for (int i = 0; i < qdCount; i++) {
            skipName(response);
            response.position(response.position() + 4);
        }

        String ip = null;
        for (int i = 0; i < anCount; i++) {
            skipName(response);
            int type = response.getShort() & 0xFFFF;
            response.position(response.position() + 6);
            int rdLength = response.getShort() & 0xFFFF;

            if (type == 1 && rdLength == 4) {
                byte[] addr = new byte[4];
                response.get(addr);
                ip = String.format("%d.%d.%d.%d", addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF, addr[3] & 0xFF);
                System.out.println("Resolved " + conn.targetHost + " to " + ip);
                break;
            } else {
                response.position(response.position() + rdLength);
            }
        }

        if (ip != null) {
            proxy.connectToRemote(conn, ip, conn.targetPort);
        } else {
            System.err.println("Failed to resolve " + conn.targetHost);
            ByteBuffer errorResponse = ByteBuffer.allocate(10);
            errorResponse.put((byte) 5);
            errorResponse.put((byte) 4);
            errorResponse.put((byte) 0);
            errorResponse.put((byte) 1);
            errorResponse.putInt(0);
            errorResponse.putShort((short) 0);
            errorResponse.flip();
            conn.clientChannel.write(errorResponse);
        }
    }

    private void skipName(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            int len = buffer.get() & 0xFF;
            if (len == 0) {
                break;
            }
            if ((len & 0xC0) == 0xC0) {
                buffer.get();
                break;
            }
            buffer.position(buffer.position() + len);
        }
    }
}