package com.lamost.smarthomeresult;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.iflytek.aiuiproduct.player.PlayController;
import com.iflytek.aiuiproduct.utils.LogHelper;
import com.lamost.ir.etclass.ETDeviceAIR;
import com.lamost.ir.etclass.ETKey;

import et.song.remote.face.IRKeyValue;
/**
 * 空调控制器
 * @author User
 *
 */
public class AircontrolManager extends ElectricManager {

	private List<ETDeviceAIR> mDeviceAIRs = new ArrayList<>();
	//空调最高/最低温度
	private final int TEMP_MAX = 30;
	private final int TEMP_MIN = 16;
	//空调最高/最低风速
	private final int WINDRATE_MAX = 4;
	private final int WINDRATE_MID= 2;
	private final int WINDRATE_MIN = 1;
	
	public AircontrolManager(Context context, PlayController mPlayController)
			throws SocketException {
		super(context, mPlayController);
		// TODO Auto-generated constructor stub
	}
	/**
	 * 子类不会继承父类的构造器，子类的构造器不管是否显示使用super调用父类构造器，子类总会调用一次
	 * （如果没有就隐式调用父类无参数的构造器，如果父类没有无参构造器，就会编译报错）
	 * @throws SocketException
	 */
	/*private AircontrolManager()throws SocketException{
		
	}*/
	
