package ua.askerov.routepal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
// Spring-префікс, пошуку властивості, що починаються з "ors"
// : ors.api.url, ors.api.key, ors.profile
@ConfigurationProperties(prefix = "ors")
public class OrsConfigProperties {

    // Вкладений клас для групи "api" (ors.api.*)
    private Api api = new Api();
    private String profile; // ors.profile

    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public static class Api {
        private String key; // ors.api.key
        private String url; // ors.api.url

        // Сюди ж можна додати ліміти! // TODO реалізувати? перевірити роботу!
        private Limit limit = new Limit(); // ors.api.limit.*

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public Limit getLimit() { return limit; }
        public void setLimit(Limit limit) { this.limit = limit; }
    }

    public static class Limit { // TODO оцінити життєздатність ідеї, переваги\недоліки (особливо у контексті використання різних API "на льоту")
        private Directions directions = new Directions();
        private Export export = new Export();

        public Export getExport() {
            return export;
        }

        public void setExport(Export export) {
            this.export = export;
        }

        public Directions getDirections() {
            return directions;
        }

        public void setDirections(Directions directions) {
            this.directions = directions;
        }

        public static class Directions {
            private int daily;

            public int getDaily() {
                return daily;
            }

            public void setDaily(int daily) {
                this.daily = daily;
            }
        }

        public static class Export {
            private int daily;

            public int getDaily() {
                return daily;
            }

            public void setDaily(int daily) {
                this.daily = daily;
            }
        }
    }
}