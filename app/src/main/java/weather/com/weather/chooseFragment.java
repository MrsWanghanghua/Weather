package  weather.com.weather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import weather.com.weather.db.Country;
import weather.com.weather.db.City;
import weather.com.weather.db.Province;
import weather.com.weather.util.HttpUtil;
import weather.com.weather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by asdf on 2017/10/19.
 */

public class chooseFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY= 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> datalist = new ArrayList<>();

    //省级列表
    private List<Province> provinceList;
    //市级列表
    private List<City> cityList;
    //县列表
    private List<Country> countryList;
    //选中的省份
    private Province selectedProvince;
    //选中的城市
    private City selectedCity;
    //当前选中的级别
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1, datalist);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCountries();
                }else if (currentLevel == LEVEL_COUNTRY){
                    String weatherId = countryList.get(position).getWeatherId();

                    if(getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if (getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefresh.setRefreshing(true);
                        //weatherActivity.mWeatherId = weatherId;
                        weatherActivity.requestWeather(weatherId);
                    }

                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTRY) {
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询选中省内所有的省，优先从数据查询，如果没有再去服务器上查询
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if (provinceList.size()>0) {
            datalist.clear();
            for(Province province : provinceList) {
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据查询，如果没有再去服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceId=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0) {
            datalist.clear();
            for(City city : cityList) {
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据查询，如果没有再去服务器上查询
     */
    private void queryCountries() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countryList=DataSupport.where("cityId=?",String.valueOf(selectedCity.getId())).find(Country.class);
        if(countryList.size()>0) {
            datalist.clear();
            for(Country country : countryList) {
                datalist.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTRY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();

            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询数据
     * @param address
     * @param type
     */
    private void queryFromServer(String address,final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequests(address, new Callback() {

            @Override
            public void onFailure(Call call, final IOException e) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)) {
                    result = Utility.handleCityRersponse(responseText,selectedProvince.getId());
                }else if("country".equals(type)) {
                    result = Utility.handelCountryResponse(responseText,selectedCity.getId());
                }
                if(result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)) {
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("country".equals(type)) {
                                queryCountries();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if(progressDialog!=null) {
            progressDialog.dismiss();
        }
    }
}
