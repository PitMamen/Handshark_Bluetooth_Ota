/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();



    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    //    private BluetoothGatt mBluetoothGatt;
    private int modelType = 0;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;       //断开连接
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    /*public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";*/

    //每一种状态都在BluetoothGattCallback重写的方法里做了相对应的操作
    public final static int ACTION_GATT_CONNECTED = 1;
    public final static int ACTION_GATT_DISCONNECTED = 2;
    public final static int ACTION_GATT_SERVICES_DISCOVERED = 3;
    public final static int ACTION_DATA_AVAILABLE = 4;
//    public final static UUID UUID_HEART_RATE_MEASUREMENT =
//            UUID.fromString("9168ffe0-1111-6666-8888-0123456789AB");

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1911");  // daydream  服务UUID 0000fe56-0000-1000-8000-00805f9b34fb


    private BluetoothGattCharacteristic mNotifyCharacteristic;

    Set<Callback> mCallbacks = new HashSet<>();

    HashMap<String, BluetoothGatt> bluetoothGattMap = new HashMap<String, BluetoothGatt>();
    HashMap<String, BluetoothGattService> mnotyGattServiceMap = new HashMap<String, BluetoothGattService>();
    HashMap<String, BluetoothGattCharacteristic> writeCharacteristicMap = new HashMap<String, BluetoothGattCharacteristic>();
    HashMap<String, BluetoothGattCharacteristic> readCharacteristicMap = new HashMap<String, BluetoothGattCharacteristic>();

    Set<String> addressList = new HashSet<>();

    boolean isStop = true;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection game and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            int type;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                type = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;

                String address = gatt.getDevice().getAddress();

                addressList.add(address);
                bluetoothGattMap.put(address, gatt);

                getCharacteristic(address);
                broadcastUpdate(type, address);
                Log.i(TAG, "Connected to GATT server." + address);
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                type = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;

                String address = gatt.getDevice().getAddress();
                addressList.remove(address);
                bluetoothGattMap.remove(address);

                removeCharacteristic(address);
                Log.i(TAG, "Disconnected from GATT server." + address);
                broadcastUpdate(type, address);

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String address = gatt.getDevice().getAddress();
                bluetoothGattMap.put(address, gatt);
                getCharacteristic(address);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, address);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                System.out.println("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead, GATT_SUCCESS");
                String address = gatt.getDevice().getAddress();
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, address);
                updateTime();
            }
        }

        private void removeCharacteristic(String address) {
            mnotyGattServiceMap.remove(address);
            writeCharacteristicMap.remove(address);
            readCharacteristicMap.remove(address);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
            Log.d(TAG, "onCharacteristicChanged thread:" + Thread.currentThread().getName() + ",id:" + Thread.currentThread().getId());
            updateTime();
            String address = gatt.getDevice().getAddress();
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, address);
        }
    };

