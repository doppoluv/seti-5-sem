package src;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerManager {
    private final Map<String, Long> peers = new ConcurrentHashMap<>();
    private final long timeoutMs;

    public PeerManager(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void updatePeerList(String senderIp, String senderPort) {
        long now = System.currentTimeMillis();
        String peerKey = senderIp + ":" + senderPort;
        Long lastSeen = peers.get(peerKey);
        if (lastSeen == null) {
            peers.put(peerKey, now);
            System.out.println(NetworkConfig.getTimestamp() + "Появилась новая копия: " + peerKey);
            printPeerList();
        } else {
            peers.put(peerKey, now);
        }
    }

    public void checkTimeouts() {
        long now = System.currentTimeMillis();
        List<String> removed = new ArrayList<>();
        peers.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > timeoutMs) {
                removed.add(entry.getKey());
                return true;
            }
            return false;
        });
        if (!removed.isEmpty()) {
            System.out.println(NetworkConfig.getTimestamp() + "Исчезли копии: " + String.join(", ", removed));
            printPeerList();
        }
    }

    private void printPeerList() {
        List<String> ipList = peers.keySet().stream().sorted().collect(Collectors.toList());
        System.out.println(NetworkConfig.getTimestamp() + "Живые копии: " + (ipList.isEmpty() ? "нет" : String.join(", ", ipList)));
    }
}