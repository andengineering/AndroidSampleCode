package jp.co.aandd.cdltestapp.ble;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedHashMap;

import jp.co.aandd.cdltestapp.R;

/**
 * Created by minamisono on 2015/11/10.
 */
public class BleDeviceAdapter extends BaseAdapter {

    private LinkedHashMap<String, BleDeviceItem> mData = new LinkedHashMap<String, BleDeviceItem>();
    private String[] mKeys;
    LayoutInflater mLayoutInflater = null;
    public BleDeviceAdapter(Context context, LinkedHashMap<String, BleDeviceItem> data){
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mData  = data;
        mKeys = mData.keySet().toArray(new String[data.size()]);
    }

    public void refreshKeys() {
        mKeys = mData.keySet().toArray(new String[mData.size()]);
    }

    public void clearList() {
        mData.clear();
    }


    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(mKeys[position]);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(R.layout.device_list, parent,false);
        if (mKeys.length > 0) {
            String key = mKeys[position];
            String name = ((BleDeviceItem)getItem(position)).getDevice().getName();
            int rssi = ((BleDeviceItem)getItem(position)).getRssi();

            TextView macAddressTextView = (TextView)view.findViewById(R.id.macAddress);
            macAddressTextView.setText("MacAddress: " +key);
            TextView nameTextView = (TextView)view.findViewById(R.id.name);
            nameTextView.setText("Name: " + name);
            TextView rssiTextView = (TextView)view.findViewById(R.id.rssi);
            rssiTextView.setText("Rssi: " + String.valueOf(rssi));
        }
        return view;
    }
}