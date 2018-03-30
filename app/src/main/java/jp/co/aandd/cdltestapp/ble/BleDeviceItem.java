package jp.co.aandd.cdltestapp.ble;

import android.bluetooth.BluetoothDevice;

/**
 * Created by minamisono on 2015/11/10.
 */
public class BleDeviceItem {
    private BluetoothDevice mDevice;
    private int mRssi;


    public void setDevice(BluetoothDevice device) {
        mDevice = device;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public int getRssi() {
        return mRssi;
    }
}