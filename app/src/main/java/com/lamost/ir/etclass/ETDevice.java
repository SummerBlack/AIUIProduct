package com.lamost.ir.etclass;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import com.lamost.ir.db.DBProfile;
import com.lamost.ir.db.ETDB;
import com.lamost.ir.face.IOp;
import et.song.device.DeviceType;
import et.song.jni.ir.ETIR;
import et.song.tool.ETTool;

public class ETDevice implements IOp {
	//private DataControl mDC;
	private int m20 = 0xff;
	private int m08 = 0xff;
	private int m10 = 0xff;
	private int mID;
	private int mGID;
	private String mName;
	private int mType;
	private int mResId;
	private int mBrand;
	private int mIndex;
	private String mMasterCode;
	private String mElectricCode;
	private int mElectricIndex;
	private String mRoomName;
	private int mRoomIndex;
	private int mRoomSequ;
	private List<ETKey> mKeyList;
	private List<ETKeyEx> mKeyExList;
	public ETDevice() {
		//mDC = DataControl.getInstance();
		mKeyList = new ArrayList<ETKey>();
		mKeyExList = new ArrayList<ETKeyEx>();
	}

	public static ETDevice Builder(int type) {

		switch (type) {
			case DeviceType.DEVICE_REMOTE_AIR:
				return new ETDeviceAIR();
			case DeviceType.DEVICE_REMOTE_TV:
				return new ETDeviceTV();
		}
		return null;
	}

	public int getmBrand() {
		return mBrand;
	}

	public void setmBrand(int mBrand) {
		this.mBrand = mBrand;
	}

	public int getmIndex() {
		return mIndex;
	}

	public void setmIndex(int mIndex) {
		this.mIndex = mIndex;
	}

	private void Set20(int value) {
		m20 = value;
	}

	private int Get20() {
		return m20;
	}

	private void Set08(int value) {
		m08 = value;
	}

	private int Get08() {
		return m08;
	}

	private void Set10(int value) {
		m10 = value;
	}

	private int Get10() {
		return m10;
	}

	protected byte[] Study(byte[] key) {
		byte[] keyBuf = new byte[key.length + 2];
		for (int i = 0; i < key.length; i++) {
			keyBuf[i] = key[i];
		}
		return ETIR.StudyCode(keyBuf, keyBuf.length);
	}

	protected byte[] Work(byte[] key) {
		if (key[2] == 0x04) {
			if (Get20() == 0xFF) {
				Set20(key[5]);
			} else {
				int value = Get20();
				value ^= 0x20;
				Set20(value);
				key[5] = (byte) Get20();
			}
		} else if (key[2] == 0x0a) {
			if (Get08() == 0xFF) {
				Set08(key[5]);
			} else {
				int value = Get08();
				value ^= 0x08;
				Set08(value);
				key[5] = (byte) Get08();
			}
		} else if (key[2] == 0x21) {
			if (Get10() == 0xFF) {
				Set10(key[5]);
			} else {
				int value = Get10();
				value ^= 0x10;
				Set10(value);
				key[5] = (byte) Get10();
			}
		}
		key[9] = (byte) (key[0] + key[1] + key[2] + key[3] + key[4] + key[5]
				+ key[6] + key[7] + key[8]);
		Set20(0xFF);
		Set08(0xFF);
		Set10(0xFF);
		return key;
	}

	public void SetID(int id) {
		mID = id;
	}

	public int GetID() {
		return mID;
	}

	public void SetGID(int id) {
		mGID = id;
	}

	public int GetGID() {
		return mGID;
	}

	public void SetName(String name) {
		mName = name;
	}

	public String GetName() {
		return mName;
	}

	public void SetType(int type) {
		mType = type;
	}

	public int GetType() {
		return mType;
	}

	public void SetRes(int resId) {
		mResId = resId;
	}

	public int GetRes() {
		return mResId;
	}

	public String getmMasterCode() {
		return mMasterCode;
	}

	public void setmMasterCode(String mMasterCode) {
		this.mMasterCode = mMasterCode;
	}

	public String getmElectricCode() {
		return mElectricCode;
	}

	public void setmElectricCode(String mElectricCode) {
		this.mElectricCode = mElectricCode;
	}

	public int getmElectricIndex() {
		return mElectricIndex;
	}

	public void setmElectricIndex(int mElectricIndex) {
		this.mElectricIndex = mElectricIndex;
	}
	
	public String getmRoomName() {
		return mRoomName;
	}

	public void setmRoomName(String mRoomName) {
		this.mRoomName = mRoomName;
	}

	public int getmRoomIndex() {
		return mRoomIndex;
	}

	public void setmRoomIndex(int mRoomIndex) {
		this.mRoomIndex = mRoomIndex;
	}

	public int getmRoomSequ() {
		return mRoomSequ;
	}

