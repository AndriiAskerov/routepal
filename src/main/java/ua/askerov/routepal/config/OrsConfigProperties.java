package ua.askerov.routepal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
// : ors.api.url, ors.api.key, ors.profile
@ConfigurationProperties(prefix = "ors")
public class OrsConfigProperties {

    // Вкладений клас для групи "api" (ors.api.*)
    private Api api = new Api();
    private String profile; // ors.profile

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public static class Api {
        private String key; // ors.api.key
        private String url; // ors.api.url
        private Limit limit = new Limit(); // ors.api.limit.*

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Limit getLimit() {
            return limit;
        }

        public void setLimit(Limit limit) {
            this.limit = limit;
        }
    }

    public static class Limit { // TODO оцінити життєздатність ідеї, переваги\недоліки (особливо у контексті використання різних API "на льоту")
        private Directions directions = new Directions();
        private Elevation elevation = new Elevation();

        public Directions getDirections() {
            return directions;
        }

        public void setDirections(Directions directions) {
            this.directions = directions;
        }

        public Elevation getElevation() {
            return elevation;
        }

        public void setElevation(Elevation elevation) {
            this.elevation = elevation;
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

        public static class Elevation {
            private Line line = new Line();
            private Point point = new Point();

            public Line getLine() {
                return line;
            }

            public void setLine(Line line) {
                this.line = line;
            }

            public Point getPoint() {
                return point;
            }

            public void setPoint(Point point) {
                this.point = point;
            }

            public static class Line {
                private int daily;

                public int getDaily() {
                    return daily;
                }

                public void setDaily(int daily) {
                    this.daily = daily;
                }
            }

            public static class Point {
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
}