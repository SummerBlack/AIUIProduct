package com.iflytek.aiuiproduct.handler.entity;

import java.util.HashMap;

public enum ServiceType {
	WEATHER,				// 天气 查询        
	TRAIN, 					// 火车查询			
	FLIGHT, 				// 航班查询
	MUSICX, 				// 音乐播放&控制
	NUMBER_MASTER, 			// 数字纠错
	CMD, 
	CHAT,                   // 闲聊
	SMARTHOME, 				// 智能家居
	TELEPHONE,				// 电话号码
	COOKBOOK,				// 菜谱
	STORY,					// 讲故事
	RADIO,					// 广播，电台查询
	JOKE,					// 讲笑话
	NEWS,					// 新闻
	PM25,					// PM2.5
	DATETIME,				// 日期查询
	CALC,					// 数值计算
	POETRY,					// 对诗
	DISHORDER,
	OTHER;					// 其他
	
	
	static HashMap<String, ServiceType> serviceMap = new HashMap<String, ServiceType>();

	static {
		serviceMap.put("weather", WEATHER);
		serviceMap.put("train", TRAIN);
		serviceMap.put("flight", FLIGHT);
		serviceMap.put("musicX", MUSICX);
		serviceMap.put("numberMaster", NUMBER_MASTER);
		serviceMap.put("cmd", CMD);
		serviceMap.put("chat", ServiceType.CHAT);
		serviceMap.put("smartHome", SMARTHOME);
		serviceMap.put("telephone", TELEPHONE);
		serviceMap.put("cookbook", ServiceType.COOKBOOK);
		serviceMap.put("story", STORY);
		serviceMap.put("radio", RADIO);
		serviceMap.put("joke", JOKE);
		serviceMap.put("news", NEWS);
		serviceMap.put("pm25", PM25);
		serviceMap.put("datetime", DATETIME);
		serviceMap.put("calc", CALC);
		serviceMap.put("poetry", ServiceType.POETRY);
		serviceMap.put("dishOrder", DISHORDER);
	}
	
	/**
	 * 根据传入的字符串service判断服务类型
	 * @param service
	 * @return
	 */
	public static ServiceType getServiceType(String service) {
		ServiceType type = serviceMap.get(service);
		//如果在上述服务类型中找不到
		if (null == type) {
			type = ServiceType.OTHER;
		}
		//在智能家居中的服务ID有不同种类.如：light_smartHome，airControl_smartHome，只要有"smartHome"，就是智能家居控制服务
		if (-1 != service.lastIndexOf("smartHome")) {
			return ServiceType.SMARTHOME;
		}
		
		return type;
	}

}