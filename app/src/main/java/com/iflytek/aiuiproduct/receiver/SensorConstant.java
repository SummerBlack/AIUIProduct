package com.iflytek.aiuiproduct.receiver;

import java.util.HashMap;
import java.util.Map;

/**
 * @author C.Feng
 * @version 
 * @date 2017-6-19 下午9:27:37
 * 
 */
public class SensorConstant {
	public static Map<String, String> sensorMap = new HashMap<>();
	static{
		sensorMap.put("0D41", "燃气传感器");
		sensorMap.put("0D73", "雨滴传感器");
	}

}
