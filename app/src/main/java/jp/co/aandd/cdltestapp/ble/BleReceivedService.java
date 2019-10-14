package jp.co.aandd.cdltestapp.ble;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by sbhattacharya on 3/29/18.
 */

public class BleReceivedService extends Service {

        private static final String TAG = "SN";

        public static final String ACTION_BLE_SERVICE = "jp.co.aandd.andblelink.ble.BLE_SERVICE";
        public static final String ACTION_BLE_DATA_RECEIVED = "jp.co.aandd.andblelink.ble.data_received";
        private static BleReceivedService bleService;
        private BluetoothGatt bluetoothGatt;
        private boolean isConnectedDevice;
        private boolean isBindService;
        private Handler uiThreadHandler = new Handler();
        private long setDateTimeDelay = Long.MIN_VALUE;
        private long indicationDelay = Long.MIN_VALUE;
        public String operation;

        public static BleReceivedService getInstance() {
            return bleService;
        }

        public static BluetoothGatt getGatt() {
            if (bleService != null) {
                return bleService.bluetoothGatt;
            }
            return null;
        }

        private final IBinder binder = new BleReceivedBinder();
        public class BleReceivedBinder extends Binder {
            public BleReceivedService getService() {
                return bleService;
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            Log.d(TAG, "BleReceivedService onBind");
            isBindService = true;
            return binder;
        }

        @Override
        public boolean onUnbind(Intent intent) {
            Log.d(TAG, "BleReceivedService onUnbind");
            isBindService = false;
            return super.onUnbind(intent);
        }

        public BleReceivedService() {
            super();
        }

        @Override
        public void onCreate() {
            Log.d(TAG, "BleReceivedService onCreate");

            if (bleService == null) {
                bleService = this;
            }


        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "BleReceivedService onDestroy");
            if (bleService != null) {
                bleService = null;
            }
        }

        public boolean isConnectedDevice() {
            return isConnectedDevice;
        }

        public boolean isBindService() {
            return isBindService;
        }

        public BluetoothManager getBluetoothManager() {
            BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
            return bluetoothManager;
        }

