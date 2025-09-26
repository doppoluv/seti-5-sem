import java.net.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.StringJoiner;

public class NetworkConfig {
    public static final int MULTICAST_PORT = 23209;
    public static final long HEARTBEAT_INTERVAL_MS = 3000;
    public static final long TIMEOUT_MS = 9000;
    public static final long CHECK_INTERVAL_MS = 1000;
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String getTimestamp() {
        return "[" + TIME_FORMATTER.format(java.time.LocalDateTime.now()) + "] ";
    }

    public static String getAvailableInterfaces() throws SocketException {
        StringJoiner sj = new StringJoiner(", ");
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface ni : Collections.list(nis)) {
            sj.add(ni.getName() + " (" + ni.getDisplayName() + ")");
        }
        return sj.length() > 0 ? sj.toString() : "нет";
    }

    public static String getLocalAddress(NetworkInterface ni, InetAddress group) {
        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
            InetAddress addr = ia.getAddress();
            if ((group instanceof Inet6Address && addr instanceof Inet6Address) ||
                (group instanceof Inet4Address && addr instanceof Inet4Address && !addr.isLoopbackAddress())) {
                return addr.getHostAddress();
            }
        }
        return null;
    }

    public static NetworkInterface findSuitableNetworkInterface(boolean ipv6) throws SocketException {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface ni : Collections.list(nis)) {
            if (ni.supportsMulticast() && !ni.isLoopback() && ni.isUp()) {
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if ((ipv6 && addr instanceof Inet6Address && addr.isLinkLocalAddress()) ||
                        (!ipv6 && addr instanceof Inet4Address && !addr.isLoopbackAddress())) {
                        return ni;
                    }
                }
            }
        }
        return null;
    }
}