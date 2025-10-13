package asyncAPI.weather;

import okhttp3.Request;
import io.github.cdimascio.dotenv.Dotenv;

public class WeatherRequset {
    private static final String url = "https://api.openweathermap.org/data/2.5/weather";
    private static final String apikey = Dotenv.load().get("WEATHER_API");

    public static Request getWeather(double lat, double lon) {
        String uri = url + "?lat=" + lat + "&lon=" + lon + "&appid=" + apikey + "&units=metric&lang=ru";
        Request request = new Request.Builder().url(uri).get().build();
        return request;
    }
}