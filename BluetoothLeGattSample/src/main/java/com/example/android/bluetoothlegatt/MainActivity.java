package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Created by Richie on 2017/6/5.
 */

public class MainActivity extends Activity implements BluetoothFragment.CallBack {

    private static final String TAG = "MainActivity";
    private Fragment mFragCurrent;
    private Fragment mFragUpdate;
    private Fragment mFragBluetooth;
    private final String FRAG_TAG_UPDATE = "update";
    private final String FRAG_TAG_BLUETOOTH = "bluetooth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.ota_mainlayout);

        initFragment();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        RadioGroup rgContent = (RadioGroup) findViewById(R.id.rg_content);
        rgContent.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.rb_content_update:
                        if (null == ver) {
                            writeCheckVersion();
                        }
                        if (mFragCurrent != mFragUpdate) {
//                            replaceFragment(mFragUpdate, FRAG_TAG_UPDATE);
                            showHideFragment(mFragUpdate, mFragBluetooth);
                        }
                        break;
                    case R.id.rb_content_bluetooth:
                        if (mFragCurrent != mFragBluetooth) {
//                            replaceFragment(mFragBluetooth, FRAG_TAG_BLUETOOTH);
                            showHideFragment(mFragBluetooth, mFragUpdate);
                        }
                        break;
                }
            }
        });

        RadioButton rbBluetooth = (RadioButton) findViewById(R.id.rb_content_bluetooth);
        rbBluetooth.setChecked(true);
    }

    private void replaceFragment(Fragment fragment, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (null == frag) {
            ft.add(R.id.fl_content, fragment, tag);
        } else {
            switch (tag) {
                case "update":
                    ft.show(mFragUpdate);
                    ft.hide(mFragBluetooth);
                    break;
                case "bluetooth":
                    ft.show(mFragBluetooth);
                    ft.hide(mFragUpdate);
                    break;
            }
//            ft.replace(R.id.fl_content, fragment, tag);
        }
        ft.commit();
        mFragCurrent = fragment;
    }

    @Override
    public void getAdress(String adress) {
        mAddress = adress;
    }

    private byte[] ver;

    @Override
    public void getData(byte[] data) {
        if (null == ver && null != data) {
            ((UpdataFragment) mFragUpdate).setVersion(data);
            ver = data;
        } else {
            ((UpdataFragment) mFragUpdate).setData(data);
        }
    }

    private BluetoothLeService mBluetoothLeService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            mConnected = false;
        }
    };

    private boolean mConnected;
    private String mAddress;

    public boolean writeCharacteristic(byte[] data) {
        if (mConnected && null != mAddress) {
            mBluetoothLeService.writeCharacteristic(data);
        }
        return mConnected && null != mAddress;
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    public void writeCheckVersion() {
        byte[] versionBytes = new byte[4];
        versionBytes[0] = 0x55;
        versionBytes[1] = 0x04;
        versionBytes[2] = 0x55;
        versionBytes[3] = (byte) 0xAA;
        if (null != mBluetoothLeService) {
            mBluetoothLeService.writeCharacteristic(versionBytes);
        }
    }

    public void resetVer() {
        ver = null;
    }


    private void initFragment() {
        mFragUpdate = new UpdataFragment();
        addFragment(mFragUpdate, FRAG_TAG_UPDATE);

        mFragBluetooth = new BluetoothFragment();
        addFragment(mFragBluetooth, FRAG_TAG_BLUETOOTH);
    }

    private void addFragment(Fragment fragment, String tag) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fl_content, fragment, tag);
        ft.commit();
    }

    private void showHideFragment(Fragment show, Fragment hide) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.show(show);
        ft.hide(hide);
        ft.commit();
        mFragCurrent = show;
    }
}
