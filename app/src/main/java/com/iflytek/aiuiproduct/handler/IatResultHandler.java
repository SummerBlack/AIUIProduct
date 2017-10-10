package com.iflytek.aiuiproduct.handler;

import java.net.SocketException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.aiuiproduct.constant.ProductConstant;
import com.iflytek.aiuiproduct.player.PlayController;
import com.lamost.connection.AIUICommunication;
import com.lamost.connection.ActionListener;
import com.lamost.webservice.ElectricForVoice;
import com.lamost.webservice.SceneDataInfo;
/**
 * 听写结果处理
 * @author User
 *
 */
public class IatResultHandler {
	private static final String TAG = "IatResultHandler";
	private static final int MSG_IAT_RESULT = 3;
	private Context mContext;
	private HandlerThread mIatHandlerThread;
	private ResultHandler mResultHandler;
	private PlayController mPlayController;
	private AIUICommunication mAIUICommunication = null;

	public IatResultHandler(Context context) {
		mContext = context;
		
		mIatHandlerThread = new HandlerThread("IatResultThread");
		mIatHandlerThread.start();
		mResultHandler = new ResultHandler(mIatHandlerThread.getLooper());
		
		mPlayController = PlayController.getInstance(mContext);
		
		try {
			mAIUICommunication = AIUICommunication.getInstance();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void handleResult(JSONObject iatResult) {
		if(iatResult.length() == 0) return;
		
		String iatStr= parseIatResult(iatResult);
		
		mResultHandler.removeMessages(MSG_IAT_RESULT );
		mResultHandler.obtainMessage(MSG_IAT_RESULT, iatStr).sendToTarget();
	}
	/**
	 * 从听写结果返回的JSONObject对象中，解析出具体的文本
	 * @param resultJson
	 * @return
	 */
	private String parseIatResult(JSONObject resultJson){
		String json = resultJson.toString();
		StringBuffer ret = new StringBuffer();
		try {
			JSONTokener tokener = new JSONTokener(json);
			JSONObject joResult = new JSONObject(tokener);

			JSONArray words = joResult.getJSONArray("ws");
			for (int i = 0; i < words.length(); i++) {
				// 转写结果词，默认使用第一个结果
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				JSONObject obj = items.getJSONObject(0);
				ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return ret.toString();
	}
	public void destroy() {
		if(mIatHandlerThread != null){
			mIatHandlerThread.quit();
			mResultHandler = null;
		}
	}
	
	class ResultHandler extends Handler{
		
		public ResultHandler(Looper looper){
					super(looper);
				}

				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					switch (msg.what) {
						case MSG_IAT_RESULT:
						{
							String iatResult = (String) msg.obj;
							if(!TextUtils.isEmpty(iatResult)){
								smartHomeIatProcess(iatResult);
							}
						} break;

			default:
				break;
			}
		}
	}
	/**
	 * 根据听写结果进行处理
	 * @param iatResult
	 * 注意：对于智能家居的任何操作都必须在搜索到主控并且更新完电器信息后进行
	 */
	private void smartHomeIatProcess(String iatResult) {
		Log.e(TAG, "家电听写处理:"+iatResult);
		//更新与搜索
		if(iatResult.contains("更新")){
			try {
				if (mAIUICommunication == null) {
					mAIUICommunication = AIUICommunication.getInstance();
				}
				
				if(mAIUICommunication.updateElectric(mContext)){
					mPlayController.justTTS("", "电器设备更新成功");
				}else{
					mPlayController.justTTS("", "电器设备更新失败");
				}
				
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}else if(iatResult.contains("搜索")){
			try {
				if (mAIUICommunication == null) {
					mAIUICommunication = AIUICommunication.getInstance();
				}
				
				mAIUICommunication.searchHost(1);
				if(mAIUICommunication.getmMasterCode() != null){
					mPlayController.justTTS("", "成功搜索到家庭网关"+mAIUICommunication.getmMasterCode());
				}else{
					mPlayController.justTTS("", "未搜索到家庭网关");
				}
				
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		if (iatResult.contains("名字")) {
			Intent intent = new Intent();
			intent.setAction(ProductConstant.ACTION_SCENE_Name);

			if (iatResult.contains("叮咚叮咚")) {
				intent.putExtra("name", "叮咚叮咚");
			} else if (iatResult.contains("灵犀灵犀")){
                intent.putExtra("name", "灵犀灵犀");
			} else if (iatResult.contains("兆峰兆峰") || iatResult.contains("赵峰赵峰")){
				intent.putExtra("name","兆峰兆峰");
			} else if (iatResult.contains("大白大白")){
                intent.putExtra("name","大白大白");
			}
			mContext.sendBroadcast(intent);
		}

		// 单个电器的控制
		if(iatResult.contains("停") && mAIUICommunication.getElectricList() != null){
			for (ElectricForVoice e: mAIUICommunication.getElectricList()) {
				if(iatResult.contains(e.getElectricName())){
					String command = "<" + e.getElectricCode() + "XI" +e.getOrderInfo() + "00000000FF>";
					sendCommond(command,"已为您停止"+ e.getElectricName()+"操作");
					return;
				}
            }
			//mPlayController.justTTS("", "未添加该电器");
		}else if(iatResult.contains("打开") && mAIUICommunication.getElectricList() != null){
			for (ElectricForVoice e: mAIUICommunication.getElectricList()){

				if(iatResult.contains(e.getElectricName())){
					String command = "<" + e.getElectricCode() + "XH" +e.getOrderInfo() + "00000000FF>";
					sendCommond(command,"已为您打开"+ e.getElectricName());
					return;
				}
            }
			//mPlayController.justTTS("", "未添加该电器");
		}else if(iatResult.contains("关闭") && mAIUICommunication.getElectricList() != null){
			for (ElectricForVoice e: mAIUICommunication.getElectricList()) {

				if(iatResult.contains(e.getElectricName())){
					String command = "<" + e.getElectricCode() + "XG" +e.getOrderInfo() + "00000000FF>";
					sendCommond(command,"已为您关闭"+ e.getElectricName());
					return;
				}
			}
			//mPlayController.justTTS("", "未添加该电器");
		}
		//情景模式的控制
		if (mAIUICommunication.getSceneList() != null) {
			for (final SceneDataInfo scene : mAIUICommunication.getSceneList()) {
				if (iatResult.contains(scene.getSceneName())) {
					String command = "<00000000" + "TH00" + scene.getSceneIndex() + "*******FF>";
					//mAIUICommunication = AIUICommunication.getInstance();
					mAIUICommunication.sendCommandToHost(command, new ActionListener() {
						
						@Override
						public void onSuccess() {
							mPlayController.playText("", "好的");
						}
						
						@Override
						public void onFailed(int errorCode) {
							if(errorCode == AIUICommunication.NO_HOST){
								mPlayController.playText("", "网络有点问题，请稍后再试");
								mAIUICommunication.searchHost(1);
							}else if(errorCode == AIUICommunication.NO_ACK){
								mPlayController.playText("", "好的");
							}
						}
					});
					
				}
			}
		}
		//单独的晾衣架程序
		if (mAIUICommunication.getElectricList() == null) {
			return ;
		}
		ElectricForVoice rack = null;
		for (ElectricForVoice racks : mAIUICommunication.getElectricList()) {
			//分别对应1、2、3、4键开关
			if (racks.getElectricCode().startsWith("0F")) {
				rack = racks;//家中只能有一个晾衣架
			}
		}
		if (rack == null){
			return;
		}
		String order = null;
		
		if((iatResult.contains("上升") || iatResult.contains("升高") || iatResult.contains("高一点"))){
			order = "<" + rack.getElectricCode() + "XH" + "01" + "00000000FF>";
		}else if ((iatResult.contains("下降") || iatResult.contains("降低") || iatResult.contains("低一点"))){
			order = "<" + rack.getElectricCode() + "XH" + "03" + "00000000FF>";
		}else if ((iatResult.contains("暂停") || iatResult.contains("停止")) && rack != null){
			order = "<" + rack.getElectricCode() + "XH" + "02" + "00000000FF>";
		}else if((iatResult.contains("开") || iatResult.contains("关")) ){//开和关是相同指令
			if (iatResult.contains("照明") || iatResult.contains("灯光")){
				order = "<" + rack.getElectricCode() + "XH" + "04" + "00000000FF>";
			}else if (iatResult.contains("消毒") || iatResult.contains("杀毒") || iatResult.contains("杀菌")) {
				order = "<" + rack.getElectricCode() + "XH" + "05" + "00000000FF>";
			}else if (iatResult.contains("烘干")){
				order = "<" + rack.getElectricCode() + "XH" + "06" + "00000000FF>";
			}else if (iatResult.contains("风干")) {
				order = "<" + rack.getElectricCode() + "XH" + "07" + "00000000FF>";
			}
		}
		
		if (order != null) {
			sendCommond(order,"操作成功");
		}
	}

	/**
	 * 向家庭网关发送指令
	 * @param cmd 发送的指令
	 * @param tts 发送完要播报的
	 */
	private void sendCommond(String cmd,final String tts){
		mAIUICommunication.sendCommandToHost(cmd, new ActionListener() {
			
			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				mPlayController.playText("", tts);
			}
			
			@Override
			public void onFailed(int errorCode) {
				// TODO Auto-generated method stub
				if(errorCode == AIUICommunication.NO_HOST){
					mPlayController.playText("", "网络有点问题，请稍后再试");
					mAIUICommunication.searchHost(1);
				}else if(errorCode == AIUICommunication.NO_ACK){
					//mPlayController.playText("", "通信异常");
				}
				
			}
		});
	}
}
