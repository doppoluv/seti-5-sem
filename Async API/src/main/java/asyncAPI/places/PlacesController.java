package asyncAPI.places;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import asyncAPI.places.PlacesResponse.Places;
import okhttp3.*;

public class PlacesController {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    public static CompletableFuture<PlacesResponse> getPlaces(double lat, double lon) {
        Request request = PlacesRequest.getPlaces(lat, lon);
        CompletableFuture<PlacesResponse> future = new CompletableFuture<>();

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
                Places[] places = gson.fromJson(responseBody, Places[].class);
                @SuppressWarnings("unchecked")
                CompletableFuture<Places>[] descFutures = Arrays.stream(places)
                    .map(place -> getPlacesDesc(place.getXid())
                    .thenApply(desc -> {
                        place.setDescription(desc != null && desc.getInfo() != null ? desc.getInfo().getDescription() : "");
                        return place;
                    })).toArray(CompletableFuture[]::new);

                CompletableFuture.allOf(descFutures).thenAccept(_ -> {
                    Places[] updatedPlaces = Arrays.stream(descFutures)
                        .map(CompletableFuture::join)
                        .toArray(Places[]::new);
                    future.complete(new PlacesResponse(updatedPlaces));
                }).exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                });
            }
        });
        
        return future;
    }


    public static CompletableFuture<PlacesDescription> getPlacesDesc(String xid) {
        Request request = PlacesRequest.getPlacesDesc(xid);
        CompletableFuture<PlacesDescription> future = new CompletableFuture<>();

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
                PlacesDescription desc = gson.fromJson(responseBody, PlacesDescription.class);
                future.complete(desc);
            }
        });

        return future;
    }
}
