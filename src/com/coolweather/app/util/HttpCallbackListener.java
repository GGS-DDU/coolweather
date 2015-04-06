package com.coolweather.app.util;

public interface HttpCallbackListener {
	// 网络请求返回结果后调用
	void onFinish(String response);

	// 出现错误时调用
	void onError(Exception e);
}
