package com.iflytek.aiuiproduct.receiver;

import java.net.SocketException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.iflytek.aiui.utils.log.DebugLog;
import com.iflytek.aiuiproduct.constant.ProductConstant;
import com.iflytek.aiuiproduct.player.PlayController;
import com.iflytek.aiuiproduct.utils.DevBoardControlUtil;
import com.lamost.connection.AIUICommunication;
import com.lamost.update.UpdateService;

/**
 * 网络状态广播监听器。
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年7月23日 上午10:33:03 
 *
 */
public class NetworkStateReceiver extends BroadcastReceiver {
	private final static String TAG = ProductConstant.TAG;
	private static boolean flag = false;
	private AIUICommunication mAiuiCommunication = null;
	private PlayController mPlayController = null;
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
			ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);  
	        NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
	        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	        //如果wifi已经连接上
	        if (wifiInfo.isConnected() && wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {

				DebugLog.LogD(TAG, "network connected.");
				//启动更新apk服务
	        	/*Intent serviceIntent = new Intent(context, UpdateService.class);
				context.startService(serviceIntent);*/
				
				DevBoardControlUtil.wifiStateLight(true);
				//如果是网络断掉后重新连上
				if(flag == false){
					flag = true;
					
					//需要处理的任务-->网络重连后需要重新搜索家庭网关,更新设备信息
					new Thread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								mAiuiCommunication = AIUICommunication.getInstance();
							} catch (SocketException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							mAiuiCommunication.searchHost(3);
							
							if(mAiuiCommunication.getmMasterCode() != null){
								mPlayController = PlayController.getInstance(context.getApplicationContext());
								if(mPlayController != null){
									mPlayController.playText("", "兆峰智能为您服务");
								}
								
								try {
									Thread.sleep(5000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								if(mAiuiCommunication.updateElectric(context)){
									if(mPlayController != null){
										mPlayController.playText("", "电器设备更新成功");
									}
								}
							}
						}
					}).start();
					
				}
			
	        } else {
				DebugLog.LogD(TAG, "network disconnected.");
				
				DevBoardControlUtil.wifiStateLight(false);
				//如果是网络连上后断掉
				if(flag == true){
					flag = false;
					//需要处理的任务
				}
	        }
		}
	}
}
