package asyncAPI.location;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import okhttp3.*;

public class LocationRequest {
    private static final String url = "https://graphhopper.com/api/1/geocode";
    private static final String apikey = "-";

    public static Request getLocations(String location) {
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        String uri = url + "?q=" + encodedLocation + "&limit=10&key=" + apikey;
        Request request = new Request.Builder().url(uri).get().build();
        return request;
    }
}
