package com.iflytek.aiuiproduct.handler.disposer;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.iflytek.aiuiproduct.handler.entity.SemanticResult;
import com.iflytek.aiuiproduct.handler.entity.ServiceType;
import com.iflytek.aiuiproduct.player.PlayController;
import com.iflytek.aiuiproduct.utils.LogHelper;
import com.lamost.smarthomeresult.AircontrolManager;
import com.lamost.smarthomeresult.ElectricManager;
import com.lamost.smarthomeresult.TVManager;

/**
 * 智能家居控制语义结果处理器
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年6月21日 下午4:30:45
 * 
 */
public class SmartHomeDisposer extends Disposer {
	//private final static  String TAG = "SmartHomeDisposer";
	/** 智能家居家电类型 **/
	private final static String KEY_SERVICE = "service";
	//灯
	private final static String ELETYPE_LIGHT = "light_smartHome";
	//空调
	private final static String ELETYPE_AIRCONTROL = "airControl_smartHome";
	// 电视
	private final static String ELETYPE_TV = "tv_smartHome";
	//风扇
	private final static String ELETYPE_FAN = "fan_smartHome";
	//窗帘
	private final static String ELETYPE_CURTAIN = "curtain_smartHome";
	//空气净化器
	private final static String ELETYPE_AIRCLEANER = "airCleaner_smartHome";
	//加湿器
	private final static String ELETYPE_HUMIDIFIER = "humidifier_smartHome";
	//冰箱
	private final static String ELETYPE_FREEZER = "freezer_smartHome";
	//取暖器
	private final static String ELETYPE_HEATER = "heater_smartHome";
	//窗口
	private final static String ELETYPE_WINDOW = "window_smartHome";
	//智能插座
	private final static String ELETYPE_SLOT = "slot_smartHome";
	//智能晾衣架
	private final static String ELETYPE_RACKS = "racks_smartHome";
	//还可以加入其它的家电
/*	private final static String KEY_DIRECT = "direct";
	private final static  String DIRECT_PLUS = "+";
	private final static  String DIRECT_MINUS = "-";*/
	
	//private ServiceType serviceType;
	private PlayController mPlayController = null;
	//操作类型
	public static enum OperationType {
		OPEN, CLOSE, SET, OTHER
	}

	static HashMap<String, OperationType> operationMap = new HashMap<String, OperationType>();

	static {
		operationMap.put("OPEN", OperationType.OPEN);
		operationMap.put("CLOSE", OperationType.CLOSE);
		operationMap.put("SET", OperationType.SET);
	}
	
	//电器类型
	public enum ElectricType {
		LIGHT,
		AIRCONTROL,
		TV,
		FAN,
		CURTAIN,
		AIRCLEANER,
		HUMIDIFIER,
		FREEZER,
		HEATER,
		WINDOW,
		SLOT,
		RACKS,
		OTHER
	};
	
	private static Map<String, ElectricType> electricMap;
	
	static {
		electricMap = new HashMap<String, ElectricType>();
		
		electricMap.put(ELETYPE_LIGHT, ElectricType.LIGHT);
		electricMap.put(ELETYPE_AIRCONTROL, ElectricType.AIRCONTROL);
		electricMap.put(ELETYPE_TV, ElectricType.TV);
		electricMap.put(ELETYPE_FAN, ElectricType.FAN);
		electricMap.put(ELETYPE_CURTAIN, ElectricType.CURTAIN);
		electricMap.put(ELETYPE_AIRCLEANER, ElectricType.AIRCLEANER);
		electricMap.put(ELETYPE_HUMIDIFIER, ElectricType.HUMIDIFIER);
		electricMap.put(ELETYPE_FREEZER, ElectricType.FREEZER);
		electricMap.put(ELETYPE_HEATER, ElectricType.HEATER);
		electricMap.put(ELETYPE_WINDOW, ElectricType.WINDOW);
		electricMap.put(ELETYPE_SLOT, ElectricType.SLOT);
		electricMap.put(ELETYPE_RACKS, ElectricType.RACKS);
	}

	//private String operation;
	//保存各个电器管理器的对象集合,缓存机制，类似与Integer中缓存了-128~127
	private Map<ElectricType, ElectricManager> mSubElectricManager = new HashMap<>();

