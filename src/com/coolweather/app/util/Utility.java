package com.coolweather.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.coolweather.app.model.City;
import com.coolweather.app.model.CoolWeatherDB;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;

//解析和处理服务器返回的数据,插入表中
public class Utility {
	// 解析和处理省级数据
	public synchronized static boolean handleProvincesResponse(
			CoolWeatherDB coolWeatherDB, String response) {
		if (!TextUtils.isEmpty(response)) {// 返回数据不为空
			String[] allProvinces = response.split(",");// 返回格式为“代号|城市,代号|城市”，所以用“,”分割
			if (allProvinces != null && allProvinces.length > 0) {
				for (String p : allProvinces) {// 每次取出一个“代号|城市”
					String[] array = p.split("\\|");// “代号|城市”，用“|”分割
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					// 将解析出来的数据插入Province表中
					coolWeatherDB.saveProvince(province);
				}
				return true;
			}
		}
		return false;// 返回数据为空，则false
	}

	// 解析和处理市级数据
	public synchronized static boolean handleCitiesResponse(
			CoolWeatherDB coolWeatherDB, String response, int provinceId) {
		if (!TextUtils.isEmpty(response)) {// 返回数据不为空
			String[] allCities = response.split(",");// 返回格式为“代号|城市,代号|城市”，所以用“,”分割
			if (allCities != null && allCities.length > 0) {
				for (String c : allCities) {// 每次取出一个“代号|城市”
					String[] array = c.split("\\|");// “代号|城市”，用“|”分割
					City city = new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(provinceId);
					// 将解析出来的数据插入City表中
					coolWeatherDB.saveCity(city);
				}
				return true;
			}
		}
		return false;// 返回数据为空，则false
	}

	// 解析和处理县级数据
	public synchronized static boolean handleCountiesResponse(
			CoolWeatherDB coolWeatherDB, String response, int cityId) {
		if (!TextUtils.isEmpty(response)) {// 返回数据不为空
			String[] allCounties = response.split(",");// 返回格式为“代号|城市,代号|城市”，所以用“,”分割
			if (allCounties != null && allCounties.length > 0) {
				for (String c : allCounties) {// 每次取出一个“代号|城市”
					String[] array = c.split("\\|");// “代号|城市”，用“|”分割
					County county = new County();
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					county.setCityId(cityId);
					// 将解析出来的数据插入County表中
					coolWeatherDB.saveCounty(county);
				}
				return true;
			}
		}
		return false;// 返回数据为空，则false
	}

	// 解析服务器返回的JSON数据，并将解析出的数据存储到本地
	public static void handleWeatherResponse(Context context, String response) {
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
			String cityName = weatherInfo.getString("city");
			String weatherCode = weatherInfo.getString("cityid");//"cityid":即天气代号
			String temp1 = weatherInfo.getString("temp1");
			String temp2 = weatherInfo.getString("temp2");
			String weatherDesp = weatherInfo.getString("weather");
			String publishTime = weatherInfo.getString("ptime");
			saveWeatherInfo(context, cityName, weatherCode, temp1, temp2,
					weatherDesp, publishTime);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	// 将服务器返回的天气信息存储到SharedPreference文件中
	private static void saveWeatherInfo(Context context, String cityName,
			String weatherCode, String temp1, String temp2, String weatherDesp,
			String publishTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();
		editor.putBoolean("city_selected", true);
		editor.putString("city_name", cityName);
		editor.putString("weather_code", weatherCode);
		editor.putString("temp1", temp1);
		editor.putString("temp2", temp2);
		editor.putString("weather_desp", weatherDesp);
		editor.putString("publish_time", publishTime);
		editor.putString("current_date", sdf.format(new Date()));
		editor.commit();
	}

}
