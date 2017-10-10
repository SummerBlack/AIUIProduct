package com.iflytek.aiuiproduct.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

/**
 * 配置读取类。
 * 
 * @author <a href="http://www.xfyun.cn">讯飞开放平台</a>
 * @date 2016年7月22日 下午2:13:00 
 *
 */
public class ConfigUtil {
	private static final String CFG_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() 
													+ "/AIUIProductDemo.properties";
	
	private static final String KEY_SAVE_APP_TIME_LOG = "save_app_time_log";
	
	private static boolean saveAppTimeLog = false;
	
	static {
		readCfg();
	}
	
	private static void readCfg() {
		File cfgFile = new File(CFG_FILE);
		if (!cfgFile.exists()) {
			return;
		}
		
		try {
			FileInputStream ins = new FileInputStream(cfgFile);
			Properties p = new Properties();
			p.load(ins);
			
			String saveAppTimeLogStr = p.getProperty(KEY_SAVE_APP_TIME_LOG);
			if ("1".equals(saveAppTimeLogStr)) {
				saveAppTimeLog = true;
			}
			
			ins.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isSaveAppTimeLog() {
		return saveAppTimeLog;
	}
	/**
	 * 从assets目录读取配置文件。
	 * 
	 * @param cfgFilePath 文件路径
	 * @return 配置内容
	 */
	public static String readAssetsCfg(Context context, String cfgFilePath) {
		String content = "";
		
		AssetManager assetManager = context.getResources().getAssets();
		try {
			InputStream ins = assetManager.open(cfgFilePath);
			byte[] buffer = new byte[ins.available()];
			
			ins.read(buffer);
			ins.close();
			
			content = new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return content;
	}
	
}
