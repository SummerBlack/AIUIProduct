package com.lamost.smarthomeresult;

import java.net.SocketException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.iflytek.aiuiproduct.player.PlayController;
import com.lamost.webservice.ElectricForVoice;

/**
 * @author C.Feng
 * @version 
 * @date 2017-6-23 上午10:24:37
 * 
 */
public class RacksManager extends ElectricManager {
	private ElectricForVoice racks = null;

	public RacksManager(Context context, PlayController mPlayController)
			throws SocketException {
		super(context, mPlayController);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void mControl(JSONObject semantic, String answer)
			throws JSONException {
		this.semantic = semantic;
		this.answer = answer;
		if (TextUtils.isEmpty(answer)) {
			answer = "操作成功";
		}
		//首先解析参数Attr、AttrType、AttrValue
		try {
			setmAttr();
			setmAttrType();
			setmAttrValue();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ElectricForVoice targetRacks = targetRacksFromResult();
		if (targetRacks != null) {
			String order = getOrder(targetRacks, mAttr);
			sendCommond(order, answer);
		}else {
			mPlayController.justTTS("", "您还未添加晾衣架设备，请添加并更新设备");
		}
		

	}
	/**
	 * 从语音识别结果中搜索出符合要求的电器设备
	 * @return
	 * @throws JSONException
	 */
	private ElectricForVoice targetRacksFromResult(){
		if (mAIUICommunication.getElectricList() == null) {
			return null;
		}
		
		for (ElectricForVoice racks : mAIUICommunication.getElectricList()) {
			//分别对应1、2、3、4键开关
			if (racks.getElectricCode().startsWith("0F")) {
				this.racks = racks;//家中只能有一个晾衣架
			}
		}
		
		return this.racks;
	}
	/**
	 * 根据电器和控制属性获取组合的控制码
	 * @param light
	 * @param Attr
	 * @return
	 */
	private String getOrder(ElectricForVoice racks, String Attr){
		if (racks == null || Attr == null) {
			return null;
		}
		
		String order = null;
		switch (Attr) {
		case "照明" :
			if ("开".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "04" + "00000000FF>";
				return order;
			} else if ("关".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XG" + "04" + "00000000FF>";
				return order;
			}
			
			break;
		case "消毒" :
			if ("开".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "05" + "00000000FF>";
				return order;
			} else if ("关".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XG" + "05" + "00000000FF>";
				return order;
			}
			
			break;
		case "烘干" :
			if ("开".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "06" + "00000000FF>";
				return order;
			} else if ("关".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XG" + "06" + "00000000FF>";
				return order;
			}
			
			break;
		case "风干" :
			if ("开".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "07" + "00000000FF>";
				return order;
			} else if ("关".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XG" + "07" + "00000000FF>";
				return order;
			}
			
			break;
		case "状态" :
			if ("升".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "01" + "00000000FF>";
				return order;
			} else if ("降".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "03" + "00000000FF>";
				return order;
			}else if ("暂停".equals(mAttrValue)) {
				order = "<" + racks.getElectricCode() + "XH" + "02" + "00000000FF>";
				return order;
			}
			
			
			break;
		default:

			break;
		}

		return null;
		
	}

}
