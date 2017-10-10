package com.lamost.ir.model;

/**
 * Created by Administrator on 2017/3/29.
 */
public class ETKeyLocal {
    private int id;
    private String masterCode;
    private int electricIndex;
    private int did;
    private String keyName;
    private int keyRes;
    private float keyX;
    private float keyY;
    private String keyValue;
    private int keyKey;
    private int keyBrandIndex;
    private int keyBrandPos;
    private int keyRow;
    private int keyState;

    public ETKeyLocal() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMasterCode() {
        return masterCode;
    }

    public void setMasterCode(String masterCode) {
        this.masterCode = masterCode;
    }

    public int getElectricIndex() {
        return electricIndex;
    }

    public void setElectricIndex(int electricIndex) {
        this.electricIndex = electricIndex;
    }

    public int getDid() {
        return did;
    }

    public void setDid(int did) {
        this.did = did;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public int getKeyRes() {
        return keyRes;
    }

    public void setKeyRes(int keyRes) {
        this.keyRes = keyRes;
    }

    public float getKeyX() {
        return keyX;
    }

    public void setKeyX(float keyX) {
        this.keyX = keyX;
    }

    public float getKeyY() {
        return keyY;
    }

    public void setKeyY(float keyY) {
        this.keyY = keyY;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public int getKeyKey() {
        return keyKey;
    }

    public void setKeyKey(int keyKey) {
        this.keyKey = keyKey;
    }

    public int getKeyBrandIndex() {
        return keyBrandIndex;
    }

    public void setKeyBrandIndex(int keyBrandIndex) {
        this.keyBrandIndex = keyBrandIndex;
    }

    public int getKeyBrandPos() {
        return keyBrandPos;
    }

    public void setKeyBrandPos(int keyBrandPos) {
        this.keyBrandPos = keyBrandPos;
    }

    public int getKeyRow() {
        return keyRow;
    }

    public void setKeyRow(int keyRow) {
        this.keyRow = keyRow;
    }

    public int getKeyState() {
        return keyState;
    }

    public void setKeyState(int keyState) {
        this.keyState = keyState;
    }

    @Override
    public String toString() {
        return "ETKeyLocal{" +
                "id=" + id +
                ", masterCode='" + masterCode + '\'' +
                ", electricIndex=" + electricIndex +
                ", did=" + did +
                ", keyName='" + keyName + '\'' +
                ", keyRes=" + keyRes +
                ", keyX=" + keyX +
                ", keyY=" + keyY +
                ", keyValue='" + keyValue + '\'' +
                ", keyKey=" + keyKey +
                ", keyBrandIndex=" + keyBrandIndex +
                ", keyBrandPos=" + keyBrandPos +
                ", keyRow=" + keyRow +
                ", keyState=" + keyState +
                '}';
    }
}
