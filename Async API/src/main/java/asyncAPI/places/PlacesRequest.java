package asyncAPI.places;

import okhttp3.Request;

public class PlacesRequest {
    public static String baseUrl = "http://api.opentripmap.com/0.1/ru/places/radius";
    public static String descUrl = "http://api.opentripmap.com/0.1/ru/places/xid/";
    public static String apikey = "-";

    public static Request getPlaces(double lat, double lon) {
        String uri = baseUrl + "?radius=10000&lon=" + lon + "&lat=" + lat + "&format=json&limit=3&apikey=" + apikey;
        Request request = new Request.Builder().url(uri).get().build();
        return request;
    }

    public static Request getPlacesDesc(String xid) {
        String uri = descUrl + xid + "?apikey=" + apikey;
        Request request = new Request.Builder().url(uri).get().build();
        return request;
    }
}
