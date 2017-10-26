package weather.com.weather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by asdf on 2017/10/20.
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
