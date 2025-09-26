package src;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MulticastDiscovery {
    private final MulticastSocket receiveSocket;
    private final DatagramSocket sendSocket;
    private final InetAddress group;
    private final NetworkInterface networkInterface;
    private final int localPort;
    private final PeerManager peerManager;
    private final ScheduledExecutorService executor;
    private volatile boolean running = true;

    public MulticastDiscovery(String multicastAddress, String interfaceName) throws IOException {
        group = InetAddress.getByName(multicastAddress);

        if (!group.isMulticastAddress()) {
            throw new IllegalArgumentException("Адрес " + multicastAddress + " не является мультикастовым.");
        }

        try {
            receiveSocket = new MulticastSocket(NetworkConfig.MULTICAST_PORT);
        } catch (IOException e) {
            throw new IOException("Ошибка привязки сокета к порту " + NetworkConfig.MULTICAST_PORT + ": " + e.getMessage());
        }

        receiveSocket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);
        sendSocket = new DatagramSocket();
        localPort = sendSocket.getLocalPort();

        if (interfaceName != null) {
            networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface == null) {
                String availableInterfaces = NetworkConfig.getAvailableInterfaces();
                throw new IllegalArgumentException("Сетевой интерфейс " + interfaceName + " не найден. Доступные интерфейсы: " + availableInterfaces);
            }
        } else {
            networkInterface = NetworkConfig.findSuitableNetworkInterface(group instanceof Inet6Address);
        }

        if (networkInterface == null) {
            throw new IOException("Не найден подходящий сетевой интерфейс");
        }

        receiveSocket.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        receiveSocket.joinGroup(new InetSocketAddress(group, NetworkConfig.MULTICAST_PORT), networkInterface);

        String localAddress = NetworkConfig.getLocalAddress(networkInterface, group);
        String protocol = group instanceof Inet4Address ? "IPv4" : "IPv6";
        System.out.println(
                "--------------------------------------------------------------------------------------------\n" +
                "IP: " + (localAddress != null ? localAddress : "неизвестно") +
                ", порт: " + localPort +
                ", интерфейс: " + networkInterface.getName() +
                "\nМультикаст: " + group.getHostAddress() + ":" + NetworkConfig.MULTICAST_PORT +
                ", протокол: " + protocol +
                "\n--------------------------------------------------------------------------------------------");

        peerManager = new PeerManager(NetworkConfig.TIMEOUT_MS);
        executor = Executors.newScheduledThreadPool(2);
    }

    public void start() {
        executor.scheduleAtFixedRate(this::sendHeartbeat, 0, NetworkConfig.HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(peerManager::checkTimeouts, NetworkConfig.CHECK_INTERVAL_MS, NetworkConfig.CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        byte[] buffer = new byte[256];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                receiveSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderIp = packet.getAddress().getHostAddress();
                if (message.startsWith("Hello, I'm ")) {
                    String senderPort = message.substring(11);
                    if (!senderPort.equals(String.valueOf(localPort))) {
                        peerManager.updatePeerList(senderIp, senderPort);
                    }
                }
            } catch (IOException e) {
                if (!receiveSocket.isClosed()) {
                    System.err.println(NetworkConfig.getTimestamp() + "Ошибка при получении сообщения: " + e.getMessage());
                }
                break;
            }
        }
    }

    private void sendHeartbeat() {
        if (!running) {
            return;
        }

        try {
            String message = "Hello, I'm " + localPort;
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, NetworkConfig.MULTICAST_PORT);
            sendSocket.send(packet);
        } catch (IOException e) {
            System.err.println(NetworkConfig.getTimestamp() + "Ошибка при отправке heartbeat: " + e.getMessage());
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        executor.shutdownNow();
        try {
            receiveSocket.leaveGroup(new InetSocketAddress(group, NetworkConfig.MULTICAST_PORT), networkInterface);
        } catch (IOException e) {
            System.err.println(NetworkConfig.getTimestamp() + "Ошибка при покидании группы: " + e.getMessage());
        } finally {
            receiveSocket.close();
            sendSocket.close();
        }
    }
}