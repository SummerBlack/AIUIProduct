package com.lamost.smarthomeresult;

import java.net.SocketException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.iflytek.aiuiproduct.player.PlayController;
import com.lamost.connection.AIUICommunication;
import com.lamost.connection.ActionListener;

public abstract class ElectricManager {
	
	private final static String TAG = "ElectricManager";
	//解析Json语句时，需要用到的KEY
	private final static String KEY_SLOTS = "slots";
	private final static String KEY_LOCALTION = "location";
	private final static String KEY_ROOM = "room";
	private static final String KEY_ZONE = "zone";
	private static final String KEY_ATTR = "attr";
	private static final String KEY_ATTRTYPE = "attrType";
	private static final String KEY_ATTRVALUE = "attrValue";
	private static final String KEY_DIRECT = "direct";
	private static final String KEY_OFFSET = "offset";
	private static final String KEY_MODIFIER = "modifier";
	
	protected JSONObject semantic = null;
	protected String answer = null;
	protected AIUICommunication mAIUICommunication = null;
	protected PlayController mPlayController = null;
	
	protected String mZone;//电器所属区域如：一楼、二楼、地下室、阁楼...
	protected String mRoom;//电器所属房间，如：客厅、卧室、书房、厨房、阳台、 主客、  餐厅、次卧、 淋浴间、更衣 室、厕所、储物间...
	protected String mAttr;//属性名称，如开关、亮度、模式等，不同电器属性有所区别
	protected String mAttrType;//属性取值类型
	protected String mModifier;//设备修饰语义，如设备的品牌
	protected String mAttrValue;//属性取值，如开、关...
	protected String mAttrValue_direct;//高低方向（+，-）
	protected int mAttrValue_offset = 1;//偏差多少（如高2度）,默认为1
	
	protected String mCode;//电器编码，即某一电器的唯一识别编号
	protected String mElectricType;//电器种类，在给定的电器列表中的种类编号
	protected int mAddress;//电器的网络短地址
	protected String mIndex;//电器编号，第多少个电器
	
	protected ElectricManager(Context context, PlayController mPlayController) throws SocketException {
		this.mAIUICommunication = AIUICommunication.getInstance();
		this.mPlayController = mPlayController;
	}
	
	/**
	 * 语义中并不是一定有zone
	 * @throws JSONException
	 */
	protected void setmZone() throws JSONException {
		if(semantic.has(KEY_SLOTS)){
			JSONObject slots = semantic.getJSONObject(KEY_SLOTS);
			if(slots.has(KEY_LOCALTION)){
				JSONObject location = slots.getJSONObject(KEY_LOCALTION);
				if(location.has(KEY_ZONE)){
					this.mZone= location.getString(KEY_ZONE);//有可能没有这个
				}
			}
		}
	}
	/**
	 * 从语义中解析房间
	 * 存在一个问题，就是如果第一次解析出房间信息，但第二次不包含房间信息，就默认为第一次的房间。即可以记住房间
	 * @throws JSONException
	 */
	protected void setmRoom() throws JSONException {
		if(semantic.has(KEY_SLOTS)){
			JSONObject slots = semantic.getJSONObject(KEY_SLOTS);
			if(slots.has(KEY_LOCALTION)){
				JSONObject location = slots.getJSONObject(KEY_LOCALTION);
				if(location.has(KEY_ROOM)){
					this.mRoom= location.getString(KEY_ROOM);//有可能没有这个
					Log.e(TAG, "房间为："+ mRoom);
				}
			}
		}	
	}
	
	/**
	 * 解析出attr属性名称，
	 * @throws JSONException
	 */
	protected void setmAttr() throws JSONException {
		if(semantic.has(KEY_SLOTS)){
			this.mAttr = semantic.getJSONObject(KEY_SLOTS).getString(KEY_ATTR);
			Log.e(TAG, "属性为："+ mAttr);
		}
	}
	/**
	 * 解析出attr取值类型
	 * @return
	 * @throws JSONException
	 */
	protected String setmAttrType() throws JSONException {	
		if(semantic.has(KEY_SLOTS)){
			this.mAttrType = semantic.getJSONObject(KEY_SLOTS).getString(KEY_ATTRTYPE);
			return this.mAttrType;
		}
		return null;
	}
	/**
	 *  解析出attr属性的取值，之前要先调用setmAttrType()
	 * @throws JSONException
	 */
	protected void setmAttrValue() throws JSONException {
		if(semantic.has(KEY_SLOTS)){
			if("String".equals(mAttrType)){
				this.mAttrValue = semantic.getJSONObject(KEY_SLOTS).getString(KEY_ATTRVALUE);
				Log.e(TAG, "属性值为："+ mAttrValue);
			} else if ("Integer".equals(mAttrType)) {
				this.mAttrValue = semantic.getJSONObject(KEY_SLOTS).getInt(KEY_ATTRVALUE) + "";
				Log.e(TAG, "属性值为："+ mAttrValue);
			} else if ("Object(digital)".equals(mAttrType)) {
				this.mAttrValue_direct = semantic.getJSONObject(KEY_SLOTS).getJSONObject(KEY_ATTRVALUE).getString(KEY_DIRECT);
				this.mAttrValue_offset = semantic.getJSONObject(KEY_SLOTS).getJSONObject(KEY_ATTRVALUE).getInt(KEY_OFFSET);
				Log.e(TAG, "属性值为："+ mAttrValue_direct + mAttrValue_offset);
			}
		}
	}
	/**
	 * 解析出modifier属性，主要指设备的品牌，描述等
	 * @throws JSONException
	 */
	protected void setmModifier() throws JSONException {
		if (semantic.has(KEY_SLOTS)) {
			JSONObject slots = semantic.getJSONObject(KEY_SLOTS);
			if (slots.has(KEY_MODIFIER)) {
				this.mModifier = slots.getString(KEY_MODIFIER);
			}
		}
	}
	
	//控制电器
	public abstract void mControl(JSONObject semantic, String answer) throws JSONException;
	
	/**
	 * 向家庭网关发送指令
	 * @param cmd 发送的指令
	 * @param tts 发送完要播报的
	 */
	protected final void sendCommond(final String cmd,final String tts){
		mAIUICommunication.sendCommandToHost(cmd, new ActionListener() {
			
			@Override
			public void onSuccess() {
				mPlayController.playText("", tts, true, "",null);
			}
			
			@Override
			public void onFailed(int errorCode) {
				if(errorCode == AIUICommunication.NO_HOST){
					mPlayController.playText("", "未搜索到家庭网关，请先搜索");
				}else if(errorCode == AIUICommunication.NO_ACK){
					//mPlayController.playText("", "通信异常");
				}
			}
		});

	}
	
	public String getmZone() {
		return mZone;
	}


	public void setmZone(String mZone) {
		this.mZone = mZone;
	}


	public String getmRoom() {
		return mRoom;
	}

	public String getmAttr() {
		return mAttr;
	}

	public String getmAttrValue() {
		return mAttrValue;
	}
	
	public String getmModifier() {
		return mModifier;
	}

	public String getmCode() {
		return mCode;
	}


	public void setmCode(String mCode) {
		this.mCode = mCode;
	}


	public String getmElectricType() {
		return mElectricType;
	}


	public void setmElectricType(String string) {
		this.mElectricType = string;
	}


	public String getmIndex() {
		return mIndex;
	}
	
	public void setmIndex(String mIndex) {
		this.mIndex = mIndex;
	}
  
}