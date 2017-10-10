package com.lamost.webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lamost.ir.db.DBProfile;
import com.lamost.ir.db.ETDB;
import com.lamost.ir.model.ETAirLocal;
import com.lamost.ir.model.ETKeyLocal;

/**
 * Created by Jia on 2016/4/6.
 */
public class WebService {

	// 命名空间
	private final static String SERVICE_NS = "http://ws.smarthome.zfznjj.com/";
	// EndPoint
	// private final static String SERVICE_URL =
	// "http://192.168.2.106:8080/zfzn02/services/smarthome?wsdl=SmarthomeWs.wsdl";

	// 阿里云
	private final static String SERVICE_URL = "http://101.201.211.87:8080/zfzn02/services/smarthome?wsdl=SmarthomeWs.wsdl";
	
	// SOAP Action
	private static String soapAction = "";
	// 调用的方法名称
	private static String methodName = "";
	//private HttpTransportSE ht;
	/*private SoapSerializationEnvelope envelope;
	private SoapObject soapObject;
	private SoapObject result;*/

	// private ArrayList<ElectricForVoice> list = null;

	public WebService() {
		/*ht = new HttpTransportSE(SERVICE_URL); // ①
		ht.debug = true;*/
	}

	/**
	 * 根据主机编号，搜索该主机下的电器设备信息
	 * 
	 * @param masterCode
	 */
	public List<ElectricForVoice> selectElectricForVoice(String masterCode,
			Context context) {
		// HttpTransportSE ht = new HttpTransportSE(SERVICE_URL) ;
		/*ht = new HttpTransportSE(SERVICE_URL);
		ht.debug = true;*/
		HttpTransportSE ht = new HttpTransportSE(SERVICE_URL);
		ht.debug = true;
		SoapSerializationEnvelope envelope;
		SoapObject soapObject;
		SoapObject result;

		String methodName = "selectElectricForVoice";
		String soapAction = SERVICE_NS + methodName;// 通常为命名空间 + 调用的方法名称

		// 使用SOAP1.1协议创建Envelop对象
		envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); // ②
		// 实例化SoapObject对象
		soapObject = new SoapObject(SERVICE_NS, methodName); // ③
		// 将soapObject对象设置为 SoapSerializationEnvelope对象的传出SOAP消息
		envelope.bodyOut = soapObject; // ⑤
		envelope.dotNet = true;
		envelope.setOutputSoapObject(soapObject);
		soapObject.addProperty("masterCode", masterCode);
		try {
			System.out.println("$$$$$$$$$$$$$$$$$$$$$");
			ht.call(soapAction, envelope);
			System.out.println("##############################");
			if (envelope.getResponse() != null) {
				// 获取服务器响应返回的SOAP消息
				result = (SoapObject) envelope.bodyIn; // ⑦
				// 解析数据
				// ArrayList<ElectricForVoice> list = new
				// ArrayList<ElectricForVoice>();
				List<ElectricForVoice> list = new ArrayList<ElectricForVoice>();

				for (int i = 0; i < result.getPropertyCount(); i++) {
					ElectricForVoice electricForVoice = new ElectricForVoice();
					SoapObject obj = (SoapObject) result.getProperty(i);
					electricForVoice.setElectricCode(obj.getProperty(
							"electricCode").toString());
					// electricForVoice.setElectricType(Integer.parseInt(electricForVoice.getElectricCode().substring(0,
					// 2)));
					electricForVoice.setElectricName(obj.getProperty(
							"electricName").toString());
					electricForVoice.setRoomName(obj.getProperty("roomName")
							.toString());
					electricForVoice.setOrderInfo(obj.getProperty("orderInfo")
							.toString());
					electricForVoice.setElectricIndex(Integer.parseInt(obj
							.getProperty("electricIndex").toString()));
					electricForVoice.setElectricType(Integer.parseInt(obj
							.getProperty("electricType").toString()));
					
					if (electricForVoice.getElectricCode().startsWith("09")) {//红外
						loadKeyFromWs(masterCode, electricForVoice.getElectricIndex(), context);
						if (electricForVoice.getElectricType() == 9) {//空调
							loadETAirByElectric(masterCode, electricForVoice.getElectricIndex(), context);
						}
					}
					
					list.add(electricForVoice);
					System.out.println(electricForVoice);
				}
				return list;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} finally {
			//resetParam();
		}
		return null;
	}

