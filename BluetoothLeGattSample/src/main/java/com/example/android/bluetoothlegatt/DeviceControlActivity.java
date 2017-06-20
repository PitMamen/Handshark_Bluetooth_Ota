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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.EditText;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.ContentValues.TAG;
import static com.example.android.bluetoothlegatt.SampleGattAttributes.Bytes2HexString;
import static com.example.android.bluetoothlegatt.SampleGattAttributes.hex2byte;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements BluetoothLeService.Callback {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public static String ACTION_GET_DATA = "Receive_the_data";

    private TextView mConnectionState, device_address;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private Button btn_down;
    private TextView tv_currentPro;
    private ProgressBar pro_bar;
    private int index = 0, pro_Status = 0;
    String mdataStr;


    private Set<String> rigAddressList = new HashSet<String>();  //存储蓝牙设备
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
//            mBluetoothLeService.connect(mDeviceAddress);
            connectService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
//            String address = bundle.getString(Constants.EXTRA_ADDRESS);
            switch (msg.what) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    Log.d(TAG, " mConnected: " + mConnected);
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu(); //声明选项菜单已更改 需重新创建
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    displayGattServices(mBluetoothLeService.getSupportedGattServices(mDeviceAddress));
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;

                    Log.d(TAG, " mConnected: " + mConnected);
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    clearUI();
                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    // 将数据显示在mDataField上
                    byte[] data = bundle.getByteArray(SampleGattAttributes.EXTRA_DATA);
                    Log.d(TAG, "Hadlerdata==: " + data.length);
                    mdataStr = Bytes2HexString(data);
                    //展示数据
                    displayData(mdataStr);
                    mBluetoothLeService.getCharacteristic(mDeviceAddress);
                    break;
            }
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "action:" + action);
            String address;
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                address = device.getAddress();
                rigAddressList.add(address);
                Log.d(TAG, "devices:" + device.getName() + "address:" + address);
            }
        }
    };