        public boolean connectDevice(BluetoothDevice device) {
            final BluetoothDevice bdevice = device;
            Log.d(TAG, "connectDevice device name " + device.getName());
            if (device == null) {
                return false;
            }
            if (operation.equalsIgnoreCase("pair")) {
                Log.d("AD","The operation is pair");
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Log.d("Sim","Calling other devices for pairing");

                        bluetoothGatt = bdevice.connectGatt(getApplicationContext(), false, bluetoothGattCallback);
                    }
                }, 500);
            } else {
                Log.d("AD","Calling the connectGatt for data transfer");
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);

            }

            Log.d(TAG, "bluetoothGatt " + bluetoothGatt);
            if (bluetoothGatt == null) {
                return false;
            }
            return true;
        }

        public void test() {
            bluetoothGatt.connect();
        }

        public void disconnectDevice() {
            if(bluetoothGatt == null) {
                return;
            }
            bluetoothGatt.close();
            bluetoothGatt.disconnect();
            bluetoothGatt = null;
        }

        private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                BluetoothDevice device = gatt.getDevice();

                if(status != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("A&D","GATT connection not successful");
                }

                Log.d(TAG, "onConnectionStateChange()" + device.getAddress() + ", " + device.getName() + ", status=" + status + " newState=" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnectedDevice = true;
                    Log.d(TAG, "Device Address : "+bluetoothGatt.getDevice().getAddress());
                    Log.d(TAG, "Bond Status: "+bluetoothGatt.getDevice().getBondState());
                    //Connected now discover services
                    if (operation.equalsIgnoreCase("pair")) {
                        Log.d("AD","Calling service discovery for pairing");
                        if (BleReceivedService.getGatt() != null) {
                            BleReceivedService.getGatt().discoverServices();
                        }
                    } else if(operation.equalsIgnoreCase("data")) {
                        if (BleReceivedService.getGatt() != null) {
                            Log.d("AD","calling the discover services");
                            BleReceivedService.getGatt().discoverServices();
                        }


                         /*if (BleReceivedService.getGatt() != null) {
                            //BleReceivedService.getGatt().discoverServices();
                            uiThreadHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    if (BleReceivedService.getGatt() != null) {
                                        Log.d("AD","calling the discover services");
                                        BleReceivedService.getGatt().discoverServices();
                                    }

                                }
                            }, 2000);
                        }  */
                    }

                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnectedDevice = false;
                    //Clean up gatt
                    gatt.disconnect();
                    gatt.close();
                    gatt = null;
                    disconnectDevice();

                }

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onServicesDiscovered()" + device.getAddress() + ", " + device.getName() + ", status=" + status);

                if (operation.equalsIgnoreCase("pair")){
                    Log.d("AD","Case of pair hence set time");
                    setupDateTime(gatt);
                } else if (operation.equalsIgnoreCase("data")){
                    Log.d(TAG,"entered case of onservice discovered with data");

                  /*  boolean writeResult =setIndication(gatt, true);
                    if(writeResult == false) {
                        Log.d(TAG, "Write Error");

                    }*/

                   if (BleReceivedService.getInstance() != null) {
                        uiThreadHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("Sim","calling the readrequestfirmware version");
                                BleReceivedService.getInstance().requestReadFirmRevision();
                            }
                        }, 50L);
                    }
                }

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onCharacteristicRead()" + device.getAddress() + ", " + device.getName() + "characteristic=" + characteristic.getUuid().toString());
                byte[] firmRevisionBytes = characteristic.getValue();
                String firmRevision = null;
                if (firmRevisionBytes == null) {
                    return;
                }
                firmRevision = new String(firmRevisionBytes);
                if (firmRevision == null || firmRevision.isEmpty()) {
                    return;
                }
                Log.d(TAG ,"FirmRevision " + firmRevision);
               // String[] firmRevisionArray = getResources().getStringArray(R.array.firm_revision_group1);
                String[] firmRevisionArray = new String[] {"BLP008_d016","BLP008_d017","HTP008_134","CWSP008_108","CWSP008_110","WSP001_201"};
                boolean isGroup1 = false;
                for (String revision : firmRevisionArray) {
                    if (revision.contains(firmRevision)) {
                        isGroup1 = true;
                        break;
                    }
                }
                if (isGroup1) {
                    setDateTimeDelay = 40L;
                    indicationDelay = 40L;
                } else {
                    setDateTimeDelay = 100L;
                    indicationDelay = 100L;

                }
                uiThreadHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothGatt gatt = BleReceivedService.getGatt();
                        boolean settingResult = false;
                        if (gatt != null) {
                            Log.d(TAG,"calling the setUpdatetime operation");
                            settingResult = setupDateTime(gatt);

                        }
                            if (!settingResult) {
                                Log.d(TAG,"setDateTime " + settingResult);

                            }
                        }


                }, setDateTimeDelay);

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onCharacteristicWrite()" + device.getAddress() + ", " + device.getName() + "characteristic=" + characteristic.getUuid().toString());
                String serviceUuidString = characteristic.getService().getUuid().toString();
                String characteristicUuidString = characteristic.getUuid().toString();
                if (operation.equalsIgnoreCase("pair")){
                    Log.d("AD","OnCharacteristic write for pairing aftrer time is set");
                    disconnectDevice();
                } else if (operation.equalsIgnoreCase("data")) {
                    Log.d("AD","entering the condition of getting data");
                    if (serviceUuidString.equals(ADGattUUID.CurrentTimeService.toString())
                            || characteristicUuidString.equals(ADGattUUID.DateTime.toString()) ) {

                        uiThreadHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothGatt gatt = BleReceivedService.getGatt();
                                Log.d(TAG,"enabling the setIndication");
                                boolean writeResult =setIndication(gatt, true);
                                if(writeResult == false) {
                                    Log.d(TAG, "Write Error");

                                }
                            }
                        }, indicationDelay);

                    }
                }

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onCharacteristicChanged()" + device.getAddress() + ", " + device.getName() + "characteristic=" + characteristic.getUuid().toString());
                parseCharcteristicValue(gatt, characteristic);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onDescriptorRead()" + device.getAddress() + ", " + device.getName() + "characteristic=" + descriptor.getCharacteristic().getUuid().toString());
                BluetoothGattService gattService = getGattSearvice(gatt);
                if (gattService != null) {
                    if(operation.equalsIgnoreCase("pair")) {
                        //TODO: If there is a separate pairing screen, then issue a device disconnect here.
                        operation = null;
                        if (gatt != null) {
                            gatt.disconnect();
                            gatt.close();
                            gatt = null;
                        }
                        disconnectDevice();


                    } else {
                        //Nothing to do since its not a pairing case.
                    }

                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onDescriptorWrite()" + device.getAddress() + ", " + device.getName() + "characteristic=" + descriptor.getCharacteristic().getUuid().toString());


            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onReadRemoteRssi()" + device.getAddress() + ", " + device.getName() + "RSSI=" + rssi + "status=" + status);
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                BluetoothDevice device = gatt.getDevice();
                Log.d(TAG, "onReadRemoteRssi()" + device.getAddress() + ", " + device.getName() + "status=" + status);
            }
        };

        public BluetoothGattService getGattSearvice(BluetoothGatt gatt) {
            BluetoothGattService service = null;
            for(UUID uuid : ADGattUUID.ServicesUUIDs) {
                service = gatt.getService(uuid);
                if(service != null)break;
            }
            return service;
        }

        public BluetoothGattCharacteristic getGattMeasuCharacteristic(BluetoothGattService service) {
            BluetoothGattCharacteristic characteristic = null;
            for(UUID uuid : ADGattUUID.MeasuCharacUUIDs) {
                characteristic = service.getCharacteristic(uuid);
                if(characteristic != null)break;
            }
            Log.d("Sim","the characteristic found is " + characteristic.getUuid().toString());
            return characteristic;
        }

        public boolean setIndication(BluetoothGatt gatt, boolean enable) {
            boolean isSuccess = false;
            if (gatt != null) {
                BluetoothGattService service = BleReceivedService.getInstance().getGattSearvice(gatt);
                if(service != null) {
                    Log.d("AD","the service for which we are setting notification is " + service.getUuid().toString());
                    BluetoothGattCharacteristic characteristic = BleReceivedService.getInstance().getGattMeasuCharacteristic(service);
                    if(characteristic != null) {

                        isSuccess = gatt.setCharacteristicNotification(characteristic, enable);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(ADGattUUID.ClientCharacteristicConfiguration);
                        if(enable) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                        else {
                            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                    else {
                        Log.d(TAG, "Characteristic NULL");
                    }
                }
                else {
                    Log.d(TAG, "Service NULL");
                }
            }
            return isSuccess;
        }


        public void parseCharcteristicValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (ADGattUUID.AndCustomWeightScaleMeasurement.equals(characteristic.getUuid())) {
                Log.d("AD","reading for WS received");
                int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                String flagString = Integer.toBinaryString(flag);
                int offset=0;
                for(int index = flagString.length(); 0 < index ; index--) {
                    String key = flagString.substring(index-1 , index);
                    if(index == flagString.length()) {
                        double convertValue = 0;
                        if(key.equals("0")) {
                           convertValue = 0.1f;
                        }
                        else {
                            convertValue = 0.1f;
                        }
                        // Unit
                        offset+=1;

                        // Value
                        double value = (double)(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)) * convertValue;
                        Log.d("SN", "Weight Value :"+value);
                        offset+=2;
                    }
                    else if(index == flagString.length()-1) {
                        if(key.equals("1")) {
                            Log.d("SN", "Y :"+String.format("%04d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)));
                            offset+=2;
                            Log.d("SN", "M :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "D :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "H :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "M :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "S :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                        }
                        else {
                            //Implies put the calendar date as the one
                            Calendar calendar = Calendar.getInstance(Locale.getDefault());

                        }
                    }
                    else if(index == flagString.length()-2) {
                        if(key.equals("1")) {
                            Log.d("SN", "ID :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                        }
                    }
                    else if(index == flagString.length()-3) {
                        if(key.equals("1")) {
                            // BMI and Height
                        }
                    }
                }

            }


            else if (ADGattUUID.BloodPressureMeasurement.equals(characteristic.getUuid())) {
                Log.d("AD","reading for BP is received");
                int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                String flagString = Integer.toBinaryString(flag);
                String systolic = "";
                String diastolic = "";
                String pulse = "";
                String systolic_display = "";
                String diastolic_display = "";
                String pulse_display = "";

                int offset=0;
                for(int index = flagString.length(); 0 < index ; index--) {
                    String key = flagString.substring(index-1 , index);

                    if(index == flagString.length()) {
                        if(key.equals("0")) {
                            // mmHg
                            Log.d("SN", "mmHg");

                        }
                        else {
                            // kPa
                            Log.d("SN", "kPa");

                        }
                        // Unit
                        offset+=1;
                        Log.d("SN", "Systolic :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                        systolic = String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                        systolic_display = String.format("%.0f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                        offset+=2;

                        Log.d("SN", "Diastolic :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                        diastolic = String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                        diastolic_display = String.format("%.0f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                        offset+=2;

                        Log.d("SN", "Mean Arterial Pressure :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));

                        offset+=2;
                    }
                    else if(index == flagString.length()-1) {
                        if(key.equals("1")) {
                            // Time Stamp
                            Log.d("SN", "Y :"+String.format("%04d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)));
                            offset+=2;
                            Log.d("SN", "M :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "D :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "H :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "M :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "S :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                        }
                        else {

                            Calendar calendar = Calendar.getInstance(Locale.getDefault());
                            //Use calendar to get the date and time
                        }
                    }
                    else if(index == flagString.length()-2) {
                        if(key.equals("1")) {
                            // Pulse Rate
                            Log.d("SN", "Pulse Rate :"+String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                            pulse = String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                            pulse_display = String.format("%.0f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
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
                                                           }
                            else if(i == statusFlagString.length() - 1) {
                            }
                            else if(i == statusFlagString.length() - 2) {
                                                           }
                            else if(i == statusFlagString.length() - 3) {
                                i--;
                                String secondStatus = statusFlagString.substring(i-1 , i);
                                if(status.endsWith("1") && secondStatus.endsWith("0")) {
                                    Log.d("AD","Pulse range detection is 1");

                                }
                                else if(status.endsWith("0") && secondStatus.endsWith("1")) {
                                    Log.d("AD","Pulse range detection is 2");
                                }
                                else if(status.endsWith("1") && secondStatus.endsWith("1")) {
                                    Log.d("AD","Pulse range detection is 3");
                                }
                                else {
                                    Log.d("AD","Pulse range detection is 0");
                                }
                            }
                            else if(i == statusFlagString.length() - 5) {
                                Log.d("AD","Measurment position detection");
                            }
                        }
                    }
                }
                 Intent intent = new Intent();
                intent.setAction("jp.co.aandd.andblelink.ble.data_received");
                intent.putExtra("Systolic", systolic_display);
                intent.putExtra("Diastolic", diastolic_display);
                intent.putExtra("Pulse", pulse_display);
                sendBroadcast(intent);

            }
            else if (ADGattUUID.WeightScaleMeasurement.equals(characteristic.getUuid())) {
                Log.d("Sim","reading for WS standard is received");
                Bundle bundle = new Bundle();
                int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                String flagString = Integer.toBinaryString(flag);
                int offset=0;
                for(int index = flagString.length(); 0 < index ; index--) {
                    String key = flagString.substring(index-1 , index);

                    if(index == flagString.length()) {
                        double convertValue = 0;
                        if(key.equals("0")) {
                            convertValue = 0.005f;
                        }
                        else {
                            convertValue = 0.01f;
                        }
                        // Unit
                        offset+=1;

                        // Value
                        double value = (double)(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)) * convertValue;
                        Log.d("SN", "Weight value :"+value);
                        offset+=2;
                    }
                    else if(index == flagString.length()-1) {
                        if(key.equals("1")) {

                            Log.d("SN", "Y :"+String.format("%04d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset)));
                            offset+=2;
                            Log.d("SN", "M :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "D :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;

                            Log.d("SN", "H :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "M :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                            Log.d("SN", "S :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                        }
                        else {
                            //Get the default date and time
                            Calendar calendar = Calendar.getInstance(Locale.getDefault());

                        }
                    }
                    else if(index == flagString.length()-2) {
                        if(key.equals("1")) {
                            Log.d("SN", "ID :"+String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset)));
                            offset+=1;
                        }
                    }
                    else if(index == flagString.length()-3) {
                        if(key.equals("1")) {
                            // BMI and Height
                        }
                    }
                }


            }

        }


        public void requestReadFirmRevision() {
            if (bluetoothGatt != null) {
                BluetoothGattService service = bluetoothGatt.getService(ADGattUUID.DeviceInformationService);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(ADGattUUID.FirmwareRevisionString);
                    if (characteristic != null) {
                        Log.d(TAG,"calling the requestRead ");
                        bluetoothGatt.readCharacteristic(characteristic);
                    }
                }
            }
        }



        public boolean setupDateTime(BluetoothGatt gatt) {
            boolean isSuccess = false;
            if(gatt != null) {
                isSuccess = setDateTimeSetting(gatt, Calendar.getInstance());
            }
            return isSuccess;
        }

        protected boolean setDateTimeSetting(BluetoothGatt gatt,Calendar cal) {
            boolean isSuccess = false;
            BluetoothGattService gattService = getGattSearvice(gatt);
            if(gattService != null) {
                BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(ADGattUUID.DateTime);
                if(characteristic != null) {
                    Log.d(TAG,"calling the set date time write characteristic");
                    characteristic = datewriteCharacteristic(characteristic, cal);
                    isSuccess = gatt.writeCharacteristic(characteristic);
                }
            }
            Log.d("SN","setDateTimeSetting " + cal.getTime());
            return isSuccess;
        }

    public static BluetoothGattCharacteristic datewriteCharacteristic(BluetoothGattCharacteristic characteristic,Calendar calendar) {

        int year 	= calendar.get(Calendar.YEAR);
        int month 	= calendar.get(Calendar.MONTH)+1;
        int day 	= calendar.get(Calendar.DAY_OF_MONTH);
        int hour 	= calendar.get(Calendar.HOUR_OF_DAY);
        int min 	= calendar.get(Calendar.MINUTE);
        int sec 	= calendar.get(Calendar.SECOND);

        byte[] value = {
                (byte)(year & 0x0FF),	// year 2bit
                (byte)(year >> 8),		//
                (byte)month,			// month
                (byte)day,				// day
                (byte)hour,				// hour
                (byte)min,				// min
                (byte)sec				// sec
        };
        characteristic.setValue(value);

        return characteristic;
    }





        private byte toBCD(int val) {
            return (byte) ((val / 10 * 16) + (val % 10));
        }




        public static String byte2hex(byte[] bytes) {
            BigInteger bi = new BigInteger(1, bytes);
            return String.format("%0" + (bytes.length << 1) + "X", bi);
        }

        byte[] toPrimitives(Byte[] oBytes)
        {
            byte[] bytes = new byte[oBytes.length];

            for(int i = 0; i < oBytes.length; i++) {
                bytes[i] = oBytes[i];
            }

            return bytes;
        }

    }
