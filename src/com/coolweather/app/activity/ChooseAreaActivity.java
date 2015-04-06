package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.model.City;
import com.coolweather.app.model.CoolWeatherDB;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;
import com.coolwether.app.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {

	// 选中的级别，即当前显示的是省，市，县哪一个列表
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;

	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();

	// 省列表
	private List<Province> provinceList;
	// 市列表
	private List<City> cityList;
	// 县列表
	private List<County> countyList;
	// 选择的省份
	private Province selectedProvince;
	// 选择的城市
	private City selectedCity;
	// 当前选择的级别
	private int currentLevel;

	// 判断是否从weatherActivity跳转过来
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra(
				"from_weather_activity", false);
		// 不是从weatherActivity跳转过来直接跳转
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {// 证明sp里面有数据
			Intent intent = new Intent(this, weatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);

		coolWeatherDB = CoolWeatherDB.getInstance(this);
		// 点击列表项事件
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(position);// 先保存选中的
					queryCities();// 查询选中的省的所有市
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(position);
					queryCounties();
				} else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(position)
							.getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,
							weatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}

		});
		queryProvinces();// 默认显示省级数据
	}

	// 查询全国所有的省，优先从数据库查询，数据库没有再到服务器查询
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());// 取出省名
			}
			adapter.notifyDataSetChanged();// 更新adpter为显示当前级别的项
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;// 设置当前的访问级别
		} else {
			queryFromServer(null, "province");
		}
	}

	// 查询选中的省内的市，优先从数据库查询，数据库没有再到服务器查询
	protected void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());// selectedProvince的作用
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}

	// 查询选中的市内的县，优先从数据库查询，数据库没有再到服务器查询
	protected void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());// selectedProvince的作用
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}

	// 根据传入的代号和类型从服务器查询（第一次查询一定会调用）
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {// code不为空
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";// 数据地址
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";// 罗列所有省份
		}
		showProgressDialog();// 显示进度对话框
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			// 获取服务器的数据
			@Override
			public void onFinish(String response) {
				boolean result = false;// 记录Utility类的方法处理的结果
				if ("province".equals(type)) {
					result = Utility.handleProvincesResponse(coolWeatherDB,
							response);
				} else if ("city".equals(type)) {
					result = Utility.handleCitiesResponse(coolWeatherDB,
							response, selectedProvince.getId());
				} else if ("county".equals(type)) {
					result = Utility.handleCountiesResponse(coolWeatherDB,
							response, selectedCity.getId());
				}
				if (result) {// 处理完毕，已经将数据插入对应的表
					// 通过以下方法返回主线程处理逻辑
					// runOnUiThread():子主线程切换到主线程
					runOnUiThread(new Runnable() {
						public void run() {
							closeProgressDialog();// 关闭进度对话框
							if ("province".equals(type)) {
								queryProvinces();// 数据已存入数据库，从数据库里取出显示在listview
							} else if ("city".equals(type)) {
								queryCities();// 数据已存入数据库，从数据库里取出显示在listview
							} else if ("county".equals(type)) {
								queryCounties();// 数据已存入数据库，从数据库里取出显示在listview
							}
						}
					});
				}
			}

			@Override
			public void onError(Exception e) {
				// 通过以下方法返回主线程处理逻辑
				runOnUiThread(new Runnable() {
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});

	}

	// 显示进度对话框
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}

	// 关闭进度对话框
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	// 捕获back键，根据当前级别来判断，应该返回市，省还是退出
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			if (isFromWeatherActivity) {
				// 是从weatherActivity跳转过来要回退到weatherActivity
				Intent intent = new Intent(this, weatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}

}
