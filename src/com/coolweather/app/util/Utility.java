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

//�����ʹ������������ص�����,�������
public class Utility {
	// �����ʹ���ʡ������
	public synchronized static boolean handleProvincesResponse(
			CoolWeatherDB coolWeatherDB, String response) {
		if (!TextUtils.isEmpty(response)) {// �������ݲ�Ϊ��
			String[] allProvinces = response.split(",");// ���ظ�ʽΪ������|����,����|���С��������á�,���ָ�
			if (allProvinces != null && allProvinces.length > 0) {
				for (String p : allProvinces) {// ÿ��ȡ��һ��������|���С�
					String[] array = p.split("\\|");// ������|���С����á�|���ָ�
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					// ���������������ݲ���Province����
					coolWeatherDB.saveProvince(province);
				}
				return true;
			}
		}
		return false;// ��������Ϊ�գ���false
	}

	// �����ʹ����м�����
	public synchronized static boolean handleCitiesResponse(
			CoolWeatherDB coolWeatherDB, String response, int provinceId) {
		if (!TextUtils.isEmpty(response)) {// �������ݲ�Ϊ��
			String[] allCities = response.split(",");// ���ظ�ʽΪ������|����,����|���С��������á�,���ָ�
			if (allCities != null && allCities.length > 0) {
				for (String c : allCities) {// ÿ��ȡ��һ��������|���С�
					String[] array = c.split("\\|");// ������|���С����á�|���ָ�
					City city = new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(provinceId);
					// ���������������ݲ���City����
					coolWeatherDB.saveCity(city);
				}
				return true;
			}
		}
		return false;// ��������Ϊ�գ���false
	}

	// �����ʹ����ؼ�����
	public synchronized static boolean handleCountiesResponse(
			CoolWeatherDB coolWeatherDB, String response, int cityId) {
		if (!TextUtils.isEmpty(response)) {// �������ݲ�Ϊ��
			String[] allCounties = response.split(",");// ���ظ�ʽΪ������|����,����|���С��������á�,���ָ�
			if (allCounties != null && allCounties.length > 0) {
				for (String c : allCounties) {// ÿ��ȡ��һ��������|���С�
					String[] array = c.split("\\|");// ������|���С����á�|���ָ�
					County county = new County();
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					county.setCityId(cityId);
					// ���������������ݲ���County����
					coolWeatherDB.saveCounty(county);
				}
				return true;
			}
		}
		return false;// ��������Ϊ�գ���false
	}

	// �������������ص�JSON���ݣ����������������ݴ洢������
	public static void handleWeatherResponse(Context context, String response) {
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
			String cityName = weatherInfo.getString("city");
			String weatherCode = weatherInfo.getString("cityid");//"cityid":����������
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

	// �����������ص�������Ϣ�洢��SharedPreference�ļ���
	private static void saveWeatherInfo(Context context, String cityName,
			String weatherCode, String temp1, String temp2, String weatherDesp,
			String publishTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy��M��d��", Locale.CHINA);
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