package com.example.android.bluetoothlegatt;

import android.app.Application;
import android.content.Context;

/**
 * Created by Richie on 2017/6/12.
 */

public class MyApplication extends Application {
    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        MyApplication.mContext  = getApplicationContext();
    }

    public static  Context getApplication(){


        return MyApplication.mContext;

    }
}
