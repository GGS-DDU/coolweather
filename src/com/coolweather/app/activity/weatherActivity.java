package com.coolweather.app.activity;

import com.coolweather.app.service.AutoUpdateService;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;
import com.coolwether.app.R;

import android.app.Activity;
import android.app.DownloadManager.Query;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class weatherActivity extends Activity {

	private LinearLayout weatherInfoLayout;
	// 城市名
	private TextView cityNameText;
	// 发布时间
	private TextView publishText;
	// 天气描述
	private TextView weatherDespText;
	// 气温1
	private TextView temp1Text;
	// 气温2
	private TextView temp2Text;
	// 当前日期
	private TextView currentDateText;
	// 切换城市按钮
	private Button switchCity;
	// 更新天气按钮
	private Button refreshWeather;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		// 初始化
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		String countyCode = getIntent().getStringExtra("county_code");
		if (!TextUtils.isEmpty(countyCode)) {
			// 有县级代号就去查询天气
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		} else {
			// 没有县级代号就直接显示本地天气
			showWeather();
		}

		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		switchCity.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(weatherActivity.this,
						ChooseAreaActivity.class);
				intent.putExtra("from_weather_activity", true);
				startActivity(intent);
				finish();
			}
		});
		refreshWeather.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				publishText.setText("同步中...");
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(weatherActivity.this);
				String weatherCode = prefs.getString("weather_code", "");
				if (!TextUtils.isEmpty(weatherCode)) {
					queryWeatherInfo(weatherCode);
				}
			}
		});
	}

	// 查询县级代号所对应的天气代号
	private void queryWeatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city"
				+ countyCode + ".xml";
		queryFromServer(address, "countyCode");
	}

	// 查询天气代号对应得天气
	private void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/"
				+ weatherCode + ".html";
		queryFromServer(address, "weatherCode");
	}

	// 根据传入的地址和类型去向服务器查询天气代号或者天气信息
	private void queryFromServer(final String address, final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(final String response) {
				if ("countyCode".equals(type)) {
					if (!TextUtils.isEmpty(response)) {
						// 从服务器返回的数据中解析出天气代号
						String[] array = response.split("\\|");
						if (array != null && array.length == 2) {
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}
					}
				} else if ("weatherCode".equals(type)) {
					// 处理返回的天气信息
					Utility.handleWeatherResponse(weatherActivity.this,
							response);
					// 回到主线程更新ui
					runOnUiThread(new Runnable() {
						public void run() {
							showWeather();
						}
					});
				}
			}

			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						publishText.setText("同步失败");
					}
				});
			}
		});
	}

	// 从sharedpreferences文件中读取存储的天气信息并显示在界面上
	private void showWeather() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", "cityName"));
		temp1Text.setText(prefs.getString("temp1", "temp1"));
		temp2Text.setText(prefs.getString("temp2", "temp2"));
		weatherDespText.setText(prefs.getString("weather_desp", "weatherDesp"));
		publishText.setText("今天" + prefs.getString("publish_time", "publish")
				+ "发布");
		currentDateText.setText(prefs.getString("current_date", "currentDate"));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}
}
