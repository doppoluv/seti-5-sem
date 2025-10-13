package asyncAPI.places;

import com.google.gson.annotations.SerializedName;

public class PlacesDescription {
    @SerializedName("info")
    private Info info;

    public Info getInfo() {
        return info;
    }

    public static class Info {
        @SerializedName("descr")
        private String description;

        public String getDescription() {
            return description != null ? description : "";
        }
    }
}
