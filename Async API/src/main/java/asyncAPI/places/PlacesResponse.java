package asyncAPI.places;

public class PlacesResponse {
    private Places[] features;

    public PlacesResponse(Places[] features) {
        this.features = features;
    }

    public Places[] getFeatures() {
        return features != null ? features : new Places[0];
    }
    

    public static class Places {
        private String xid;
        private String name;
        private String description;

        public String getXid() {
            return xid;
        }

        public String getName() {
            return name != null ? name : "Без названия";
        }

        public String getDescription() {
            return description != "" ? description : "Нет описания";
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