	public SmartHomeDisposer(Context context) {
		super(context);
		mPlayController = getPlayController();
		try {
			//mSubElectricManager.put(ElectricType.LIGHT, new LightManager(context, mPlayController));
			mSubElectricManager.put(ElectricType.AIRCONTROL, new AircontrolManager(context, mPlayController));
			mSubElectricManager.put(ElectricType.TV, new TVManager(context, mPlayController));
			//mSubElectricManager.put(ElectricType.RACKS, new RacksManager(context, mPlayController));
			//mSubElectricManager.put(ElectricType.WINDOW, new WindowManager(context, mPlayController));
			//mSubElectricManager.put(ElectricType.CURTAIN, new CurtainManager(context, mPlayController));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 获取操作类型
	 * @param operation
	 * @return
	 */
	private OperationType getOperationType(String operation) {
		if (operation == null) {
			return OperationType.OTHER;
		}
		OperationType type = operationMap.get(operation);
		if (null == type) {
			type = OperationType.OTHER;
		}
		return type;
	}
	/**
	 * 获取电器类型
	 * @param str
	 * @return
	 */
	private ElectricType getElectricType(String str){
		if(str == null){
			return ElectricType.OTHER;
		}
		ElectricType type = electricMap.get(str);
		if(null == type){
			type = ElectricType.OTHER;
		}
		return type;
	}
	/**
	 * 智能家居的具体控制
	 */
	@Override
	public void  disposeResult(SemanticResult result) {
		if (null != result) {
			try{
				JSONObject json = result.getJson();
				//获取操作码
				String operation = result.getOperation();
				OperationType optype = getOperationType(operation);
				String answer = null;
				
				if (optype == OperationType.SET) {
					// 判断控制类型,如灯、空调、风扇、窗帘...
					String serviceType = json.getString(KEY_SERVICE);
					JSONObject semantic = json.getJSONObject(KEY_SEMANTIC);
					
					if (json.has(KEY_ANSWER)) {
						answer = json.getJSONObject(KEY_ANSWER).getString("text");
					}
					
					ElectricType type = getElectricType(serviceType);
					if (type != null) {
						LogHelper.d(type.toString());
						//ElectricManager manager = ElectricManager.build(type, semantic, answer, mPlayController);
						ElectricManager manager = mSubElectricManager.get(type);
						if (manager != null) {
							manager.mControl(semantic, answer);
						}
					}
					
				}
			}catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean canHandle(ServiceType type){
		if(type == ServiceType.SMARTHOME
				|| type == ServiceType.DISHORDER ){
			return true;
		}else{
			return false;
		}
	}
	
	/*private void processFreezerSmartHome(OperationType operation, String uuid, JSONObject extraData){
		switch (operation) {
		case CLOSE:
			getPlayController().playText(uuid, "已为您关闭冰箱");
			break;
		case OPEN:
			getPlayController().playText(uuid, "已为您打开冰箱");
			break;
		case SET:
			try {
				JSONObject semantic = extraData.getJSONObject(KEY_SEMANTIC);
				JSONObject slots = semantic.getJSONObject(KEY_SLOTS);
				String temperatureZone = slots.optString("temperatureZone","");
				String attr = slots.getString("attr");
				String attrType = slots.getString("attrType");
				String text = null;
				String ATTR_VALUE_KEY = "attrValue";
				if(attr.equals("温度")){
					if(attrType.equals("Integer")){
						int temperature = slots.getInt(ATTR_VALUE_KEY);
						text = "准备把您冰箱" + temperatureZone +"的温度调到" + temperature + "度";
					}else if(attrType.equals("String")){
						String val = slots.getString(ATTR_VALUE_KEY);
						if(val.equals("MAX")){
							text = "准备把您冰箱" +temperatureZone + "的温度调节到最高温度";
						}else if(val.equals("MIN")){
							text = "准备把您冰箱" +temperatureZone + "的温度调节到最低温度";
						}
					}else {
						JSONObject temp = slots.getJSONObject(ATTR_VALUE_KEY);
						int offset = temp.getInt("offset");
						String direct = temp.getString(KEY_DIRECT);
						
						if (DIRECT_MINUS.equals(direct)) {
							text = "准备把您冰箱" + temperatureZone +"的温度调低" + offset + "度";
						}else if (DIRECT_PLUS.equals(direct)) {
							text = "准备把您冰箱" + temperatureZone +"的温度调高" + offset + "度";
						}
					}
				}else if (attr.equals("开关")) {
					String val = slots.getString(ATTR_VALUE_KEY);
					if(val.equals("开")){
						text = "准备开启您冰箱" + temperatureZone;
					}else if(val.equals("关")){
						text = "准备关闭您冰箱" + temperatureZone;
					}
				} else {
					String val = slots.getString(ATTR_VALUE_KEY);
					if(val.equals("开")) {
						text = "准备将您冰箱" + temperatureZone + "开启" + attr;
					}else{
						text = "准备关闭您冰箱" + temperatureZone + "的" + attr;
					}
					
					if(!attr.equals("解冻")){
						text += "模式";
					}
				}
				
				int duration = 0;
				if((duration = slots.optInt("duration", 0)) != 0) {
					text = duration + "分钟后" + text;
				}
				
				if(slots.has("datetime")){
					String timeOrig = slots.getJSONObject("datetime").getString("timeOrig");
					text = timeOrig + text;
				}
				
				
				if(text != null){
					getPlayController().playText(uuid, text, true, null);
				}
				
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		default:
			break;
		}
	}*/
}
