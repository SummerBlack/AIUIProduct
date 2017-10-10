package com.lamost.smarthomeresult;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import com.iflytek.aiuiproduct.player.PlayController;
import com.iflytek.aiuiproduct.utils.LogHelper;
import com.lamost.webservice.ElectricForVoice;

/**
 * 灯的控制
 * @author User
 * @version
 */
public class LightManager extends ElectricManager {
	//private final static String TAG = "LightManager";
	//保存该账户下所有的灯
	private List<ElectricForVoice> lights = new ArrayList<>();
	//符合我们要求的灯
	private List<ElectricForVoice> targetLights = new ArrayList<>();
	
	public LightManager(Context context, PlayController mPlayController)
			throws SocketException {
		super(context, mPlayController);
		
	}

	@Override
	public void mControl(JSONObject semantic, String answer)
			throws JSONException {
		// TODO Auto-generated method stub
		LogHelper.d("LightManager.mControl");
		this.semantic = semantic;
		this.answer = answer;
		
		//首先解析参数Attr、AttrType、AttrValue
		try {
			setmAttr();
			setmAttrType();
			setmAttrValue();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ElectricForVoice targetLight = targetLightFromResult();
		
		if (targetLight != null && mAttr != null) {
			String order = getOrder(targetLight, mAttr);
			sendCommond(order, answer);
		}
		
	}
	/**
	 * 从语音识别结果中搜索出符合要求的电器设备
	 * @return
	 * @throws JSONException
	 */
	private ElectricForVoice targetLightFromResult() throws JSONException{
		if (mAIUICommunication.getElectricList() == null) {
			return null;
		}
		//保存所有的灯的开关
		lights.clear();//首先清除之前保存的
		for (ElectricForVoice light : mAIUICommunication.getElectricList()) {
			//分别对应1、2、3、4键开关
			if (light.getElectricCode().startsWith("01") || light.getElectricCode().startsWith("02") 
					|| light.getElectricCode().startsWith("03") || light.getElectricCode().startsWith("04")) {
				lights.add(light);
			}
		}
		
		if (lights.size() == 0){
			//没有添加灯类设备，提示后退出
			mPlayController.justTTS("", "您还未添加任何灯具设备，请添加并更新设备");
			return null;
		} else if (lights.size() == 1) {
			//只有一个灯类设备，就不判断房间等信息，可以直接控制
			return lights.get(0);
		} else {
			//灯具不止一个，需要做进一步判断
			setmRoom();//解析房间
			setmModifier();//解析品牌或其他描述
			targetLights.clear();
			//房间和设备名称都符号
			for (ElectricForVoice targetLight : lights) {
				if (targetLight.getRoomName().equals(mRoom) && targetLight.getElectricName().equals(mModifier)) {
					targetLights.add(targetLight);
				}
			}
			
			if (targetLights.size() > 1) {
				//给出提示后退出,提示你的xx房间中存在xx个xx设备，？
				mPlayController.justTTS("", "检测到您的" + mRoom + "中，存在" + targetLights.size() 
						+ "个" + mModifier + "设备，请确保同一个房间中灯具的命名不要重复！");
				return null;
				
			} else if (targetLights.size() == 1) {
				//直接控制
				return targetLights.get(0);
				
			} else {
				//没有符合上述要求的，就扩大搜索范围
				//保存提示信息用
				StringBuilder targetLightsDescription = new StringBuilder();
				//targetLights.clear();
				//房间符合
				for (ElectricForVoice targetLight : lights) {
					if (targetLight.getRoomName().equals(mRoom)) {
						targetLights.add(targetLight);
						targetLightsDescription.append(targetLight.getElectricName() + "，");
					}
				}
				
				if (targetLights.size() > 1) {
					//给出提示后退出，提示该房间中存在x个灯具设备，分别为xxx，请问您想控制哪一个？
					mPlayController.justTTS("", "检测到您的" + mRoom + "中，存在" + targetLights.size() 
							+ "个灯具设备，分别为" + targetLightsDescription + "请问您想控制哪一个？");
					targetLightsDescription = null;
					return null;
				} else if (targetLights.size() == 1) {
					//直接控制
					targetLightsDescription = null;
					return targetLights.get(0);
					
				} else {
					//房间名称也没有符合的，继续搜索设备名称是否符合
					for (ElectricForVoice targetLight : lights) {
						if (targetLight.getElectricName().equals(mModifier)) {
							targetLights.add(targetLight);
							targetLightsDescription.append(targetLight.getRoomName() + "，");
						}
					}
					
					if (targetLights.size() > 1) {
						//给出提示后退出，共检测到有X个xx灯具设备，注册位置分别为xxx，请问您想控制哪一个？
						mPlayController.justTTS("", "检测到存在" + targetLights.size() 
								+ "个灯具设备，注册位置分别为" + targetLightsDescription + "请问您想控制哪个房间的？");
						targetLightsDescription = null;
						return null;
						
					} else if (targetLights.size() == 1) {
						//直接控制
						targetLightsDescription = null;
						return targetLights.get(0);
						
					} else {
						//设备名称也没有符合的，给出提示没有相应的设备
						mPlayController.justTTS("", "未检测到您要控制的设备");
						targetLightsDescription = null;
					}
				}	
			}
		}
		return null;
	}
	/**
	 * 根据电器和控制属性获取组合的控制码
	 * @param light
	 * @param Attr
	 * @return
	 */
	private String getOrder(ElectricForVoice light, String Attr){
		if (light == null || Attr == null) {
			return null;
		}
		
		String order = null;
		switch (Attr) {
		case "开关" :
			if ("开".equals(mAttrValue)) {
				order = "<" + light.getElectricCode() + "XH" +light.getOrderInfo() + "00000000FF>";
				return order;
			} else if ("关".equals(mAttrValue)) {
				order = "<" + light.getElectricCode() + "XG" +light.getOrderInfo() + "00000000FF>";
				return order;
			}
			
			break;
		case "亮度" :
			
			break;
		default:

			break;
		}

		return null;
		
	}
}
