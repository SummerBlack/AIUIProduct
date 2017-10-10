package com.lamost.ir.model;

/**
 * Created by Administrator on 2017/3/29.
 */
public class ETAirLocal {
    private int id;
    private String masterCode;
    private int electricIndex;
    private int airBrand;
    private int airIndex;
    private int airTemp;
    private int airRate;
    private int airDir;
    private int airAutoDir;
    private int airMode;
    private int airPower;

    public ETAirLocal() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAirPower() {
        return airPower;
    }

    public void setAirPower(int airPower) {
        this.airPower = airPower;
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

    public int getAirBrand() {
        return airBrand;
    }

    public void setAirBrand(int airBrand) {
        this.airBrand = airBrand;
    }

    public int getAirIndex() {
        return airIndex;
    }

    public void setAirIndex(int airIndex) {
        this.airIndex = airIndex;
    }

    public int getAirTemp() {
        return airTemp;
    }

    public void setAirTemp(int airTemp) {
        this.airTemp = airTemp;
    }

    public int getAirRate() {
        return airRate;
    }

    public void setAirRate(int airRate) {
        this.airRate = airRate;
    }

    public int getAirDir() {
        return airDir;
    }

    public void setAirDir(int airDir) {
        this.airDir = airDir;
    }

    public int getAirAutoDir() {
        return airAutoDir;
    }

    public void setAirAutoDir(int airAutoDir) {
        this.airAutoDir = airAutoDir;
    }

    public int getAirMode() {
        return airMode;
    }

    public void setAirMode(int airMode) {
        this.airMode = airMode;
    }

    @Override
    public String toString() {
        return "ETAirLocal{" +
                "id=" + id +
                ", masterCode='" + masterCode + '\'' +
                ", electricIndex=" + electricIndex +
                ", airBrand=" + airBrand +
                ", airIndex=" + airIndex +
                ", airTemp=" + airTemp +
                ", airRate=" + airRate +
                ", airDir=" + airDir +
                ", airAutoDir=" + airAutoDir +
                ", airMode=" + airMode +
                ", airPower=" + airPower +
                '}';
    }
}
