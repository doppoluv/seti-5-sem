package asyncAPI;

import asyncAPI.location.LocationController;
import asyncAPI.location.LocationResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            String location = scanner.nextLine();
            if (location.isEmpty()) {
                System.err.println("location is empty");
                System.exit(1);
            }


            CompletableFuture<LocationResponse> locFuture = LocationController.getLocations(location);
            locFuture.thenAccept(response -> {
                if (response.getHits() != null && response.getHits().length > 0) {
                    System.out.println("Найденные локации: ");
                    Arrays.stream(response.getHits()).forEach(loc -> System.out.println(loc));
                } else {
                    System.out.println("Локации не найдены");
                }
            }).exceptionally(ex -> {
                System.err.println("Ошибка:" + ex.getMessage());
                return null;
            }).join();
        }
    }
}