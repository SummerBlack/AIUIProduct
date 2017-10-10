package com.iflytek.aiuiproduct.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.iflytek.aiuiproduct.player.PlayController;

/**
 * 收到传感器报警后，语音提示
 * @author C.Feng
 * @version 
 * @date 2017-6-19 下午6:08:04
 * 
 */
public class SensorReceiver extends BroadcastReceiver {
	private PlayController mPlayController = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		mPlayController = PlayController.getInstance(context);
		Bundle value = intent.getBundleExtra("sensor");
		String sensorType = value.getString("sensorType");
		String sensorState = value.getString("sensorState");
		if ("Z0".equals(sensorState)) {
			mPlayController.justTTS("", SensorConstant.sensorMap.get(sensorType) + "报警已被成功解除");
		} else if ("Z1".equals(sensorState)) {
			if ("0D41".equals(sensorType)) {
				mPlayController.justTTS("", "燃气传感器被触发，存在燃气泄漏的危险，请及时处理");
			} else if("0D73".equals(sensorType)){
				mPlayController.justTTS("", "打雷下雨回家受收衣服了");
			} else if("0DA1".equals(sensorType)) {
				mPlayController.justTTS("", "当前室温过高，是否需要打开空调");
			}
		}
	}
}
