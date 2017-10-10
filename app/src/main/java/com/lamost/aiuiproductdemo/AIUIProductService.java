package com.lamost.aiuiproductdemo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.iflytek.aiui.servicekit.AIUIAgent;

import com.iflytek.aiui.utils.log.DebugLog;
import com.iflytek.aiuiproduct.AIUIProcessor;
import com.iflytek.aiuiproduct.constant.ProductConstant;

/**
 * @author C.Feng
 * @version
 * @date 2017-3-4 下午9:05:37
 * 
 */
public class AIUIProductService extends Service {

	private static final String TAG = ProductConstant.TAG;

	public static final int NOTIFICATION_ID = 1;

	public static final String ACTION = "com.lamost.aiuiproductdemo.action.AIUIProductService";

	// AIUI服务控制对象
	private AIUIAgent mAIUIAgent;

	private AIUIProcessor mProcessor;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// android.os.Debug.waitForDebugger();
		mProcessor = new AIUIProcessor(this);
		// 创建AIUIAgent对象，绑定到AIUIServcie，绑定成功之后服务即为开启状态
		// 创建AIUIAgent时传递的参数AIUIListener是用于接受AIUIService抛出事件的监听器
		if (mAIUIAgent == null) {
			mAIUIAgent = AIUIAgent.createAgent(this, mProcessor);
		}

		mProcessor.setAgent(mAIUIAgent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		DebugLog.LogD(TAG, "onStartCommand");
		startForeground();
		return super.onStartCommand(intent, Service.START_STICKY, startId);
	}

	/*
	 * 在手机休眠一段时间后（1-2小时），后台运行的服务被强行kill掉，有可能是系统回收内存的一种机制，
	 * 要想避免这种情况可以通过startForeground让服务前台运行，当stopservice的时候通过stopForeground去掉
	 */
	private void startForeground() {
		// 创建一个启动Service的Intent，从程序看，启动的是自己...
		Intent serviceIntent = new Intent();
		serviceIntent.setAction(ACTION);
		serviceIntent.setPackage(getPackageName());
		// 显示在手机状态栏的通知
		PendingIntent pendingIntent = PendingIntent.getService(
				AIUIProductService.this, 0, serviceIntent, 0);
		Notification notification = new Notification.Builder(
				AIUIProductService.this)
		// 设置显示在状态栏的通知提示消息
				.setTicker("AIUIProductService is running.")
				// 设置通知内容的标题
				.setContentTitle("AIUIProductService")
				// 设置通知内容
				.setContentText("Hello, AIUIProductService!")
				// 设置通知将要启动程序的Intent
				.setContentIntent(pendingIntent).build();
		// 使此服务在前台运行
		startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugLog.LogD(TAG, "destroy AIUIAgent");

		mProcessor.destroy();

		if (mAIUIAgent != null) {
			mAIUIAgent.destroy();
			mAIUIAgent = null;
		}

		stopForeground(true);
	}

}