//    private void clearUI() {
//        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
//        mDataField.setText(R.string.no_data);
//    }

    // 连接服务
    private void connectService() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.registerCallback(this);
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            //这里如果蓝牙连接上了 则自动写数据
            byte[] writeBytes = new byte[20];
            writeBytes[0] = (byte) 0x00;
            mBluetoothLeService.writeCharacteristic(writeBytes);


            if (mdataStr == null) {
                Log.d("haha", "消息为空！！！！！: ");
            } else if (mdataStr != null) {
                Intent intent = new Intent(ACTION_GET_DATA);
                intent.putExtra("name", mdataStr);
                sendBroadcast(intent);
            }
            Log.d(TAG, "Connect request result=" + result + ",address:" + mDeviceAddress);
            if (result) {
                rigAddressList.add(mDeviceAddress);
            }
        }
    }


    Handler Prohandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 3) {
                pro_Status = (int) msg.obj;
                pro_bar.setProgress(pro_Status);  //设置当前进度
                int tv_percentage = (pro_bar.getProgress() * 100 + 1) / 100;//当前的进度除以进度条的最大值 获得百分比
                tv_currentPro.setText("当前进度：" + tv_percentage + "%");

            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        initView();
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


    }

    private void writeCharacteristic(byte[] data) {
        mBluetoothLeService.writeCharacteristic(data);
    }

    private List<byte[]> getPackge(String fifeName) {
        InputStream inputStream = null;
        List<byte[]> allpacklist;
        try {
            inputStream = getResources().getAssets().open(fifeName);
            int count = 0;
            byte[] bufferData = new byte[16];
            //bufferData清零
            for (int i = 0; i < bufferData.length; i++) {
                bufferData[i] = (byte) 0xff;
            }

            allpacklist = new ArrayList<>();
            //读取bin文件
            while (inputStream.read(bufferData) != -1) {
                byte[] tmppackge = new byte[20];
                tmppackge[0] = (byte) (count % 0xff);
                tmppackge[1] = (byte) (count / 0xff);
                for (int j = 0; j < 16; j++) {
                    tmppackge[j + 2] = bufferData[j];
                }
                allpacklist.add(tmppackge);
                count++;
                //bufferData清零
                for (int y = 0; y < bufferData.length; y++) {
                    bufferData[y] = (byte) 0xff;
                }
            }
            Log.d(TAG, "fenbao==: " + allpacklist.size());
            for (byte[] str : allpacklist) {
                Log.d(TAG, "fenbao==: " + str[0] + str[1] + str[2] + str[3] + str[4] + str[5] + str[6] + str[7] + str[8] + str[9] + str[10] + str[11] + str[12] + str[13] + str[14] + str[15] + str[16] + str[17]);
            }
            if (allpacklist != null) {
                return allpacklist;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void initView() {
        btn_down = (Button)findViewById(R.id.btn_update);
        pro_bar = (ProgressBar) findViewById(R.id.progressBar);
        tv_currentPro = (TextView) findViewById(R.id.tv_CurrentPro);
        device_address = (TextView) findViewById(R.id.device_address);
        device_address.setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);


        //升级操作全在这个按钮中
        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] startPackeg = new byte[2];
                        startPackeg[0] = (byte) 0X01;
                        startPackeg[1] = (byte) 0XFF;
                        writeCharacteristic(startPackeg);   //发送 开始包

                        try {
                            boolean is01FF = false;
                            do {
                                Thread.sleep(10);

                                if (mdataStr != null) {
                                    if (mdataStr.equals("01FF")) {     //开始包回执  01FF
                                        is01FF = true;
                                    } else {
                                        is01FF = false;
                                    }
                                }
                                Log.d(TAG, "is01FF: " + is01FF);
                            } while (is01FF == false);

                            List<byte[]> byteList = getPackge("updata.bin");
                            //nowpackmun 当前发送包的包数 根据发送包的数量 进度条动态变化
                            for (int nowpackmun = 0; nowpackmun < byteList.size(); nowpackmun++) {
                                byte[] bytes = byteList.get(nowpackmun);
                                int cureentPackgeNum = nowpackmun;
                                writeCharacteristic(bytes);
                                if (pro_Status <byteList.size()) {
                                    pro_bar.setMax(100);  //设置最大值为100
                                    pro_Status = ((cureentPackgeNum * 100) / byteList.size()) + 1;   //进度条进度
                                    Log.d(TAG, "pro_Status==: " + pro_Status);
                                    Message message = Prohandle.obtainMessage();
                                    message.what = 3;
                                    message.obj = pro_Status;
                                    Prohandle.sendMessage(message);
                                    if (cureentPackgeNum==byteList.size()){   //发完即断开
                                        unRegister();
                                        mBluetoothLeService.disconnect();

                                    }
                                }

                                boolean PackIndexReturn = false;
                                int count_Second = 0;
                                do {
                                    Thread.sleep(10);

                                    byte[] a = {bytes[0], bytes[1]};
                                    if (mdataStr != null) {
                                        if (mdataStr.equals(Bytes2HexString(a))) {
                                            PackIndexReturn = true;
                                        } else {
                                            PackIndexReturn = false;
                                        }
                                    }
                                    Log.d(TAG, "PackIndexReturn: " + PackIndexReturn + "a" + a);

                                    if (count_Second > 20) {
                                        writeCharacteristic(bytes);
                                    }
                                    count_Second++;
                                } while (PackIndexReturn == false);
                            }


                            //发送刷机包 进入刷机模式 开始刷机
//                            byte[] startfrelsh = new byte[2];
//                            startfrelsh[0] = (byte) 0X02;
//                            startfrelsh[1] = (byte) 0XFF;
//                            writeCharacteristic(startfrelsh);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    }
                }).start();

            }
        });
    }


    boolean isRegister = false;

    private void register() {
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        isRegister = true;
    }

    private void unRegister() {
        if (isRegister) {
            unregisterReceiver(mGattUpdateReceiver);
            isRegister = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        register();
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unRegister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.unRegisterCallback(this);
        }
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                register();
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                unRegister();
                return true;
            case R.id.menu_refresh:
//                Intent intent = new Intent(DeviceControlActivity.this, ShowfirmwareDataActivity.class);
//                startActivity(intent);

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    //广播动作
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        //system
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }

    @Override
    public void onDispatchData(int type, byte[] data, String address) {
        Message message = handler.obtainMessage(type);       //从已经得到数据的回调中  再通过handler发送
        Bundle bundle = new Bundle();
        bundle.putByteArray(SampleGattAttributes.EXTRA_DATA, data);
        bundle.putString(SampleGattAttributes.EXTRA_ADDRESS, address);
        message.setData(bundle);
        handler.sendMessage(message);

    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    byte[] WriteBytes = new byte[20];
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    final EditText et ;  //申明变量

                    et = new EditText(parent.getContext()); //创建对象
                    et.setSingleLine(true);  //设置属性

                    final EditText etHex ;  //申明变量
                    etHex = new EditText(parent.getContext()); //创建对象
                    etHex.setSingleLine(true);  //设置属性

                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();

                        //如果该char可写
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {


                            LayoutInflater factory = LayoutInflater.from(parent.getContext());
                            final View textEntryView = factory.inflate(R.layout.dialog, null);
                            final EditText editTextName = (EditText) textEntryView.findViewById(R.id.editTextName);
                            final EditText editTextNumEditText = (EditText)textEntryView.findViewById(R.id.editTextNum);
                            AlertDialog.Builder ad1 = new AlertDialog.Builder(parent.getContext());
                            ad1.setTitle("WriteCharacteristic");
                            ad1.setView(textEntryView);
                            ad1.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int i) {
                                    byte[] value = new byte[20];
                                    value[0] = (byte) 0x00;
                                    if(editTextName.getText().length() > 0){
                                        //write string
                                        WriteBytes= editTextName.getText().toString().getBytes();
                                    }else if(editTextNumEditText.getText().length() > 0){
                                        WriteBytes= hex2byte(editTextNumEditText.getText().toString().getBytes());
                                    }
                                    characteristic.setValue(value[0],
                                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                                    characteristic.setValue(WriteBytes);

                                    mBluetoothLeService.writeCharacteristic(WriteBytes);
                                }
                            });
                            ad1.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int i) {

                                }
                            });
                            ad1.show();

                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.

    //显示蓝牙中所有的服务
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }
}
