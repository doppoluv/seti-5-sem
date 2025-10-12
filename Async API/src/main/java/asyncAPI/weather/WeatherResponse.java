package asyncAPI.weather;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("main")
    private Main main;
    @SerializedName("weather")
    private Weather[] weather;

    public Main getMain() {
        return main != null ? main : new Main();
    }

    public Weather[] getWeather() {
        return weather != null ? weather : new Weather[0];
    }

    public static class Main {
        @SerializedName("temp")
        private double temperature;
        @SerializedName("humidity")
        private int humidity;

        public double getTemperature() {
            return temperature;
        }

        public int getHumidity() {
            return humidity;
        }
    }

    public static class Weather {
        @SerializedName("description")
        private String description;

        public String getDescription() {
            return description != null ? description : "";
        }
    }
}