	public void setmRoomSequ(int mRoomSequ) {
		this.mRoomSequ = mRoomSequ;
	}

	public void SetKey(ETKey key) {
		mKeyList.add(key);
	}
	public void SetKeyEx(ETKeyEx key) {
		mKeyExList.add(key);
	}

	public ETKey GetKeyByIndex(int index) {
		return mKeyList.get(index);
	}
	public ETKeyEx GetKeyByIndexEx(int index) {
		return mKeyExList.get(index);
	}
	public ETKey GetKeyByValue(int keyValue) {
		for (ETKey key : mKeyList) {
			if (key.GetKey() == keyValue) {
				return key;
			}
		}
		return null;
	}
	public ETKeyEx GetKeyByValueEx(int keyValue) {
		for (ETKeyEx key : mKeyExList) {
			if (key.GetKey() == keyValue) {
				return key;
			}
		}
		return null;
	}
	public byte[] GetKeyValue(int value) throws Exception {
		ETKey key = GetKeyByValue(value);
		if (key == null) {
			return null;
		}
		if (key.GetValue() == null){
			return null;
		}
		if (key.GetState() == ETKey.ETKEY_STATE_STUDY){
			return Study(key.GetValue());
		}
		if (key.GetState() == ETKey.ETKEY_STATE_TYPE){
			return Work(key.GetValue());
		}
		if (key.GetState() == ETKey.ETKEY_STATE_KNOWN){
			return Work(key.GetValue());
		}
		return null;
	}
	
	public byte[] GetKeyValueEx(int value) throws Exception {
		ETKeyEx key = GetKeyByValueEx(value);
		if (key == null){
			return null;
		}
		if (key.GetValue() == null){
			return null;
		}
		return Study(key.GetValue());
	}

