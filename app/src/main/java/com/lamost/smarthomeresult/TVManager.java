package com.lamost.smarthomeresult;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.iflytek.aiuiproduct.player.PlayController;
import com.iflytek.aiuiproduct.utils.LogHelper;
import com.lamost.ir.etclass.ETDeviceTV;

import et.song.remote.face.IRKeyValue;

/**
 * @author C.Feng
 * @version 
 * @date 2017-9-28 下午6:53:30
 * 
 */
public class TVManager extends ElectricManager {
	private List<ETDeviceTV> mDeviceTVs = new ArrayList<>();
	private final int VOICE_MAX = 100;
	private final int VOICE_MIN = 0;

	public TVManager(Context context, PlayController mPlayController)
			throws SocketException {
		super(context, mPlayController);

	}

	@Override
	public void mControl(JSONObject semantic, String answer)
			throws JSONException {
		// TODO Auto-generated method stub
		LogHelper.d("TVManager.mControl");
		
		this.semantic = semantic;

		if (!TextUtils.isEmpty(answer)) {
			this.answer = answer;
		}else {
			this.answer = "操作成功";
		}

		
		//首先解析参数Attr、AttrType、AttrValue
		try {
			setmAttr();
			setmAttrType();
			setmAttrValue();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// 获取电视列表
		mDeviceTVs = mAIUICommunication.getmDeviceTVs();
		
		if (mDeviceTVs.size() <= 0) {
			mPlayController.justTTS("", "您还未添加电视设备，请先添加并更新设备");
			return;
		} else if (mDeviceTVs.size() == 1) {//如果只存在一个电器设备，就忽略它的名称、房间等修饰信息，直接看它的控制属性
			ETDeviceTV air = mDeviceTVs.get(0);
			byte[] code = mSearchCode(air, mAttr);
			
			if (code != null) {
				String order = sendOrder(air.getmElectricCode(),code);
				sendCommond(order, this.answer);
			}
			
		} else {
			//有多个电视，就需要先判断，判断依据就是先判断房间，如果房间中有空调，默认打开第一个，如果还不能做出判断再解析品牌
			setmRoom();//解析房间
			setmModifier();//解析品牌
			
			int size = mDeviceTVs.size();
			StringBuilder mRoomAndName = new StringBuilder();
			for (ETDeviceTV air : mDeviceTVs) {
				mRoomAndName.append(air.getmRoomName() + air.GetName() + ",");
			}
			
			if (mRoom == null && mModifier == null) {//(0,0)直接pass
				mPlayController.justTTS("", "检测到您有" + size + "台电视，分别为" + mRoomAndName + "请问您想控制哪一台？");
				return;
			}
			
			List<ETDeviceTV> targetTV = new ArrayList<>();//(1,0)或者(0,1)
			//int targetAirNum = 0;
			for (ETDeviceTV tv : mDeviceTVs) {
				//如果有房间或品牌符合要求的
				if (tv.getmRoomName().equals(mRoom) || tv.GetName().equals(mModifier)) {
					//targetAirNum ++;
					targetTV.add(tv);//保存满足条件的
				}
			}
			
			if (targetTV.size() == 0) {//没有满足条件的
				mPlayController.justTTS("", "检测到您有" + size + "台电视，分别为" + mRoomAndName + "请问您想控制哪一台？");
				return;
			} else if (targetTV.size() == 1) {//满足条件的就一个
				ETDeviceTV tv = targetTV.get(0);
				byte[] code = mSearchCode(tv, mAttr);

				if (code != null) {
					String order = sendOrder(tv.getmElectricCode(),code);
					sendCommond(order, this.answer);
				}
				return;
			} else {//满足条件的不止一个
				//房间优先,如果房间名称符号，就发送控制指令，不向下搜索了（一般一个房间里就一个空调）
				for (ETDeviceTV tv : targetTV) {
					if (tv.getmRoomName().equals(mRoom)) {
						byte[] code = mSearchCode(tv, mAttr);

						if (code != null) {
							String order = sendOrder(tv.getmElectricCode(),code);
							sendCommond(order, this.answer);
						}
						return;
					}
				}
				//如果上述没有符合的，就搜索品牌，只要有一个匹配，就不向下搜索
				for (ETDeviceTV tv : targetTV) {
					if (tv.GetName().equals(mModifier)) {
						byte[] code = mSearchCode(tv, mAttr);

						if (code != null) {
							String order = sendOrder(tv.getmElectricCode(),code);
							sendCommond(order, this.answer);
						}
						return;
					}
				}
			}
		}

	}
	
	/**
	 * 搜索对应的红外码
	 * @param tv 空调对象，不同的空调可能温度等不一样，相同的控制属性，搜索的红外码可能也不一样
	 * @param Attr 控制属性，如打开、关闭
	 * @return
	 */
	private byte[] mSearchCode(ETDeviceTV tv, String Attr){
		if (tv == null || Attr == null) {
			return null;
		}
		//按键
		int key = 0;
		//红外码
		byte[] keyValue = null;

		switch (Attr) {
		
		case "开关" :
			if ("开".equals(mAttrValue)) {
				//打开电视
				key = IRKeyValue.KEY_TV_POWER;
				//air.SetPower((byte)0);
			} else if ("关".equals(mAttrValue)) {
				//关闭电视
				key = IRKeyValue.KEY_TV_POWER;
			}
			break;
		case "静音":
			if ("设置".equals(mAttrValue)) {
				key = IRKeyValue.KEY_TV_MUTE;
			}
			break;
		case "频道":
			if ("Integer".equals(mAttrType)) { //设置具体频道
				// 电视的频道
				int channel = Integer.parseInt(mAttrValue);
				
			} else if ("Object(digital)".equals(mAttrType)) { // 频道增加或减少
				if ("+".equals(mAttrValue_direct)) {
					key = IRKeyValue.KEY_TV_CHANNEL_IN;
				} else if ("-".equals(mAttrValue_direct)) {
					key = IRKeyValue.KEY_TV_CHANNEL_OUT;
				}
			}
			break;
	
		case "音量" :
			if ("Integer".equals(mAttrType)) { // 设置具体音量，
				// 电视的音量
				int voice = Integer.parseInt(mAttrValue);
				
			} else if ("String".equals(mAttrType)) {
				//将电视音量设置为最大、最小
				if ("MAX".equals(mAttrValue)) {
					
				} else if ("MIN".equals(mAttrValue)) {
					
				}
			} else if ("Object(digital)".equals(mAttrType)) {
				
				if ("+".equals(mAttrValue_direct)) { // 音量加
					key = IRKeyValue.KEY_TV_VOLUME_IN;
				} else if ("-".equals(mAttrValue_direct)) {//音量减
					key = IRKeyValue.KEY_TV_VOLUME_OUT;	
				}	
			}
			break;
		 default:

			break;
		}
		if (key == 0) {
			return null;
		}
		
		try {
			keyValue = tv.GetKeyValueEx(key);
			if (keyValue == null) {
				keyValue = tv.GetKeyValue(key);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return keyValue;
	}
	/**
	 * 根据传入的红外码返回发送的指令
	 * @param electricCode
	 * @param bytes
	 * @return
	 */
	private String sendOrder(String electricCode, byte[] bytes){
		if (electricCode == null || bytes == null) {
			return null;
		}
        final String irCode = bytestoIrCode(bytes);
		String irOrder;
        String length = Integer.toHexString(irCode.length());
        String irCount = (length.length() < 2) ? "0" + length : length;
        System.out.println("红外指令长度："+Integer.toHexString(irCode.length()));
		if(length.equals("20")) {
			irOrder = "<" + electricCode + "XM" + irCount + irCode + "FF>";
		}else{
			irOrder = "<" + electricCode + "XM" + irCount + irCode +"000"+ "FF>";
		}
       // String irOrder = "<" + electricCode +"XM" +irCount + irCode + "FF>";
        System.out.println("红外命令：" + irOrder);
		return irOrder;
    }
    
    private String bytestoIrCode(byte[] bytes){
        String irCode = "";
        if(bytes.length == 4){
            irCode = String.valueOf(bytes);
        }else {
            irCode = bytes2Order(bytes);
        }
        return irCode;
    }
    
    private String bytes2Order(byte[] bytes){
        String irCode = byte2hex(bytes);
        System.out.println("红外码："+irCode);
        return irCode;
    }
   
    /**
     * 字节码转换为16进制字符串
     * @param bytes
     * @return
     */
    public static String byte2hex(byte[] bytes){
        String hs = "";
        String stmp = "";
        for(int i = 0;i<bytes.length;i++) {
            stmp = Integer.toHexString(bytes[i] & 0XFF);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

}