	/**
	 * 根据主机编号，查询该主机下的情景模式信息
	 * 
	 * @param masterCode
	 * @return
	 */
	public List<SceneDataInfo> masterReadScene(String masterCode) {
		HttpTransportSE ht = new HttpTransportSE(SERVICE_URL);
		ht.debug = true;
		SoapSerializationEnvelope envelope;
		SoapObject soapObject;
		SoapObject result;

		String methodName = "masterReadScene";
		String soapAction = SERVICE_NS + methodName;// 通常为命名空间 + 调用的方法名称

		// 使用SOAP1.1协议创建Envelop对象
		envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); // ②
		// 实例化SoapObject对象
		soapObject = new SoapObject(SERVICE_NS, methodName); // ③
		// 将soapObject对象设置为 SoapSerializationEnvelope对象的传出SOAP消息
		envelope.bodyOut = soapObject; // ⑤
		envelope.dotNet = true;
		envelope.setOutputSoapObject(soapObject);
		soapObject.addProperty("masterCode", masterCode);

		try {
			ht.call(soapAction, envelope);
			// 根据测试发现，运行这行代码时有时会抛出空指针异常，使用加了一句进行处理
			if (envelope != null && envelope.getResponse() != null) {
				// 获取服务器响应返回的SOAP消息
				result = (SoapObject) envelope.bodyIn; // ⑦
				// 接下来就是从SoapObject对象中解析响应数据的过程了
				// Map<String, SceneDataInfo> sceneMap = new HashMap<>();
				List<SceneDataInfo> sceneList = new ArrayList<>();

				for (int i = 0; i < result.getPropertyCount(); i++) {
					SceneDataInfo mSceneDataInfo = new SceneDataInfo();
					SoapObject obj = (SoapObject) result.getProperty(i);

					String sceneIndex = obj.getProperty("sceneIndex")
							.toString();
					String sceneName = obj.getProperty("sceneName").toString();
					mSceneDataInfo.setSceneIndex(sceneIndex);
					mSceneDataInfo.setSceneName(sceneName);
					// sceneMap.put(sceneName, mSceneDataInfo);
					sceneList.add(mSceneDataInfo);
				}

				return sceneList;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} finally {
			//resetParam();
		}
		return null;
	}

	/*private void resetParam() {
		envelope = null;
		soapObject = null;
		result = null;
	}*/

	public void loadKeyFromWs(String masterCode, int electricIndex,
			Context context) {
		HttpTransportSE ht = new HttpTransportSE(SERVICE_URL);
		ht.debug = true;
		SoapSerializationEnvelope envelope;
		SoapObject soapObject;
		SoapObject result;

		methodName = "loadKeyByElectric";
		soapAction = SERVICE_NS + methodName;

		// 使用SOAP1.1协议创建Envelop对象
		envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); // ②
		// 实例化SoapObject对象
		soapObject = new SoapObject(SERVICE_NS, methodName); // ③
		// 将soapObject对象设置为 SoapSerializationEnvelope对象的传出SOAP消息
		envelope.bodyOut = soapObject; // ⑤
		envelope.dotNet = true;
		envelope.setOutputSoapObject(soapObject);
		soapObject.addProperty("masterCode", masterCode);
		soapObject.addProperty("electricIndex", electricIndex);
		try {
			System.out.println("$$$$$$$$$$$$$$$$$$$$$");
			ht.call(soapAction, envelope);
			System.out.println("##############################");
			if (envelope.getResponse() != null) {
				// 获取服务器响应返回的SOAP消息
				result = (SoapObject) envelope.bodyIn; // ⑦
				String str = result.getProperty(0).toString();
				List<ETKeyLocal> list = new Gson().fromJson(str,
						new TypeToken<List<ETKeyLocal>>() {
						}.getType());
				saveOrUpdate(list, context);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} finally {
			// resetParam();
			envelope = null;
			soapObject = null;
			result = null;
		}
	}

	public void loadETAirByElectric(String masterCode, int electricIndex,
			Context context) {
		HttpTransportSE ht = new HttpTransportSE(SERVICE_URL);
		ht.debug = true;
		SoapSerializationEnvelope envelope;
		SoapObject soapObject;
		SoapObject result;

		methodName = "loadETAirByElectric";
		soapAction = SERVICE_NS + methodName;

		// 使用SOAP1.1协议创建Envelop对象
		envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); // ②
		// 实例化SoapObject对象
		soapObject = new SoapObject(SERVICE_NS, methodName); // ③
		// 将soapObject对象设置为 SoapSerializationEnvelope对象的传出SOAP消息
		envelope.bodyOut = soapObject; // ⑤
		envelope.dotNet = true;
		envelope.setOutputSoapObject(soapObject);
		soapObject.addProperty("masterCode", masterCode);
		soapObject.addProperty("electricIndex", electricIndex);
		try {
			System.out.println("$$$$$$$$$$$$$$$$$$$$$");
			ht.call(soapAction, envelope);
			System.out.println("##############################");
			if (envelope.getResponse() != null) {
				// 获取服务器响应返回的SOAP消息
				result = (SoapObject) envelope.bodyIn; // ⑦
				String str = result.getProperty(0).toString();
				ETAirLocal etAir = new Gson().fromJson(str, ETAirLocal.class);
				// 一个电器对应一条记录
				saveOrUpdateETAir(etAir, context);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} finally {
			// resetParam();
			envelope = null;
			soapObject = null;
			result = null;
		}
	}

