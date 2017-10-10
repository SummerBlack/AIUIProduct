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
 * @author C.Feng
 * @version 
 * @date 2017-5-18 下午8:10:31
 * 
 */
public class WindowManager extends ElectricManager {
	//保存该账户下所有的窗户
	private List<ElectricForVoice> windows = new ArrayList<>();
	//符合我们要求的窗户
	private List<ElectricForVoice> targetwindows = new ArrayList<>();
		
	public WindowManager(Context context, PlayController mPlayController)
			throws SocketException {
		super(context, mPlayController);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void mControl(JSONObject semantic, String answer)
			throws JSONException {
		// TODO Auto-generated method stub
		LogHelper.d("WindowManager.mControl");
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
		
		ElectricForVoice targetWindow = targetWindowFromResult();
		
		if (targetWindow != null && mAttr != null) {
			String order = getOrder(targetWindow, mAttr);
			sendCommond(order, answer);
		}
		
	}
	/**
	 * 从语音识别结果中搜索出符合要求的电器设备
	 * @return
	 * @throws JSONException
	 */
	private ElectricForVoice targetWindowFromResult() throws JSONException{
		//保存所有的窗户
		windows.clear();//首先清楚之前保存的
		for (ElectricForVoice window : mAIUICommunication.getElectricList()) {
			//
			if (window.getElectricCode().startsWith("07")) {
				windows.add(window);
			}
		}
		
		if (windows.size() == 0){
			//没有添加窗户设备，提示后退出
			mPlayController.justTTS("", "您还未添加任何窗户，请添加并更新设备");
			return null;
		} else if (windows.size() == 1) {
			//只有一个窗户设备，就不判断房间等信息，可以直接控制
			return windows.get(0);
		} else {
			//窗户不止一个，需要做进一步判断
			setmRoom();//解析房间
			this.mModifier = "窗户";//默认名称为窗户
			setmModifier();//解析品牌或其他描述
			targetwindows.clear();
			//房间和设备名称都符号
			for (ElectricForVoice targetWindow : windows) {
				if (targetWindow.getRoomName().equals(mRoom) && targetWindow.getElectricName().equals(mModifier)) {
					targetwindows.add(targetWindow);
				}
			}
			if (targetwindows.size() > 1) {
				//给出提示后退出,提示你的xx房间中存在xx个xx同名设备
				mPlayController.justTTS("", "检测到您的" + mRoom + "中，存在" + targetwindows.size() 
						+ "个" + mModifier + "设备，请确保同一个房间中灯具的命名不要重复！");
				return null;
				
			} else if (targetwindows.size() == 1) {
				//直接控制
				return targetwindows.get(0);
				
			} else {
				//没有符合上述要求的，就扩大搜索范围
				//保存提示信息用
				StringBuilder targetwindowsDescription = new StringBuilder();
				//targetwindows.clear();
				//房间符合
				for (ElectricForVoice targetWindow : windows) {
					if (targetWindow.getRoomName().equals(mRoom)) {
						targetwindows.add(targetWindow);
						targetwindowsDescription.append(targetWindow.getElectricName() + "，");
					}
				}
				
				if (targetwindows.size() > 1) {
					//给出提示后退出，提示该房间中存在x个灯具设备，分别为xxx，请问您想控制哪一个？
					mPlayController.justTTS("", "检测到您的" + mRoom + "中，存在" + targetwindows.size() 
							+ "个窗户，分别为" + targetwindowsDescription + "请问您想控制哪一个？");
					targetwindowsDescription = null;
					return null;
				} else if (targetwindows.size() == 1) {
					//直接控制
					targetwindowsDescription = null;
					return targetwindows.get(0);
					
				} else {
					//房间名称也没有符合的，继续搜索设备名称是否符合
					for (ElectricForVoice targetWindow : windows) {
						if (targetWindow.getElectricName().equals(mModifier)) {
							targetwindows.add(targetWindow);
							targetwindowsDescription.append(targetWindow.getRoomName() + "，");
						}
					}
					
					if (targetwindows.size() > 1) {
						//给出提示后退出，共检测到有X个xx灯具设备，注册位置分别为xxx，请问您想控制哪一个？
						mPlayController.justTTS("", "检测到存在" + targetwindows.size() 
								+ "个窗户，注册位置分别为" + targetwindowsDescription + "请问您想控制哪个房间的？");
						targetwindowsDescription = null;
						return null;
						
					} else if (targetwindows.size() == 1) {
						//直接控制
						targetwindowsDescription = null;
						return targetwindows.get(0);
						
					} else {
						//设备名称也没有符合的，给出提示没有相应的设备
						mPlayController.justTTS("", "未检测到您要控制的设备");
						targetwindowsDescription = null;
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
