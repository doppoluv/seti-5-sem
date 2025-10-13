package asyncAPI.model;

import asyncAPI.location.LocationResponse.Location;
import asyncAPI.places.PlacesResponse.Places;
import asyncAPI.weather.WeatherResponse;

public class Result {
    private Location selectedLocation;
    private WeatherResponse weather;
    private Places[] places;

    public Result(Location selectedLocation, WeatherResponse weather, Places[] places) {
        this.selectedLocation = selectedLocation;
        this.weather = weather;
        this.places = places != null ? places : new Places[0];
    }

    public Location getSelectedLocation() {
        return selectedLocation;
    }

    public WeatherResponse getWeather() {
        return weather;
    }

    public Places[] getPlaces() {
        return places;
    }
}
