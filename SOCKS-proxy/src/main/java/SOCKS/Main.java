package SOCKS;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Socks5Proxy <port>");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[0]);
            Socks5Proxy proxy = new Socks5Proxy(port);
            proxy.start();
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Failed to start proxy: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
