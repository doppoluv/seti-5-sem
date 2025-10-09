package asyncAPI;

import asyncAPI.location.LocationController;
import asyncAPI.location.LocationResponse;
import asyncAPI.location.LocationResponse.Location;

import java.nio.charset.StandardCharsets;
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
            locFuture.thenApply(response -> {
                Location[] locations = response.getHits();
                if (locations != null && locations.length > 0) {
                    LocationController.printLocations(locations);
                    LocationController.choseLocation(locations, scanner);
                } else {
                    System.out.println("Локации не найдены");
                }
                return null;
            }).exceptionally(ex -> {
                System.err.println("Ошибка: " + ex.getMessage());
                return null;
            }).join();
        }
    }
}