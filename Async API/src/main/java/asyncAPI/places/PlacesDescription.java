package asyncAPI.places;

import com.google.gson.annotations.SerializedName;

public class PlacesDescription {
    @SerializedName("features")
    private Feature[] features;

    public Info getInfo() {
        if (features != null && features.length > 0 && features[0].properties != null) {
            return new Info(features[0].properties);
        }
        return new Info(null);
    }

    public static class Feature {
        @SerializedName("properties")
        private Properties properties;
    }

    public static class Properties {
        @SerializedName("datasource")
        private Datasource datasource;

        @SerializedName("name")
        private String name;

        @SerializedName("address_line1")
        private String address;

        @SerializedName("address_line2")
        private String city;
    }

    public static class Datasource {
        @SerializedName("raw")
        private Raw raw;
    }

    public static class Raw {
        @SerializedName("wikipedia")
        private String wikipedia;

        @SerializedName("description")
        private String description;
    }

    public static class Info {
        private Properties properties;

        public Info(Properties properties) {
            this.properties = properties;
        }

        public String getDescription() {
            if (properties == null) {
                return "";
            }

            if (properties.datasource != null && properties.datasource.raw != null && properties.datasource.raw.description != null) {
                return properties.datasource.raw.description;
            }
            
            return "";
        }
    }
}
