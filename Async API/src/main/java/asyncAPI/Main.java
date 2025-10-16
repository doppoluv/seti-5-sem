package asyncAPI;

import asyncAPI.location.LocationController;
import asyncAPI.location.LocationResponse;
import asyncAPI.location.LocationResponse.Location;
import asyncAPI.model.Result;
import asyncAPI.places.PlacesController;
import asyncAPI.places.PlacesResponse;
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
            locFuture.thenCompose(response -> {
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
                CompletableFuture<PlacesResponse> placesFuture = PlacesController.getPlaces(
                        chosenLocation.getPoint().getLatitude(), chosenLocation.getPoint().getLongitude());

                return CompletableFuture.allOf(weatherFuture, placesFuture)
                        .thenCompose(_ -> {
                            WeatherResponse weather = weatherFuture.join();
                            PlacesResponse places = placesFuture.join();
                            PlacesResponse.Places[] placeArray = places.getFeatures();
                            return CompletableFuture.completedFuture(new Result(chosenLocation, weather, placeArray));
                        });
            }).exceptionally(ex -> {
                System.err.println("Ошибка: " + ex.getMessage());
                System.exit(1);
                return null;
            }).whenComplete((result, _) -> {
                if (result != null) {
                    System.out.println("\nИтоговый результат:");
                    System.out.println("- Локация: " + result.getSelectedLocation());
                    System.out.println("- Погода: " + (result.getWeather() != null && 
                            result.getWeather().getWeather().length > 0 ?
                            result.getWeather().getMain().getTemperature() + "°C, " +
                            result.getWeather().getWeather()[0].getDescription() : "Нет данных о погоде"));
                    
                    System.out.println("- Интересные места: ");
                    int placesCount = 1;
                    for (PlacesResponse.Places place : result.getPlaces()) {
                        if (place.getName().isEmpty()) {
                            continue;
                        }
                        System.out.println("[" + placesCount + "] " + place.getName() + ": " + place.getDescription());
                        placesCount++;
                    }
                    if (placesCount == 1) {
                        System.out.println("    Нет информации по интересным местам");
                    }
                }

                System.out.println("\nПрограмма завершена.");
                System.exit(0);
            }).join();
        }
    }
}