package asyncAPI.location;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import okhttp3.*;

public class LocationController {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

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
                // System.out.println("Код ответа: " + response.code());
                // System.out.println("uri: " + response.request().url());
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
}
