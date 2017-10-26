package weather.com.weather.gson;

/**
 * Created by asdf on 2017/10/20.
 */

public class AQI {
    public AQICity city;

    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
