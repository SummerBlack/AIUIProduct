package com.iflytek.aiuiproduct.handler.disposer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import com.iflytek.aiuiproduct.handler.entity.SemanticResult;
import com.iflytek.aiuiproduct.handler.entity.ServiceType;

public class CookDisposer extends Disposer {

	public CookDisposer(Context context) {
		super(context);
	}

	@Override
	public void disposeResult(SemanticResult result) {
		// TODO Auto-generated method stub
		JSONArray results = null;
		JSONObject cookbook = null;
		try {
			results = result.getJson().getJSONObject("data").getJSONArray("result");
			cookbook = (JSONObject) results.get(0);//获取菜谱步骤，默认取结果中的第一个
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringBuilder builder = new StringBuilder();
		if (cookbook != null){
			builder = new StringBuilder();
			
			if (cookbook.has("title")) {
				//获取菜名
				try {
					String title = cookbook.getString("title");
					builder.append(title);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (cookbook.has("ingredient")) {
				//获取主要食材
				try {
					String ingredient = cookbook.getString("ingredient");
					builder.append("主要食材为"+ingredient+"。");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (cookbook.has("accessory")) {
				//获取辅助食材
				try {
					String accessory = cookbook.getString("accessory");
					builder.append("辅助食材为"+accessory+"。");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (cookbook.has("steps")) {
				//制作步骤
				try {
					String steps = cookbook.getString("steps");
					builder.append("制作步骤如下。"+steps);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		String cookBookText = builder.toString();
		getPlayController().playText(result.getUUID(), cookBookText, true, "", null);

		
	}


	@Override
	public boolean canHandle(ServiceType type) {
		// TODO Auto-generated method stub
		if(type == ServiceType.COOKBOOK){
			return true;
		}else{
			return false;
		}
	}

}
