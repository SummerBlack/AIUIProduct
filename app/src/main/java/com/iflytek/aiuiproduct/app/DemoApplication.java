package com.iflytek.aiuiproduct.app;

import android.app.Application;

import com.iflytek.aiui.utils.log.DebugLog;
import com.iflytek.aiuiproduct.constant.ProductConstant;
import com.iflytek.aiuiproduct.exception.CrashCollector;

/**
 * Demo的Application对象。
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年6月28日 下午10:07:00
 * 
 */
public class DemoApplication extends Application {
	private static final String TAG = ProductConstant.TAG;
	private static final boolean SAVE_CRASHLOG = true;

	// private DeamonClient mDeamonClient;

	@Override
	public void onCreate() {
		super.onCreate();
		if (SAVE_CRASHLOG) {
			CrashCollector collector = CrashCollector.getInstance();
			collector.init(getApplicationContext());
		}
		// registerDeamon();
		DebugLog.LogD(TAG, "onCreate");
	}

	/*
	 * private void registerDeamon() { StartAction startAction = new
	 * StartAction("service",
	 * "com.lamost.aiuiproductdemo.action.AIUIProductService");
	 * 
	 * mDeamonClient = new DeamonClient();
	 * mDeamonClient.register(getApplicationContext(), startAction);
	 * 
	 * DebugLog.LogD(TAG, "registerDeamon"); }
	 */

}