//    private void updateTime1(byte[] data) {
//        if (data.length > 3 && data[2] == (byte) 0x09) {
//            long now = System.nanoTime();
//            long offset1 = now - before;
//            before = now;
//            Log.d(TAG, "offset1:" + offset1);
//            String dataStr = Byte2six.Bytes2HexString(data);
//            Log.d(TAG, "dataStr:" + dataStr);
//        }
//    }

    long before = 0;

    private void updateTime() {
        long nowTime = System.nanoTime();
        long offsetTime = nowTime - before;
        before = nowTime;
        Log.d(TAG, "offsetTime:" + offsetTime);
    }

    private void broadcastUpdate(final int type, String address) {
        dispathData(type, null, address);
    }

    //该方法将从蓝牙中获取数据
    private void broadcastUpdate(final int type, final BluetoothGattCharacteristic characteristic, String address) {
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
//            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {

            final byte[] data = characteristic.getValue();    //这里从蓝牙中获取数据

            System.out.println("data.lenght=="+data.length);
            if (data != null && data.length > 0) {
                Log.d(TAG, "lenght:" + data.length);
                dispathData(type, data, address);
            }
        }
    }


    private void dispathData(int type, byte[] data, String address) {
        for (Callback callback : mCallbacks) {
            callback.onDispatchData(type, data, address);
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind thread:" + Thread.currentThread().getName() + ",id:" + Thread.currentThread().getId());
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        BluetoothGatt bluetoothGatt = bluetoothGattMap.get(address);
        if (bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt == null) {
            Log.d(TAG, "mBluetoothGatt is null," + address);
            return false;
        }

        Log.d(TAG, "Trying to create a new connection.");
//        mBluetoothDeviceAddress = address;
        addressList.add(address);
        bluetoothGattMap.put(address, bluetoothGatt);
        mConnectionState = STATE_CONNECTING;
        System.out.println("device.getBondState==" + device.getBondState());       //12
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || bluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt bluetoothGatt : bluetoothGattMap.values()) {
            bluetoothGatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothAdapter == null || bluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt bluetoothGatt : bluetoothGattMap.values()) {
            bluetoothGatt.close();
        }
        bluetoothGattMap.clear();
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || bluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt bluetoothGatt : bluetoothGattMap.values()) {
            bluetoothGatt.readCharacteristic(characteristic);
        }
    }

    //向蓝牙中写入数据
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || bluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        Log.d(TAG, "size:" + bluetoothGattMap.size());
        for (BluetoothGatt bluetoothGatt : bluetoothGattMap.values()) {
//            if("".equals(bluetoothGatt.getDevice().getAddress()))
            bluetoothGatt.writeCharacteristic(characteristic);
        }
        before = System.nanoTime();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || bluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt bluetoothGatt : bluetoothGattMap.values()) {
            bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        }

        // This is specific to Heart Rate Measurement.
        if (characteristic.getUuid().equals(UUID_HEART_RATE_MEASUREMENT)) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12"));//属性UUID  0000ffe1-0000-1000-8000-00805f9b34fb


            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            for (BluetoothGatt bluetoothGatt : bluetoothGattMap.values()) {
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

//    AtomicBoolean changeStart = new AtomicBoolean(false);

    public void writeChangeCharacteristic(byte[] data) {

    }


    //写数据的关键方法  发出通知 获取数据
    public void writeCharacteristic(byte[] date) {
        Log.d(TAG, "rigAddressList size:" + addressList.size());
        for (String address : addressList) {
            getCharacteristic(address);
        }

        if (writeCharacteristicMap.isEmpty()) return;

        for (BluetoothGattCharacteristic writeCharacteristic : writeCharacteristicMap.values()) {
            final int charaProp = writeCharacteristic.getProperties();

            //如果该char可写
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    setCharacteristicNotification(mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                //读取数据，数据将在回调函数中
                //mBluetoothLeService.readCharacteristic(characteristic);
                writeCharacteristic.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                writeCharacteristic.setValue(date);
                writeCharacteristic(writeCharacteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = writeCharacteristic;
                setCharacteristicNotification(writeCharacteristic, true);
            }
        }
    }

    /*
     * **************************************************************
	 * *****************************读函数*****************************
	 */
    public void read(BluetoothGattCharacteristic readCharacteristic) {
        // mBluetoothLeService.readCharacteristic(readCharacteristic);
        // readCharacteristic的数据发生变化，发出通知
        setCharacteristicNotification(readCharacteristic, true);
        // Toast.makeText(this, "读成功", Toast.LENGTH_SHORT).show();
    }

    public void getCharacteristic(String address) {
        // 写数据的服务和characteristic
        BluetoothGattService mnotyGattService = mnotyGattServiceMap.get(address);
        if (mnotyGattService == null) {
            mnotyGattService = getSupportedGattServices(address, UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1911"));  //服务UUID 0000fe56-0000-1000-8000-00805f9b34fb
        }
        if (mnotyGattService == null) {
            Log.d(TAG, "mnotyGattService is null," + address);
//            bluetoothGattMap.remove(address);
            return;
        } else {
            mnotyGattServiceMap.put(address, mnotyGattService);
        }
        BluetoothGattCharacteristic writeCharacteristic = writeCharacteristicMap.get(address);
        if (writeCharacteristic == null) {
            writeCharacteristic = mnotyGattService
                    .getCharacteristic(UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12"));  //写
        }
        if (writeCharacteristic == null) {
            Log.d(TAG, "mnotyGattService is null," + address);
            return;
        } else {
            writeCharacteristicMap.put(address, writeCharacteristic);
        }

        // 读数据的服务和characteristic
        BluetoothGattCharacteristic readCharacteristic = readCharacteristicMap.get(address);
        if (readCharacteristic == null) {
            readCharacteristic = mnotyGattService
                    .getCharacteristic(UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b12"));       //读
        }
        if (readCharacteristic == null) {
            Log.d(TAG, "mnotyGattService is null," + address);
            return;
        } else {
            readCharacteristicMap.put(address, readCharacteristic);
            read(readCharacteristic);
        }

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        BluetoothGatt bluetoothGatt = bluetoothGattMap.get(address);
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    public BluetoothGattService getSupportedGattServices(String address, UUID uuid) {
        BluetoothGattService mBluetoothGattService;
        BluetoothGatt bluetoothGatt = bluetoothGattMap.get(address);
        if (bluetoothGatt == null) {
            Log.d(TAG, "bluetoothGatt is null");
            return null;
        }
        mBluetoothGattService = bluetoothGatt.getService(uuid);
        return mBluetoothGattService;
    }

    //注册接口回调
    public void registerCallback(Callback callback) {
        if (callback != null) {
            this.mCallbacks.add(callback);
        }
    }

    public void unRegisterCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public interface Callback {
        void onDispatchData(int type, byte[] data, String address);
    }
}
