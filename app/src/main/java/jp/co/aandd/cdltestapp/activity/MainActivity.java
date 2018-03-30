package jp.co.aandd.cdltestapp.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.UUID;

import jp.co.aandd.cdltestapp.R;
import jp.co.aandd.cdltestapp.ble.BleDeviceAdapter;
import jp.co.aandd.cdltestapp.ble.BleDeviceItem;
import jp.co.aandd.cdltestapp.ble.BleReceivedService;


public class MainActivity extends Activity {

    private final String TAG = "CDLTestApp:MainActivity";

    private LinkedHashMap<String, BleDeviceItem> deviceInfoMap;

    private BleDeviceAdapter bleDeviceAdapter;
    private BluetoothManager bluetoothManager;
    private boolean isStartedScan;
    private boolean mIsBleReceiver = false;
    private boolean mIsBindBleReceivedServivce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        disabledButton();
        initDeviceInfoMap();
        initListView();
        initUserSwitch();
        doStartService();
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);


        //Add check CDL version.
        Toast.makeText(this, "Ver.1108", Toast.LENGTH_LONG).show();

        //Add request permission for Android 6.0 or later.
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);

            if (permissionCheck == -1) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, 0);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        enabledButton();
        doBindBleReceivedService();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void initDeviceInfoMap() {
        deviceInfoMap = new LinkedHashMap<>();
        deviceInfoMap.clear();
    }

    private String deviceName;
    private String mailAddress;
    private void initListView() {
        ListView deviceListView = (ListView) findViewById(R.id.listview1);
        bleDeviceAdapter = new BleDeviceAdapter(this, deviceInfoMap);

        deviceListView.setAdapter(bleDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                view.setEnabled(false);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setEnabled(true);
                    }
                }, 300);

                BleDeviceItem item = (BleDeviceItem) bleDeviceAdapter.getItem(position);


                bluetoothManager.getAdapter().stopLeScan(leScanCallback);
                isStartedScan = false;
                Button button = (Button) findViewById(R.id.startButton);
                button.setText(getResources().getString(R.string.button_start));

                BluetoothDevice device = item.getDevice();
                deviceName = device.getName();

                bleDeviceAdapter.clearList();
                bleDeviceAdapter.notifyDataSetChanged();

                BleReceivedService.getInstance().connectDevice(device);
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        doStopLeScan();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("Sim","Connecting device");
                        BleReceivedService.getInstance().connectDevice(device);
                    }
                }); */

            }
        });
    }


    private void initUserSwitch() {
        Switch mySwitch = (Switch) findViewById(R.id.dummyUserSwitch);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    UUID randomUUID = UUID.randomUUID();
                    ((Switch)findViewById(R.id.dummyUserSwitch)).setText(randomUUID.toString() + "@debug.co.jp");
                } else {
                    ((Switch)findViewById(R.id.dummyUserSwitch)).setText("testtest@debug.co.jp");
                }
            }
        });
        mySwitch.setText("testtest@debug.co.jp");
    }

    private void enabledButton() {
        Button button = (Button) findViewById(R.id.startButton);
        button.setEnabled(true);
        button.setOnClickListener(onClickListener);

    }

    private void disabledButton() {
        Button button = (Button) findViewById(R.id.startButton);
        button.setEnabled(false);
        button.setOnClickListener(null);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            v.setEnabled(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    v.setEnabled(true);
                }
            }, 200);
            if (!isStartedScan) {
//                bluetoothManager.getAdapter().startLeScan(new UUID[]{AndCustomService.ServiceUuid},leScanCallback);
                bluetoothManager.getAdapter().startLeScan(leScanCallback);
                isStartedScan = true;

                Button button = (Button) findViewById(R.id.startButton);
                button.setText(getResources().getString(R.string.button_stop));
            }
            else {
                bluetoothManager.getAdapter().stopLeScan(leScanCallback);
                isStartedScan = false;
                Button button = (Button) findViewById(R.id.startButton);
                button.setText(getResources().getString(R.string.button_start));
                bleDeviceAdapter.clearList();
                bleDeviceAdapter.notifyDataSetChanged();
            }
        }
    };

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            final String deviceMacAddress = device.getAddress();
            final int deviceRssi = rssi;
            final BluetoothDevice bleDevice = device;

            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    BleDeviceItem item = new BleDeviceItem();
                    item.setDevice(bleDevice);
                    item.setRssi(deviceRssi);

                    deviceInfoMap.put(deviceMacAddress, item);

                    bleDeviceAdapter.refreshKeys();
                    bleDeviceAdapter.notifyDataSetChanged();

                }
            });
        }

    };


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

        }
    };

    public static void readCharacteristicCoolDesign(BluetoothGattCharacteristic characteristic) {

        Bundle bundle = new Bundle();
        int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

        String flagString = Integer.toBinaryString(flag);
        int offset=0;
        for(int index = flagString.length(); 0 < index ; index--) {
            String key = flagString.substring(index-1 , index);

            if(index == flagString.length()) {
                if(key.equals("0")) {
                    // mmHg
                    Log.d("CDL-Unit", "mmHg");

                }
                else {
                    // kPa
                    Log.d("CDL-Unit", "kPa");

                }
                // Unit
                offset+=1;
                Log.d("SN", "Systolic :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                offset+=2;
                Log.d("SN", "Diastolic :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                offset+=2;
                Log.d("SN", "Mean Arterial Pressure :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                offset+=2;
            }
            else if(index == flagString.length()-1) {
                if(key.equals("1")) {
                    // Time Stamp
                    Log.d("SN", "Year :"+String.format("%04d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)));
                    offset+=2;
                    Log.d("SN", "Month :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                    offset+=1;
                    Log.d("SN", "Day :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                    offset+=1;

                    Log.d("SN", "Hour :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                    offset+=1;
                    Log.d("SN", "Min :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                    offset+=1;
                    Log.d("SN", "Sec :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                    offset+=1;
                }
                else {
                    // 日時が存在しない場合、現在日時を格納する。
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());

                }
            }
            else if(index == flagString.length()-2) {
                if(key.equals("1")) {
                    // Pulse Rate
                    Log.d("SN", "Pulse Rate :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                    offset+=2;
                }
            }
            else if(index == flagString.length()-3) {
                // UserID
            }
            else if(index == flagString.length()-4) {
                // Measurement Status Flag
                int statusFalg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                String statusFlagString = Integer.toBinaryString(statusFalg);
                for(int i = statusFlagString.length(); 0 < i ; i--) {
                    String status = statusFlagString.substring(i-1 , i);
                    if(i == statusFlagString.length()) {
                        int body_mov = (status.endsWith("1"))? 1 : 0;
                        Log.d("SN","KEY_BODY_MOVEMENT_DETECTION" + body_mov );

                    }
                    else if(i == statusFlagString.length() - 1) {
                        int cuff_fit = (status.endsWith("1"))? 1 : 0;
                        Log.d("SN","KEY_CUFF_FIT_DETECTION" + cuff_fit );

                    }
                    else if(i == statusFlagString.length() - 2) {
                        int irregular_pul = (status.endsWith("1"))? 1 : 0;
                        Log.d("SN","KEY_IRREGULAR_PULSE_DETECTION" + irregular_pul );

                    }
                    else if(i == statusFlagString.length() - 3) {
                        i--;
                        String secondStatus = statusFlagString.substring(i-1 , i);
                        if(status.endsWith("1") && secondStatus.endsWith("0")) {
                            int pulser_rate_range_detection = 1;

                        }
                        else if(status.endsWith("0") && secondStatus.endsWith("1")) {
                            int pulse_rate_range_detection = 2;

                        }
                        else if(status.endsWith("1") && secondStatus.endsWith("1")) {
                            int pulse_rate_range_detection = 3;
                        }
                        else {
                            int pulse_rate_range_detection = 0;
                        }
                    }
                    else if(i == statusFlagString.length() - 5) {
                        int measurement_position = (status.endsWith("1"))? 1 : 0;
                        Log.d("SN","KEY_MEASUREMENT_POSITION_DETECTION" + measurement_position );

                    }
                }
            }
        }
        //Sim writing the device ID;


    }

    private void doStopLeScan() {
        if (isStartedScan) {
            stopScan();
        }

    }

    private void stopScan() {
        if (BleReceivedService.getInstance() != null) {
            isStartedScan = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (BleReceivedService.getInstance().getBluetoothManager().getAdapter() != null) {
                        BleReceivedService.getInstance().getBluetoothManager().getAdapter().stopLeScan(leScanCallback);
                         Toast.makeText(getApplicationContext(), "StopLeScan", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void doStartService() {
        Intent intent1 = new Intent(this, BleReceivedService.class);
        startService(intent1);
        if (!mIsBleReceiver) {
            IntentFilter filter = new IntentFilter(BleReceivedService.ACTION_BLE_SERVICE);
            registerReceiver(bleServiceReceiver, filter);
            mIsBleReceiver = true;
        }

    }
    private void doStopService() {
        if (mIsBleReceiver) {
            unregisterReceiver(bleServiceReceiver);
            mIsBleReceiver = false;
        }

        Intent intent1 = new Intent(this, BleReceivedService.class);
        stopService(intent1);
    }
    private void doBindBleReceivedService(){
        if (!mIsBindBleReceivedServivce) {
            bindService(new Intent(MainActivity.this,
                    BleReceivedService.class), mBleReceivedServiceConnection, Context.BIND_AUTO_CREATE);
            mIsBindBleReceivedServivce = true;
        }
    }

    private void doUnbindBleReceivedService() {
        if (mIsBindBleReceivedServivce) {
            unbindService(mBleReceivedServiceConnection);
            mIsBindBleReceivedServivce = false;
        }
    }

    private ServiceConnection mBleReceivedServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            doStartLeScan();
        }
    };

    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private void doStartLeScan() {
        isStartedScan = true;
        doTryLeScan();
    }

    private void doTryLeScan() {

        if (!isStartedScan) {
            startScan();
        }

    }

    private void startScan() {

        if (BleReceivedService.getInstance() != null) {
            if (BleReceivedService.getInstance().isConnectedDevice()) {
                BleReceivedService.getInstance().disconnectDevice();
            }
            isStartedScan = true;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean result = BleReceivedService.getInstance().getBluetoothManager().getAdapter().startLeScan(leScanCallback);
                    Log.d(TAG,"Dashboard startLeScan is " + result);
                     Toast.makeText(getApplicationContext(), "StartLeScan", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}


