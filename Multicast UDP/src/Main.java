package src;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String multicastAddress = null;
        String interfaceName = null;

        if (args.length < 1 || args.length > 2) {
            System.err.println(NetworkConfig.getTimestamp() + "Использование: java Main <multicast_address> [interface_name]");
            System.exit(1);
        }

        multicastAddress = args[0];
        if (args.length == 2) {
            interfaceName = args[1];
        }

        try {
            MulticastDiscovery discovery = new MulticastDiscovery(multicastAddress, interfaceName);
            Runtime.getRuntime().addShutdownHook(new Thread(discovery::stop));
            discovery.start();
        } catch (IllegalArgumentException e) {
            System.err.println(NetworkConfig.getTimestamp() + "Ошибка: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(NetworkConfig.getTimestamp() + "Ошибка ввода-вывода: " + e.getMessage());
            System.exit(1);
        }
    }
}