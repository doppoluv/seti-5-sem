package asyncAPI.location;

import com.google.gson.annotations.SerializedName;

public class LocationResponse {
    @SerializedName("hits")
    private Location[] hits;

    public Location[] getHits() {
        return hits != null ? hits : new Location[0];
    }

    public static class Location {
        @SerializedName("point")
        private Point point;
        @SerializedName("name")
        private String name;
        @SerializedName("country")
        private String country;
        @SerializedName("osm_value")
        private String osmValue;

        public Point getPoint() {
            return point;
        }

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public String getOsmValue() {
            return osmValue;
        }

        @Override
        public String toString() {
            return "Location{name='" + name + ", country='" + country + "', osmValue='" + osmValue + "'}";
        }
    }

    public static class Point {
        @SerializedName("lat")
        private double latitude;
        @SerializedName("lng")
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return "Point{lat=" + latitude + ", lng=" + longitude + "}";
        }
    }
}