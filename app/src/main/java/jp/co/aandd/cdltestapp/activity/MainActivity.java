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
    private String operation;
    Button pairButton, dataButton;


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

        //Declare the pair or connect button
        pairButton = (Button) findViewById(R.id.PairButton);
        pairButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.d("AD","User has clicked on the pair button");
                operation = "pair";
                BleReceivedService.getInstance().operation = "pair";
                enabledButton();

            }
        });

        dataButton = (Button) findViewById(R.id.getData);
        dataButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.d("AD","User has clicked on the data button");
                operation = "data";
                BleReceivedService.getInstance().operation = "data";
                enabledButton();

            }
        });
        pairButton.setVisibility(View.VISIBLE);
        dataButton.setVisibility(View.VISIBLE);


    }

    @Override
    protected void onResume() {
        super.onResume();
        disabledButton();
        pairButton.setVisibility(View.VISIBLE);
        dataButton.setVisibility(View.VISIBLE);
        //enabledButton();
        doBindBleReceivedService();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(broadcastReceiver); //Register this receiver later
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
                if (operation.equalsIgnoreCase("pair")) {
                    Log.d("AD","case of pair operation");
                    BleReceivedService.getInstance().connectDevice(device);
                } else if (operation.equalsIgnoreCase("data")) {
                    final BluetoothDevice bdevice = device;
                    Log.d("AD","case of data operation");
                      runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        doStopLeScan();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("Sim","Connecting device");
                        BleReceivedService.getInstance().connectDevice(bdevice);
                    }
                });

                }


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
                if (operation == null) {
                    Toast.makeText(getApplicationContext(), "Please click on the Pair or Take Reading button", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Log.d("AD","The value of operation is " + operation);
                    pairButton.setVisibility(View.GONE);
                    dataButton.setVisibility(View.GONE);
                }


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
            Log.d("AD","Entered the onLeScan function");
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