	private void saveOrUpdateETAir(ETAirLocal etAir, Context context) {
		ETDB db = ETDB.getInstance(context);
		String sql = "SELECT * FROM ETAirDevice WHERE master_code = ? and electric_index = ?";
		ContentValues contentValues = new ContentValues();
		Cursor cursor = null;
		try {
			cursor = db.queryData2Cursor(
					sql,
					new String[] { etAir.getMasterCode(),
							String.valueOf(etAir.getElectricIndex()) });
			if (cursor.getCount() > 0) {
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_BRAND,
						etAir.getAirBrand());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_INDEX,
						etAir.getAirIndex());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_TEMP,
						etAir.getAirTemp());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_RATE,
						etAir.getAirRate());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_DIR,
						etAir.getAirDir());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_AUTO_DIR,
						etAir.getAirAutoDir());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_MODE,
						etAir.getAirMode());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_POWER,
						etAir.getAirPower());
				String where = "master_code = ? and electric_index = ?";
				db.updataData(
						DBProfile.AIRDEVICE_TABLE_NAME,
						contentValues,
						where,
						new String[] { etAir.getMasterCode(),
								String.valueOf(etAir.getElectricIndex()) });
			} else {
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_MASTERCODE,
						etAir.getMasterCode());
				contentValues.put(
						DBProfile.TABLE_AIRDEVICE_FIELD_ELECTRICINDEX,
						etAir.getElectricIndex());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_BRAND,
						etAir.getAirBrand());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_INDEX,
						etAir.getAirIndex());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_TEMP,
						etAir.getAirTemp());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_RATE,
						etAir.getAirRate());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_DIR,
						etAir.getAirDir());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_AUTO_DIR,
						etAir.getAirAutoDir());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_MODE,
						etAir.getAirMode());
				contentValues.put(DBProfile.TABLE_AIRDEVICE_FIELD_POWER,
						etAir.getAirPower());
				db.insertData(DBProfile.AIRDEVICE_TABLE_NAME, contentValues);
			}
			contentValues.clear();

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
			db.close();
		}
	}

	private void saveOrUpdate(List<ETKeyLocal> list, Context context) {
		ETDB db = ETDB.getInstance(context);
		String sql = "SELECT * FROM ETKEY WHERE master_code = ? and electric_index = ? and key_key = ?";
		ContentValues contentValues = new ContentValues();
		Cursor cursor = null;
		for (ETKeyLocal keyLocal : list) {
			try {
				cursor = db.queryData2Cursor(
						sql,
						new String[] { keyLocal.getMasterCode(),
								String.valueOf(keyLocal.getElectricIndex()),
								String.valueOf(keyLocal.getKeyKey()) });
				if (cursor.getCount() > 0) {
					// 如果已经存在就更新
					contentValues.put("key_value", keyLocal.getKeyValue());
					String where = "master_code = ? and electric_index = ? and key_key = ?";
					db.updataData(
							DBProfile.KEY_TABLE_NAME,
							contentValues,
							where,
							new String[] {
									keyLocal.getMasterCode(),
									String.valueOf(keyLocal.getElectricIndex()),
									String.valueOf(keyLocal.getKeyKey()) });
				} else {
					// 否则就保存
					contentValues.put(DBProfile.TABLE_KEY_FIELD_MASTER_CODE,
							keyLocal.getMasterCode());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_ELECTRIC_INDEX,
							keyLocal.getElectricIndex());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_DEVICE_ID,
							keyLocal.getDid());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_NAME,
							keyLocal.getKeyName());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_RES,
							keyLocal.getKeyRes());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_X,
							keyLocal.getKeyX());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_Y,
							keyLocal.getKeyY());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_KEYVALUE,
							keyLocal.getKeyValue());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_KEY,
							keyLocal.getKeyKey());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_BRANDINDEX,
							keyLocal.getKeyBrandIndex());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_BRANDPOS,
							keyLocal.getKeyBrandPos());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_ROW,
							keyLocal.getKeyRow());
					contentValues.put(DBProfile.TABLE_KEY_FIELD_STATE,
							keyLocal.getKeyState());
					db.insertData(DBProfile.KEY_TABLE_NAME, contentValues);
				}
				contentValues.clear();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
			db.close();
		}

	}

}