	/**
	 * 到ETKEY表中查询mastercode和ElectricIndex对应的电器，并获得该电器各个控制按键对应的各个按键信息保存到mKeyList中
	 * @param db
     */
	@Override
	public void Load(ETDB db) {
		// TODO Auto-generated method stub
		mKeyList.clear();
		try {
			String sql ="select * from "
					+ DBProfile.KEY_TABLE_NAME + " where "
					+ DBProfile.TABLE_KEY_FIELD_MASTER_CODE+ " = '" + mMasterCode + "' AND "
					+ DBProfile.TABLE_KEY_FIELD_ELECTRIC_INDEX+ " = " + mElectricIndex;
			System.out.println(sql);
			Cursor c = db.queryData2Cursor("select * from "
					+ DBProfile.KEY_TABLE_NAME + " where "
					+ DBProfile.TABLE_KEY_FIELD_MASTER_CODE+ " = '" + mMasterCode + "' AND "
					+ DBProfile.TABLE_KEY_FIELD_ELECTRIC_INDEX+ " = " + mElectricIndex, null);
			System.out.println();
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				ETKey key = new ETKey();

				int id = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_ID));
				key.SetId(id);
				String masterCode = c.getString(c.getColumnIndex(DBProfile.TABLE_KEY_FIELD_MASTER_CODE));
				key.setmMasterCode(masterCode);
				int electricIndex = c.getInt(c.getColumnIndex(DBProfile.TABLE_KEY_FIELD_ELECTRIC_INDEX));
				key.setmElectricIndex(electricIndex);
				int did = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_DEVICE_ID));
				key.SetDID(did);
				String name = c.getString(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_NAME));
				key.SetName(name);
				int state = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_STATE));
				key.SetState(state);
				int res = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_RES));
				key.SetRes(res);
				int row = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_ROW));
				key.SetRow(row);
				int brandIndex = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_BRANDINDEX));
				key.SetBrandIndex(brandIndex);
				int brandPos = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_BRANDPOS));
				key.SetBrandPos(brandPos);
				int k = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_KEY));
				key.SetKey(k);
				byte[] keyValue = ETTool.HexStringToBytes(c.getString(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_KEYVALUE)));
				key.SetValue(keyValue);
				float x = c.getFloat(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_X));
				key.SetX(x);
				float y = c.getFloat(c
						.getColumnIndex(DBProfile.TABLE_KEY_FIELD_Y));
				key.SetY(y);
				mKeyList.add(key);
			}
			c.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mKeyExList.clear();
		
		try {
			Cursor c = db.queryData2Cursor("select * from "
					+ DBProfile.KEYEX_TABLE_NAME + " where "
					+ DBProfile.TABLE_KEYEX_FIELD_MASTER_CODE + " = '" + mMasterCode+ "' AND "
					+ DBProfile.TABLE_KEYEX_FIELD_ELECTRIC_INDEX + " = " + mElectricIndex, null);

			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				ETKeyEx key = new ETKeyEx();

				int id = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_ID));
				key.SetId(id);
				String masterCode = c.getString(c.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_MASTER_CODE));
				key.setmMasterCode(masterCode);
				int electricIndex = c.getInt(c.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_ELECTRIC_INDEX));
				key.setmElectricIndex(electricIndex);
				int did = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_DEVICE_ID));
				key.SetDID(did);
				String name = c.getString(c
						.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_NAME));
				key.SetName(name);
				int k = c.getInt(c
						.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_KEY));
				key.SetKey(k);
				byte[] keyValue = ETTool.HexStringToBytes(c.getString(c
						.getColumnIndex(DBProfile.TABLE_KEYEX_FIELD_KEYVALUE)));
				key.SetValue(keyValue);
				mKeyExList.add(key);
			}
			c.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void Update(ETDB db) {
		// TODO Auto-generated method stub
		ContentValues deviceValue = new ContentValues();
		deviceValue.put(DBProfile.TABLE_DEVICE_FIELD_NAME, GetName());
		try {
			db.updataData(DBProfile.DEVICE_TABLE_NAME, deviceValue,
					DBProfile.TABLE_DEVICE_FIELD_ID + " = ?",
					new String[] { String.valueOf(mID) });
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int GetCount() {
		// TODO Auto-generated method stub
		return mKeyList.size() ;
	}

	@Override
	public Object GetItem(int i) {
		// TODO Auto-generated method stub
		return mKeyList.get(i);
	}

	@Override
	public void Delete(ETDB db) {
		// TODO Auto-generated method stub
		try {
			db.deleteData(DBProfile.WATCHTV_TABLE_NAME,
					DBProfile.TABLE_WATCHTV_FIELD_DEVICE_ID + " = ?",
					new String[] { String.valueOf(mID) });
			
			db.deleteData(DBProfile.KEYEX_TABLE_NAME,
					DBProfile.TABLE_KEYEX_FIELD_DEVICE_ID + " = ?",
					new String[] { String.valueOf(mID) });
			db.deleteData(DBProfile.KEY_TABLE_NAME,
					DBProfile.TABLE_KEY_FIELD_DEVICE_ID + " = ?",
					new String[] { String.valueOf(mID) });
			db.deleteData(DBProfile.DEVICE_TABLE_NAME,
					DBProfile.TABLE_DEVICE_FIELD_ID + " = ?",
					new String[] { String.valueOf(mID) });
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void Inster(ETDB db) {
		// TODO Auto-generated method stub
		
		try {
//			ContentValues deviceValue = new ContentValues();
//			deviceValue.put("master_code",getmMasterCode());
//			deviceValue.put("electric_code", getmElectricCode());
//			deviceValue.put(DBProfile.TABLE_DEVICE_FIELD_GROUP_ID,
//					this.GetGID());
//			deviceValue.put(DBProfile.TABLE_DEVICE_FIELD_NAME, this.GetName());
//			deviceValue.put(DBProfile.TABLE_DEVICE_FIELD_RES, this.GetRes());
//			deviceValue.put(DBProfile.TABLE_DEVICE_FIELD_TYPE, this.GetType());
//			db.insertData(DBProfile.DEVICE_TABLE_NAME, deviceValue);

			/*new Thread(){
				@Override
				public void run() {
					mDC.mElectricData.addElectric(GetType(), getmElectricCode(), GetName(), getmRoomIndex(), getmRoomSequ(), null, null);
				}
			}.start();*/


//			Cursor cursor = db.queryData2Cursor("SELECT count(*) FROM "
//					+ DBProfile.DEVICE_TABLE_NAME + " order by "
//					+ DBProfile.TABLE_DEVICE_FIELD_ID + " desc ", null);
//			cursor.moveToFirst();
//			long count = cursor.getLong(0);
//			cursor.close();
//			if (count == 0)
//				return;
//
//			Cursor c = db.queryData2Cursor("SELECT * FROM "
//					+ DBProfile.DEVICE_TABLE_NAME + " order by "
//					+ DBProfile.TABLE_DEVICE_FIELD_ID + " desc ", null);
//			c.moveToFirst();
//			int col = c.getColumnIndex(DBProfile.TABLE_DEVICE_FIELD_ID);
//			int id = c
//					.getInt(col);
			
			for (int i = 0; i < mKeyList.size(); i++) {
				ETKey key = mKeyList.get(i);
				//key.SetDID(id);
				key.setmMasterCode(getmMasterCode());
				key.setmElectricIndex(getmElectricIndex());
				key.Inster(db);
			}
			
			for (int i = 0; i < mKeyExList.size(); i++) {
				ETKeyEx key = mKeyExList.get(i);
				//key.SetDID(id);
				key.setmMasterCode(getmMasterCode());
				key.setmElectricIndex(getmElectricIndex());
				key.Inster(db);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
