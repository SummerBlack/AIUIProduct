package com.iflytek.aiuiproduct.handler.entity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 语义结果抽象类。
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年6月29日 上午10:48:28
 *
 */
public class SemanticResult {
	public final static String KEY_SERVICE = "service";
	public final static String KEY_TEXT = "text";
	public final static String KEY_ANSWER = "answer";
	public final static String KEY_HISTORY = "history";
	public final static String KEY_PROMPT = "prompt";
	public final static String KEY_DATA = "data";
	public final static String KEY_RESULT = "result";
	public final static String KEY_DIALOG_STAT = "dialog_stat";
	public final static String KEY_SEMANTIC = "semantic";
	public final static String KEY_SLOTS = "slots";
	public final static String KEY_CONTENT = "content";
	public final static String KEY_OPERATION = "operation";
	public final static String KEY_SID = "sid";
	public final static String KEY_UUID = "uuid";
	public final static String KEY_RC = "rc";

	protected String sid;
	protected String uuid;
	//服务类型
	private ServiceType service;
	private String operation;
	private String answerText = "";
	private JSONObject json = null;

	public SemanticResult(String service, JSONObject json) {
		this.service = ServiceType.getServiceType(service);
		this.json = json;
		
		try {
			this.sid = json.getString(KEY_SID);
			this.uuid = json.getString(KEY_UUID);
			this.operation = json.getString(KEY_OPERATION);
			this.answerText = json.has(KEY_ANSWER)?
					json.getJSONObject(KEY_ANSWER).optString(KEY_TEXT):"";
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public ServiceType getServiceType() {
		return service;
	}

	public String getAnswerText() {
		return answerText;
	}

	public JSONObject getJson() {
		return json;
	}
	
	public String getSid() {
		return sid;
	}
	
	public String getUUID() {
		return uuid;
	}
	
	public String getOperation(){
		return operation;
	}
	
}
