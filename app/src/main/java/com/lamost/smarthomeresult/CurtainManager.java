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
//控制窗帘
public class CurtainManager extends ElectricManager {
	//private final static String TAG = "CurtainManager";
	//保存该账户下所有的窗户
	private List<ElectricForVoice> curtains = new ArrayList<>();
	//符合我们要求的窗户
	private List<ElectricForVoice> targetCurtains = new ArrayList<>();
	public CurtainManager(Context context, PlayController mPlayController)
			throws SocketException {
		super(context, mPlayController);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void mControl(JSONObject semantic, String answer)
			throws JSONException {
		// TODO Auto-generated method stub
		LogHelper.d("CurtainManager.mControl");
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
		
		ElectricForVoice targetCurtain = targetCurtainFromResult();
		
		if (targetCurtain != null && mAttr != null) {
			String order = getOrder(targetCurtain, mAttr);
			sendCommond(order, answer);
		}
		
	}
	/**
	 * 从语音识别结果中搜索出符合要求的电器设备
	 * @return
	 * @throws JSONException
	 */
	private ElectricForVoice targetCurtainFromResult() throws JSONException{
		//保存所有的窗户
		curtains.clear();//首先清楚之前保存的
		for (ElectricForVoice curtain : mAIUICommunication.getElectricList()) {
			//
			if (curtain.getElectricCode().startsWith("06")) {
				curtains.add(curtain);
			}
		}
		
		if (curtains.size() == 0){
			//没有添加窗户设备，提示后退出
			mPlayController.justTTS("", "您还未添加任何窗帘，请添加并更新设备");
			return null;
		} else if (curtains.size() == 1) {
			//只有一个窗户设备，就不判断房间等信息，可以直接控制
			return curtains.get(0);
		} else {
			//窗户不止一个，需要做进一步判断
			setmRoom();//解析房间
			this.mModifier = "窗帘";//默认名称为窗户
			setmModifier();//解析品牌或其他描述
			targetCurtains.clear();
			//房间和设备名称都符号
			for (ElectricForVoice targetCurtain : curtains) {
				if (targetCurtain.getRoomName().equals(mRoom) && targetCurtain.getElectricName().equals(mModifier)) {
					targetCurtains.add(targetCurtain);
				}
			}
			if (targetCurtains.size() > 1) {
				//给出提示后退出,提示你的xx房间中存在xx个xx同名设备
				mPlayController.justTTS("", "检测到您的" + mRoom + "中，存在" + targetCurtains.size() 
						+ "个" + mModifier + "设备，请确保同一个房间中灯具的命名不要重复！");
				return null;
				
			} else if (targetCurtains.size() == 1) {
				//直接控制
				return targetCurtains.get(0);
				
			} else {
				//没有符合上述要求的，就扩大搜索范围
				//保存提示信息用
				StringBuilder targetCurtainsDescription = new StringBuilder();
				//targetCurtains.clear();
				//房间符合
				for (ElectricForVoice targetCurtain : curtains) {
					if (targetCurtain.getRoomName().equals(mRoom)) {
						targetCurtains.add(targetCurtain);
						targetCurtainsDescription.append(targetCurtain.getElectricName() + "，");
					}
				}
				
				if (targetCurtains.size() > 1) {
					//给出提示后退出，提示该房间中存在x个灯具设备，分别为xxx，请问您想控制哪一个？
					mPlayController.justTTS("", "检测到您的" + mRoom + "中，存在" + targetCurtains.size() 
							+ "个窗帘，分别为" + targetCurtainsDescription + "请问您想控制哪一个？");
					targetCurtainsDescription = null;
					return null;
				} else if (targetCurtains.size() == 1) {
					//直接控制
					targetCurtainsDescription = null;
					return targetCurtains.get(0);
					
				} else {
					//房间名称也没有符合的，继续搜索设备名称是否符合
					for (ElectricForVoice targetCurtain : curtains) {
						if (targetCurtain.getElectricName().equals(mModifier)) {
							targetCurtains.add(targetCurtain);
							targetCurtainsDescription.append(targetCurtain.getRoomName() + "，");
						}
					}
					
					if (targetCurtains.size() > 1) {
						//给出提示后退出，共检测到有X个xx灯具设备，注册位置分别为xxx，请问您想控制哪一个？
						mPlayController.justTTS("", "检测到存在" + targetCurtains.size() 
								+ "个窗户，注册位置分别为" + targetCurtainsDescription + "请问您想控制哪个房间的？");
						targetCurtainsDescription = null;
						return null;
						
					} else if (targetCurtains.size() == 1) {
						//直接控制
						targetCurtainsDescription = null;
						return targetCurtains.get(0);
						
					} else {
						//设备名称也没有符合的，给出提示没有相应的设备
						mPlayController.justTTS("", "未检测到您要控制的设备");
						targetCurtainsDescription = null;
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
	private String getOrder(ElectricForVoice window, String Attr){
		if (window == null || Attr == null) {
			return null;
		}
		
		String order = null;
		switch (Attr) {
		case "开关" :
			if ("开".equals(mAttrValue)) {
				order = "<" + window.getElectricCode() + "XH" +window.getOrderInfo() + "00000000FF>";
				return order;
			} else if ("关".equals(mAttrValue)) {
				order = "<" + window.getElectricCode() + "XG" +window.getOrderInfo() + "00000000FF>";
				return order;
			}
			
			break;
		
		default:

			break;
		}

		return null;
		
	}

	


}
