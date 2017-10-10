package com.lamost.aiuiproductdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class LaunchReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("LaunchReceiver", "开机自动启动");
		Intent serviceIntent = new Intent(AIUIProductService.ACTION);
		serviceIntent.setPackage(context.getPackageName());
		context.startService(serviceIntent);
	}

}
