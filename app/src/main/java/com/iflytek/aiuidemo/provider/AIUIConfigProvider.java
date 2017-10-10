package com.iflytek.aiuidemo.provider;


import com.iflytek.aiuiproduct.utils.ConfigUtil;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * AIUI配置Provider。
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年6月12日 上午8:58:54 
 *
 */
public class AIUIConfigProvider extends ContentProvider {
	private static final String TAG = "AIUIConfigProvider";
	
	private static final String METHOD_READAIUICFG = "readAIUICfg";
	
	@Override 
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
	/*APP A通过Provider的方式向其他应用如APP B提供数据，
	如果APP B需要调用APP A中的方法，则Provider必须重写call方法，
	APP B通过调用call方法会执行APP A中Provider的call方法，
	B中调用call方法的返回值就是Provider中call方法的返回值。*/
	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		Log.e(TAG, "Bundle call(String method, String arg, Bundle extras)");
		if (METHOD_READAIUICFG.equals(method)) {
			String config = readAIUICfg();
			
			Bundle bundle = new Bundle();
			bundle.putString("config", config);
			
			return bundle;
		}
		
		return super.call(method, arg, extras);
	}
	
	private String readAIUICfg() {
		/*注意：1.请将从开放平台获取的appid、key（login参数中）以及scene（global参数中）信息填入到aiui.cfg中的相应位置；
			 2.在安装此demo之前，请先将AIUIProductDemo、ControlService从开发板上删除，二者的包名分别为：
			 	com.iflytek.aiuiproduct.demo
			 	com.iflytek.aiui.devboard.controlservice*/
		
		String config = ConfigUtil.readAssetsCfg(getContext(), "cfg/aiui.cfg");
		
		return config;
	}
	
}
