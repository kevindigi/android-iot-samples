package com.digicorp.androidiotsamples.impact_measurement.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by kevin.adesara on 01/08/17.
 */

public class SensorData implements Parcelable {
    public static final Creator<SensorData> CREATOR = new Creator<SensorData>() {
        @Override
        public SensorData createFromParcel(Parcel in) {
            return new SensorData(in);
        }

        @Override
        public SensorData[] newArray(int size) {
            return new SensorData[size];
        }
    };
    private String sensorAddress;
    private String sensorName;
    private String hexScanRecord;
    private byte[] scanRecord;
    private float x;
    private float y;
    private float z;

    public SensorData(@NonNull String sensorAddress, @NonNull String sensorName, @NonNull String hexScanRecord, @NonNull byte[] scanRecord) {
        this.sensorAddress = sensorAddress;
        this.sensorName = sensorName;
        this.hexScanRecord = hexScanRecord;
        this.scanRecord = scanRecord;
    }

    protected SensorData(Parcel in) {
        sensorAddress = in.readString();
        sensorName = in.readString();
        hexScanRecord = in.readString();
        scanRecord = in.createByteArray();
        x = in.readFloat();
        y = in.readFloat();
        z = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sensorAddress);
        dest.writeString(sensorName);
        dest.writeString(hexScanRecord);
        dest.writeByteArray(scanRecord);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeFloat(z);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getSensorAddress() {
        return sensorAddress;
    }

    public void setSensorAddress(@NonNull String sensorAddress) {
        this.sensorAddress = sensorAddress;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(@NonNull byte[] scanRecord) {
        this.scanRecord = scanRecord;
    }

    public String getHexScanRecord() {
        return hexScanRecord;
    }

    public void setHexScanRecord(@NonNull String hexScanRecord) {
        this.hexScanRecord = hexScanRecord;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(@NonNull String sensorName) {
        this.sensorName = sensorName;
    }
}
