package asyncAPI.weather;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import okhttp3.*;

public class WeatherController {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    public static CompletableFuture<WeatherResponse> getWeather(double lat, double lon) {
        Request request = WeatherRequset.getWeather(lat, lon);
        CompletableFuture<WeatherResponse> future = new CompletableFuture<>();
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
                WeatherResponse weatherResponse = gson.fromJson(responseBody, WeatherResponse.class);
                future.complete(weatherResponse);
            }
        });

        return future;
    }
}