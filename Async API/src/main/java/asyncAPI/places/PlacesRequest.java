package asyncAPI.places;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Request;

public class PlacesRequest {
    public static String baseUrl = "https://api.geoapify.com/v2/places";
    public static String detailsUrl = "https://api.geoapify.com/v2/place-details";
    private static final String apikey = Dotenv.load().get("PLACES_API");

    public static Request getPlaces(double lat, double lon) {
        String uri = baseUrl + "?categories=tourism.sights,tourism.attraction,entertainment,leisure"
                             + "&filter=circle:" + lon + "," + lat + ",10000" + "&limit=5" + "&apiKey=" + apikey;
        Request request = new Request.Builder().url(uri).get().build();
        return request;
    }

    public static Request getPlacesDesc(String placeId) {
        String uri = detailsUrl + "?id=" + placeId + "&apiKey=" + apikey;
        Request request = new Request.Builder().url(uri).get().build();
        return request;
    }
}
