package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.example.android.bluetoothlegatt.DeviceControlActivity.ACTION_GET_DATA;
import static com.example.android.bluetoothlegatt.SampleGattAttributes.Bytes2HexString;

/**
 * Created by Richie on 2017/6/5.
 */

public class BluetoothFragment extends Fragment implements BluetoothLeService.Callback {
    CallBack mBack;
    private final String TAG = "BluetoothFragment";
    private BluetoothLeService mBluetoothLeService;
    private BluetoothFragment.LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private Set<String> rigAddressList = new HashSet<String>();  //存储蓝牙设备

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ListView mListView;
    private String mAddress;   //蓝牙地址
    private String mdataStr;


    private boolean mConnected = false;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            switch (msg.what) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    Log.d(TAG, " mConnected: " + mConnected);
//                    updateConnectionState(R.string.connected);
//                    invalidateOptionsMenu(); //声明选项菜单已更改 需重新创建
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;

                    if(null != mLeDeviceListAdapter){
                        mLeDeviceListAdapter.setConnectAdress("");
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }

                    Log.d(TAG, " mConnected: " + mConnected);
//                    updateConnectionState(R.string.disconnected);
//                    invalidateOptionsMenu();
//                    clearUI();
                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:

                    byte[] data = bundle.getByteArray(SampleGattAttributes.EXTRA_DATA);
                    Log.d(TAG, "Hadlerdata==: " + data.length);
                    mdataStr = Bytes2HexString(data);
                    mBluetoothLeService.getCharacteristic(mAddress);
//                    if (mdataStr != null) {
//                        mBack.getData(mdataStr);   //将收到的数据 传入升级界面
//                    }
                    if(null != mBack){
                        mBack.getData(data);
                    }
                    Log.d(TAG, "mDataField==: " + mdataStr);
                    break;
            }

        }
    };
    private ServiceConnection mMServiceConnection;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.listfragment_layout, container, false);
        mListView = (ListView) view.findViewById(R.id.lv_listviewbluetooth);
        mHandler = new Handler();
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            getActivity().finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            getActivity().finish();
        }

        scanLeDevice(true);  //扫描蓝牙
        mLeDeviceListAdapter = new BluetoothFragment.LeDeviceListAdapter();
        mListView.setAdapter(mLeDeviceListAdapter);
        listOnItemClick();


        return view;
    }

    private void listOnItemClick() {
        //当点击ietm的时候 绑定服务 连接蓝牙
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                final String mAddress = device.getAddress();
                mBack.getAdress(mAddress);  //将连接的address 传给过去

                mLeDeviceListAdapter.setConnectAdress(mAddress);
                mLeDeviceListAdapter.notifyDataSetChanged();

                //管理服务的生命周期
                mMServiceConnection = getServiceConnection(mAddress);

                if (mdataStr == null) {
                    Log.d("haha", "消息为空！！！！！: ");
                } else if (mdataStr != null) {
                    Intent intent = new Intent(ACTION_GET_DATA);
                    intent.putExtra("name", mdataStr);
                    getActivity().sendBroadcast(intent);
                }
//                Log.d(TAG, "Connect request result=" + result + ",address:" + mDeviceAddress);
//                if (result) {
//                    rigAddressList.add(mDeviceAddress);
//                }
                Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
                getActivity().bindService(gattServiceIntent, mMServiceConnection, BIND_AUTO_CREATE); //绑定服务  开始连接蓝牙


            }
        });
    }

    @NonNull
    private ServiceConnection getServiceConnection(final String mAddress) {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    getActivity().finish();
                }
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.registerCallback(BluetoothFragment.this);  //注册回调
                    final boolean result = mBluetoothLeService.connect(mAddress);

                    //这里如果蓝牙连接上了 则自动发送数据
//                    byte[] writeBytes = new byte[20];
//                    writeBytes[0] = (byte) 0x00;
//                    mBluetoothLeService.writeCharacteristic(writeBytes);
//                    writeCheckVersion();

                    Log.d(TAG, "Connect request result=" + result + ",address:" + mAddress);
                    if (result) {
                        rigAddressList.add(mAddress);
                        scanLeDevice(false);  //停止扫描
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBluetoothLeService = null;
            }
        };
    }


    private void writeCheckVersion(){
        byte[] versionBytes = new byte[4];
        versionBytes[0] = 0x55;
        versionBytes[1] = 0x04;
        versionBytes[2] = 0x55;
        versionBytes[3] = (byte) 0xAA;
        mBluetoothLeService.writeCharacteristic(versionBytes);
    }
    //连接服务
    private void connectService() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.registerCallback(BluetoothFragment.this);
            final boolean result = mBluetoothLeService.connect(mAddress);
            Log.d(TAG, "Connect request result=" + result + ",address:" + mAddress);
            if (result) {
                rigAddressList.add(mAddress);
                scanLeDevice(false);  //停止扫描
            }
        }
    }

    //开始扫描服务
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.startDiscovery();
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
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


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BluetoothFragment.this.getActivity().getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            BluetoothFragment.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new BluetoothFragment.ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.tv_status = (TextView) view.findViewById(R.id.tv_status);
                view.setTag(viewHolder);
            } else {
                viewHolder = (BluetoothFragment.ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            if(device.getAddress().equals(address)){
                // 已连接
                viewHolder.tv_status.setText(getResources().getString(R.string.connecte));
            }else {
                // 未连接
                viewHolder.tv_status.setText(getResources().getString(R.string.disconnecte));
            }
            return view;
        }

        private String address;
        public void setConnectAdress(String address){
            this.address = address;
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
//                        }
//                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
         TextView tv_status;
    }


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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mBack = (CallBack) getActivity();
        if (activity != null) {
            register();
        }

    }


    @Override
    public void onDetach() {
        super.onDetach();
        scanLeDevice(false);
        unRegister();

    }


    boolean isRegister = false;

    private void register() {
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        isRegister = true;
    }

    private void unRegister() {
        if (isRegister) {
            getActivity().unregisterReceiver(mGattUpdateReceiver);
            isRegister = false;
        }
    }


    public interface CallBack {
        void getAdress(String adress);

        void getData(byte[] data);
    }


}
