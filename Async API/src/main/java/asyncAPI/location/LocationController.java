package asyncAPI.location;

import asyncAPI.location.LocationResponse.Location;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import okhttp3.*;

public class LocationController {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private static boolean chosen = false;

    public static CompletableFuture<LocationResponse> getLocations(String location) {
        Request request = LocationRequest.getLocations(location);
        CompletableFuture<LocationResponse> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new IOException("Неожиданный ответ: " + response));
                    return;
                }
                String responseBody = response.body().string();
                LocationResponse locationResponse = gson.fromJson(responseBody, LocationResponse.class);
                future.complete(locationResponse);
            }
        });

        return future;
    }

    public static void printLocations(Location[] locations) {
        System.out.println("Найденные локации: ");
        for (int i = 0; i < locations.length; i++) {
            Location loc = locations[i];
            System.out.print("[" + (i + 1) + "] ");
            System.out.println(loc);
        }
    }

    public static Location chooseLocation(Location[] locations, Scanner scanner) {
        while (!chosen) {
            System.out.print("Введите номер выбранной локации: ");
            int locationIndex = scanner.nextInt();
            if (locationIndex < 1 || locationIndex > locations.length) {
                System.err.println("Неправильно набран номер");
            } else {
                chosen = true;
                return locations[locationIndex - 1];
            }
        }

        return null;
    }
}