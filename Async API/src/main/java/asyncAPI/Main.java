package asyncAPI;

import asyncAPI.location.LocationController;
import asyncAPI.location.LocationResponse;
import asyncAPI.location.LocationResponse.Location;
import asyncAPI.weather.WeatherController;
import asyncAPI.weather.WeatherResponse;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.print("Введите название: ");
            String location = scanner.nextLine();
            if (location.isEmpty()) {
                System.err.println("Ошибка: вы не ввели название локации");
                System.exit(1);
            }

            CompletableFuture<LocationResponse> locFuture = LocationController.getLocations(location);
            locFuture.thenApply(response -> {
                Location[] locations = response.getHits();
                if (locations == null || locations.length == 0) {
                    System.out.println("Локации не найдены");
                    return CompletableFuture.completedFuture(null);
                }

                LocationController.printLocations(locations);

                Location chosenLocation = LocationController.chooseLocation(locations, scanner);
                if (chosenLocation == null) {
                    System.err.println("Ошибка с выбором локации");
                    return CompletableFuture.completedFuture(null);
                }

                CompletableFuture<WeatherResponse> weatherFuture = WeatherController.getWeather(
                    chosenLocation.getPoint().getLatitude(), chosenLocation.getPoint().getLongitude());
                
                return CompletableFuture.completedFuture(chosenLocation);
            }).exceptionally(ex -> {
                System.err.println("Ошибка: " + ex.getMessage());
                return CompletableFuture.completedFuture(null);
            }).whenComplete((_, _) -> {
                System.exit(1);
            }).join();
        }
    }
}