	/**
	 * 空调的控制，应该包括：开关、温度、模式、风速、风向
	 */
	@Override
	public void mControl(JSONObject semantic, String answer) throws JSONException {
		// TODO Auto-generated method stub
		LogHelper.d("AircontrolManager.mControl");
		
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
		
		mDeviceAIRs = mAIUICommunication.getmDeviceAIRs();
		if (mDeviceAIRs.size() <= 0) {
			mPlayController.justTTS("", "您还未添加空调设备，请先添加并更新设备");
			return;
		} else if (mDeviceAIRs.size() == 1) {//如果只存在一个电器设备，就忽略它的名称、房间等修饰信息，直接看它的控制属性
			ETDeviceAIR air = mDeviceAIRs.get(0);
			byte[] code = mSearchCode(air, mAttr);
			
			if (code != null) {
				String order = sendOrder(air.getmElectricCode(),code);
				sendCommond(order, answer);
			}
			
		} else {
			//有多个空调，就需要先判断，判断依据就是先判断房间，如果房间中有空调，默认打开第一个，如果还不能做出判断再解析品牌
			setmRoom();//解析房间
			setmModifier();//解析品牌
			
			int size = mDeviceAIRs.size();
			StringBuilder mRoomAndName = new StringBuilder();
			for (ETDeviceAIR air : mDeviceAIRs) {
				mRoomAndName.append(air.getmRoomName() + air.GetName() + ",");
			}
			
			if (mRoom == null && mModifier == null) {//(0,0)直接pass
				
				mPlayController.justTTS("", "检测到您有" + size + "台空调，分别为" + mRoomAndName + "请问您想控制哪一台？");
				return;
			}
			
			List<ETDeviceAIR> targetAir = new ArrayList<ETDeviceAIR>();//(1,0)或者(0,1)
			//int targetAirNum = 0;
			for (ETDeviceAIR air : mDeviceAIRs) {
				//如果有房间或品牌符合要求的
				if (air.getmRoomName().equals(mRoom) || air.GetName().equals(mModifier)) {
					//targetAirNum ++;
					targetAir.add(air);//保存满足条件的
				}
			}
			
			if (targetAir.size() == 0) {//没有满足条件的
				mPlayController.justTTS("", "检测到您有" + size + "台空调，分别为" + mRoomAndName + "请问您想控制哪一台？");
				return;
			} else if (targetAir.size() == 1) {//满足条件的就一个
				ETDeviceAIR air = targetAir.get(0);
				byte[] code = mSearchCode(air, mAttr);

				if (code != null) {
					String order = sendOrder(air.getmElectricCode(),code);
					sendCommond(order, answer);
				}
				return;
			} else {//满足条件的不止一个
				//房间优先,如果房间名称符号，就发送控制指令，不向下搜索了（一般一个房间里就一个空调）
				for (ETDeviceAIR air : targetAir) {
					if (air.getmRoomName().equals(mRoom)) {
						byte[] code = mSearchCode(air, mAttr);

						if (code != null) {
							String order = sendOrder(air.getmElectricCode(),code);
							sendCommond(order, answer);
						}
						return;
					}
				}
				//如果上述没有符合的，就搜索品牌，只要有一个匹配，就不向下搜索
				for (ETDeviceAIR air : targetAir) {
					if (air.GetName().equals(mModifier)) {
						byte[] code = mSearchCode(air, mAttr);

						if (code != null) {
							String order = sendOrder(air.getmElectricCode(),code);
							sendCommond(order, answer);
						}
						return;
					}
				}
			}
		}
	}
	/**
	 * 搜索对应的红外码
	 * @param air 空调对象，不同的空调可能温度等不一样，相同的控制属性，搜索的红外码可能也不一样
	 * @param Attr 控制属性，如打开、关闭
	 * @return
	 */
	private byte[] mSearchCode(ETDeviceAIR air, String Attr){
		if (air == null || Attr == null) {
			return null;
		}
		//按键
		int key = 0;
		//红外码
		byte[] keyValue = null;
		//默认把空调开关打开
		air.SetPower((byte)1);
		
		switch (Attr) {
		
		case "开关" :
			if ("开".equals(mAttrValue)) {
				//打开空调
				key = IRKeyValue.KEY_AIR_POWER;
				air.SetPower((byte)0);
				
			} else if ("关".equals(mAttrValue)) {
				//关闭空调
				key = IRKeyValue.KEY_AIR_POWER;
				air.SetPower((byte)1);
			}
			break;
		case "制冷" ://mode = 2
			if ("开".equals(mAttrValue)) {
				//空调设置为制冷模式
				key = IRKeyValue.KEY_AIR_MODE;
				air.SetMode((byte)1);
			}
			break;
		case "制热" ://mode = 5
			if ("开".equals(mAttrValue)) {
				//空调设置为制热模式
				key = IRKeyValue.KEY_AIR_MODE;
				air.SetMode((byte)4);
			}
			break;
		case "除湿" ://mode = 3
			if ("开".equals(mAttrValue)) {
				//空调设置为除湿模式
				key = IRKeyValue.KEY_AIR_MODE;
				air.SetMode((byte)2);
			}
			break;
		case "送风" ://mode = 4
			if ("开".equals(mAttrValue)) {
				//空调设置为送风模式
				key = IRKeyValue.KEY_AIR_MODE;
				air.SetMode((byte)3);
			}
			break;
		case "自动" ://mode = 1
			if ("开".equals(mAttrValue)) {
				//空调设置为自动模式
				key = IRKeyValue.KEY_AIR_MODE;
				air.SetMode((byte)5);
			}
			break;
		case "温度" :
			if ("Integer".equals(mAttrType)) {
				//将空调设置为temp度(温度 16-30)
				int temp = Integer.parseInt(mAttrValue);
				
				if (temp < TEMP_MIN) {
					temp = TEMP_MIN;
				} else if (temp > TEMP_MAX){
					temp = TEMP_MAX;
				}
				
				key = IRKeyValue.KEY_AIR_TEMPERATURE_IN;//温度加
				air.SetTemp((byte)(temp-1));
				
			} else if ("String".equals(mAttrType)) {
				//将空调温度设置为最大、最小
				if ("MAX".equals(mAttrValue)) {
					key = IRKeyValue.KEY_AIR_TEMPERATURE_IN;//温度加
					air.SetTemp((byte)(TEMP_MAX-1));
				} else if ("MIN".equals(mAttrValue)) {
					key = IRKeyValue.KEY_AIR_TEMPERATURE_IN;//温度加
					air.SetTemp((byte)(TEMP_MIN-1));
				}
				
			} else if ("Object(digital)".equals(mAttrType)) {
				//温度高一点，低一点,高两度...
				if ("+".equals(mAttrValue_direct)) {
					key = IRKeyValue.KEY_AIR_TEMPERATURE_IN;//温度加
					air.SetTemp((byte)(air.GetTemp()+ mAttrValue_offset - 1));
					
				} else if ("-".equals(mAttrValue_direct)) {
					key = IRKeyValue.KEY_AIR_TEMPERATURE_IN;//温度加
					air.SetTemp((byte)(air.GetTemp()- mAttrValue_offset - 1));
				}
				
			}

			break;
		case "风速" :
			if ("Integer".equals(mAttrType)) {
				//将空调风速设置为rate级(1,2,3,4)
				int rate = Integer.parseInt(mAttrValue);
				if (rate < WINDRATE_MIN) {
					rate = WINDRATE_MIN;
				} else if (rate > WINDRATE_MAX){
					rate = WINDRATE_MAX;
				}
				key = IRKeyValue.KEY_AIR_WIND_RATE;
				air.SetWindRate((byte)(rate-1));
				
			} else if ("String".equals(mAttrType)) {
				//将空调风速设置为最大、最小
				if ("MAX".equals(mAttrValue) || "高风".equals(mAttrValue) || "强劲风".equals(mAttrValue)) {
					key = IRKeyValue.KEY_AIR_WIND_RATE;
					air.SetWindRate((byte)(WINDRATE_MAX-1));
					
				} else if ("MIN".equals(mAttrValue)|| "低风".equals(mAttrValue) || "微风".equals(mAttrValue)) {
					key = IRKeyValue.KEY_AIR_WIND_RATE;
					air.SetWindRate((byte)(WINDRATE_MIN-1));
					
				} else if ("中风".equals(mAttrValue)) {
					key = IRKeyValue.KEY_AIR_WIND_RATE;
					air.SetWindRate((byte)(WINDRATE_MID-1));
				} 
				
			} else if ("Object(digital)".equals(mAttrType)) {
				//空调风速高一点，低一点...
				if ("+".equals(mAttrValue_direct)) {
					key = IRKeyValue.KEY_AIR_WIND_RATE;
					air.SetWindRate((byte)(air.GetWindRate()+ mAttrValue_offset - 1));
					
				} else if ("-".equals(mAttrValue_direct)) {
					key = IRKeyValue.KEY_AIR_WIND_RATE;
					air.SetWindRate((byte)(air.GetWindRate()- mAttrValue_offset - 1));
				}
			}

			break;
		 default:

			break;
		}
		if (key == 0) {
			return null;
		}
		
		ETKey k = air.GetKeyByValue(key);
		if (k == null) {//解决下面抛出的空指针异常
			return null;
		}
        if (k.GetState() != ETKey.ETKEY_STATE_STUDY
                && k.GetState() != ETKey.ETKEY_STATE_DIY) {
        	//如果不是电源按键，并且电源目前的状态是关闭，就不发红外码
            if (key != IRKeyValue.KEY_AIR_POWER && air.GetPower() != 0x01) {
                return null;
            }
        }else if(k.GetState() == ETKey.ETKEY_STATE_STUDY){
            keyValue = k.GetValue();
            return keyValue;
        }
       
        try {
            keyValue = air.GetKeyValue(key);
            //sendOrder(keyValue);
        } catch (Exception e) {
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
        String length = Integer.toHexString(irCode.length());
        String irCount = (length.length()<2) ? "0" + length : length;
        System.out.println("红外指令长度："+Integer.toHexString(irCode.length()));
        String irOrder = "<" + electricCode +"XM" +irCount + irCode + "FF>";
        System.out.println("红外命令："+irOrder);
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
