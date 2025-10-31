package asyncAPI.places;

import com.google.gson.annotations.SerializedName;

public class PlacesResponse {
    @SerializedName("features")
    private Places[] features;

    public PlacesResponse(Places[] features) {
        this.features = features;
    }

    public Places[] getFeatures() {
        return features != null ? features : new Places[0];
    }
    

    public static class Places {
        @SerializedName("properties")
        private Properties properties;

        private String description;

        public String getXid() {
            return properties != null ? properties.getPlaceId() : null;
        }

        public String getName() {
            if (properties == null) {
                return "Без названия";
            }
            String name = properties.getName();
            return name != null ? name : "Без названия";
        }

        public String getDescription() {
            return description != null && !description.isEmpty() ? description : "Нет описания";
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static class Properties {
            @SerializedName("place_id")
            private String placeId;
            
            @SerializedName("name")
            private String name;

            public String getPlaceId() {
                return placeId;
            }

            public String getName() {
                return name;
            }
        }
    }
}
