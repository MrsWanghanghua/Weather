package weather.com.weather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import weather.com.weather.gson.Weather;
import weather.com.weather.util.HttpUtil;
import weather.com.weather.util.Utility;

/**
 * Created by asdf on 2017/10/24.
 */

public class AutoUpdateService extends Service{
    public AutoUpdateService(){

    }

    public IBinder onBind(Intent intent){
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        updateWeather();//更新天气信息
        updateBingPic();//更新必应图片
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 100;
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        alarmManager.cancel(pi);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气信息
     */
    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);

        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=6985f1f7f23c47b6ae4861c7f04e72f1";
            HttpUtil.sendOkHttpRequests(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weatherData = Utility.handleWeatherResponse(responseText);
                    if(weatherData != null && "ok".equals(weatherData.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }
            });
        }
    }

    /**
     * 更新必应每日一图
     */
    private void updateBingPic(){
        String bingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequests(bingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }
        });
    }
}
