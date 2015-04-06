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

	// ѡ�еļ��𣬼���ǰ��ʾ����ʡ���У�����һ���б�
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;

	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();

	// ʡ�б�
	private List<Province> provinceList;
	// ���б�
	private List<City> cityList;
	// ���б�
	private List<County> countyList;
	// ѡ���ʡ��
	private Province selectedProvince;
	// ѡ��ĳ���
	private City selectedCity;
	// ��ǰѡ��ļ���
	private int currentLevel;

	// �ж��Ƿ��weatherActivity��ת����
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra(
				"from_weather_activity", false);
		// ���Ǵ�weatherActivity��ת����ֱ����ת
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {// ֤��sp����������
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
		// ����б����¼�
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(position);// �ȱ���ѡ�е�
					queryCities();// ��ѯѡ�е�ʡ��������
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
		queryProvinces();// Ĭ����ʾʡ������
	}

	// ��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����ݿ�û���ٵ���������ѯ
	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());// ȡ��ʡ��
			}
			adapter.notifyDataSetChanged();// ����adpterΪ��ʾ��ǰ�������
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;// ���õ�ǰ�ķ��ʼ���
		} else {
			queryFromServer(null, "province");
		}
	}

	// ��ѯѡ�е�ʡ�ڵ��У����ȴ����ݿ��ѯ�����ݿ�û���ٵ���������ѯ
	protected void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());// selectedProvince������
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

	// ��ѯѡ�е����ڵ��أ����ȴ����ݿ��ѯ�����ݿ�û���ٵ���������ѯ
	protected void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());// selectedProvince������
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

	// ���ݴ���Ĵ��ź����ʹӷ�������ѯ����һ�β�ѯһ������ã�
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {// code��Ϊ��
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";// ���ݵ�ַ
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";// ��������ʡ��
		}
		showProgressDialog();// ��ʾ���ȶԻ���
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			// ��ȡ������������
			@Override
			public void onFinish(String response) {
				boolean result = false;// ��¼Utility��ķ�������Ľ��
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
				if (result) {// ������ϣ��Ѿ������ݲ����Ӧ�ı�
					// ͨ�����·����������̴߳����߼�
					// runOnUiThread():�����߳��л������߳�
					runOnUiThread(new Runnable() {
						public void run() {
							closeProgressDialog();// �رս��ȶԻ���
							if ("province".equals(type)) {
								queryProvinces();// �����Ѵ������ݿ⣬�����ݿ���ȡ����ʾ��listview
							} else if ("city".equals(type)) {
								queryCities();// �����Ѵ������ݿ⣬�����ݿ���ȡ����ʾ��listview
							} else if ("county".equals(type)) {
								queryCounties();// �����Ѵ������ݿ⣬�����ݿ���ȡ����ʾ��listview
							}
						}
					});
				}
			}

			@Override
			public void onError(Exception e) {
				// ͨ�����·����������̴߳����߼�
				runOnUiThread(new Runnable() {
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});

	}

	// ��ʾ���ȶԻ���
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}

	// �رս��ȶԻ���
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	// ����back�������ݵ�ǰ�������жϣ�Ӧ�÷����У�ʡ�����˳�
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			if (isFromWeatherActivity) {
				// �Ǵ�weatherActivity��ת����Ҫ���˵�weatherActivity
				Intent intent = new Intent(this, weatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}

}
