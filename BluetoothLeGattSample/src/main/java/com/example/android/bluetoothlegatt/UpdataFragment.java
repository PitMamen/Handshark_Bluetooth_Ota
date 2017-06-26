package com.example.android.bluetoothlegatt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.czp.library.ArcProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.R.attr.data;
import static android.R.attr.name;
import static android.R.attr.value;
import static com.example.android.bluetoothlegatt.SampleGattAttributes.Bytes2HexString;

/**
 * Created by Richie on 2017/6/5.
 */

public class UpdataFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();
    private TextView tv_currentversion, tv_bestnewversion, tv_CurrentPro, tv_new_serverion;
    private ArcProgress pro_bar;
    private Button btn_updata;
    private byte[] mdataStr;
    private int pro_Status = 0;
    private int mLen;
    private Context mContext = MyApplication.getApplication();
    private boolean mAvailable;
    private String mVersion;
    private String mTemp;  //服务器版本号
    private static final int MSG_UPDATE_BAR = 0x1110;
    private static final int MSG_UPDATE_VER = 0x1111;

    String urlStr = "http://123.207.115.76:8899/app/drives/maxVersion.do";
    String current_version = mContext.getString(R.string.current_version);
    Handler Prohandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == MSG_UPDATE_BAR) {
                pro_Status = msg.arg1;
                pro_bar.setProgress(pro_Status);  //设置当前进度
                int tv_percentage = (pro_bar.getProgress() * 100 + 1) / 100;//当前的进度除以进度条的最大值 获得百分比

                if (0 == msg.arg1) {
                    String sucessful = getString(R.string.sucessful);
                    tv_CurrentPro.setText(sucessful);

                    tv_currentversion.setText(current_version + mTemp);
                } else {
                    String currentPro = getString(R.string.current_pro);
                    tv_CurrentPro.setText(currentPro + tv_percentage + "%");
                }
            } else if (MSG_UPDATE_VER == msg.what) {
                if (null != tv_currentversion && null != mContext) {
                    tv_currentversion.setText(current_version + version);
                }
            }

        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.updata_fragment_layput, null, false);
        mAvailable = isNetworkAvailable(getActivity());
        initView(view);
        return view;
    }

    private List<byte[]> binDatas = new ArrayList<>();

    private void DownloadBin() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = null;
                    try {
                        url = new URL(urlStr);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("api-version", "1.0.0");
                        conn.setRequestMethod("GET");

                        conn.setDoInput(true);

                        //取得inputStream，并进行读取
                        InputStream input = conn.getInputStream();
                        BufferedReader in = new BufferedReader(new InputStreamReader(input));
                        String line = null;
                        StringBuffer sb = new StringBuffer();
                        while ((line = in.readLine()) != null) {
                            sb.append(line);   //拼接
                        }
                        String json = sb.toString();
                        String binUrl = null;
                        try {
                            JSONObject object = new JSONObject(json);
                            JSONObject jsonObject = object.optJSONObject("response");
                            JSONObject drvies = jsonObject.optJSONObject("drives");
                            System.out.println("drvies==" + drvies);
                            //服务器版本号
                            mVersion = drvies.optString("version");


                            String name = drvies.optString("driveName");
                            Log.e(TAG, "name: " + name);  //MY_C04_MXB1422
                            String tmp = name.substring(name.length() - 2);
                            mTemp = tmp.substring(0, 1) + "." + tmp.substring(1, tmp.length());
                            Log.e(TAG, "temp==: " + mTemp);  //2.2
//
//
//                            byte[] namebyte = name.getBytes();
//                            Log.e(TAG, "bb: "+Arrays.toString(namebyte) );  //[77, 89, 95, 67, 48, 52, 95, 77, 88, 66, 49, 52, 50, 50]
//
//                            byte[] verbyte = {namebyte[12],namebyte[13]};
//                            Log.e(TAG, "verbyte: "+Arrays.toString(verbyte) );
//                            BigInteger big = new BigInteger(verbyte);
//                            String str = big.toString(16);
//                            mVersion = getVersion(Integer.parseInt(str));
//                            Log.e(TAG, "mVersion==: "+mVersion );  //  0.0


                            Log.d(TAG, "Version==: " + mVersion);   //1.0.4

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mVersion != null) {
                                        tv_new_serverion.setText(getResources().getString(R.string.checknew_sersion) + mTemp);
                                    }
                                }
                            });


                            String driveUrl = drvies.optString("driveUrl");
                            String filePrefix = jsonObject.optString("filePrefix");
                            binUrl = filePrefix + driveUrl;
                            Log.d(TAG, "run: ******** -> url = " + binUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        input.close();
                        in.close();
                        //
//                        if(ver > us){
//                            return;
//                        }
//                        if(null != version && version.equals(mVersion)){
//
//                            //TODO:已经是最新版本
//                            return;
//                        }


                        URL binUU = new URL(binUrl);
                        HttpURLConnection connection = (HttpURLConnection) binUU.openConnection();
                        InputStream is = connection.getInputStream();

                        binDatas.clear();
                        binDatas.addAll(readIs(is));    //将读取到的 bin文件内容 存储添加在集合中

                        is.close();

                        Log.d(TAG, "run: ******-> download ok");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private boolean writeCharacteristic(byte[] data) {
        Activity activity = getActivity();
        if (null != activity && activity instanceof MainActivity) {
            return ((MainActivity) activity).writeCharacteristic(data);
        }
        return false;
    }


    // 读取流，并分包
    @SuppressLint("NewApi")
    private List<byte[]> readIs(InputStream is) {
        List<byte[]> data = new ArrayList<>();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buff = new byte[1024];
            int len = -1;
            while (-1 != (len = is.read(buff))) {
                bos.write(buff, 0, len);
            }
            byte[] bytes = bos.toByteArray();
            int length = bytes.length;
            int fullCount = (int) Math.ceil(length / 16.0) - 1;
            int current = 0;
            for (int i = 0; i < fullCount; i++) {
                byte[] b = new byte[20];
                for (int j = 0; j < 20; j++) {
                    // 补0
                    b[j] = (byte) 0xFF;
                }
                System.arraycopy(bytes, current, b, 2, 16);
                current += 16;
                data.add(b);
            }
            int last = length - 16 * fullCount;
            byte[] b = new byte[20];
            for (int j = 0; j < 20; j++) {
                // 补0
                b[j] = (byte) 0xFF;
            }
            System.arraycopy(bytes, current, b, 2, last);
            data.add(b);
            // 异或操作
            int size = data.size();
            for (int i = 0; i < size; i++) {
                byte[] bb18 = data.get(i);
                bb18[18] = getXor(bb18);
                data.set(i, bb18);
            }
            // 添加标志
            for (int i = 0; i < size; i++) {
                byte[] byteFlag = data.get(i);
                byteFlag[0] = (byte) (i % 256);
                byteFlag[1] = (byte) (i / 256);
                data.set(i, byteFlag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public byte getXor(byte[] datas) {
        byte temp = datas[2];
        for (int i = 3; i < datas.length - 2; i++) {
            temp ^= datas[i];
        }
        return temp;
    }


    private boolean running;

    private void initView(View view) {
        pro_bar = (ArcProgress) view.findViewById(R.id.myProgress);
        tv_CurrentPro = (TextView) view.findViewById(R.id.tv_CurrentPro);
        tv_bestnewversion = (TextView) view.findViewById(R.id.tv_bestnew_version);
        tv_currentversion = (TextView) view.findViewById(R.id.tv_currentversion);
        btn_updata = (Button) view.findViewById(R.id.btn_updata);
        tv_new_serverion = (TextView) view.findViewById(R.id.tv_new_serverion);

        //升级操作全在这个按钮中
        btn_updata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAvailable) {
                    new Thread(new Runnable() {

                        private Message mMessage;

                        @Override
                        public void run() {
                            if (running) {
                                return;
                            }

                            Log.d("ota", "run: ************-> " + binDatas.size());

                              //开始包 20个字节,3~16个字节为0Xff，
                            byte[] buffer = new byte[16];
                            for (int i = 0; i < 16; i++) {
                                buffer[i] = (byte) 0xFF;
                            }
                            //开始包 FF01
                            byte[] startPackeg = new byte[20];
                            startPackeg[1] = (byte) 0XFF;
                            startPackeg[0] = (byte) 0X01;
                            for (int i = 0; i < 16; i++) {
                                startPackeg[i + 2] = buffer[i];
                            }
                            //在开始包的 第19个字节 是异或操作，
                            startPackeg[18] = getXor(buffer);
                            boolean connect = writeCharacteristic(startPackeg);   //发送 开始包
                            SystemClock.sleep(200);
                            if (connect && !running) {
                                running = true;
                                try {
                                    //如果回执不是FF01 则继续等待
                                    Log.d("ota", "run: *****************-> " + mdataStr[0] + " : " + mdataStr[1]);

                                    byte[] in = {startPackeg[0], startPackeg[1]};
                                    //如果回执不等于 发过包的索引,则一直等待
                                    while (!checkByte(in, mdataStr)) {
                                        SystemClock.sleep(10);
                                    }
                                    //如果是 则开始发后面的包
                                    List<byte[]> byteList = binDatas;
                                    if (null == byteList || byteList.isEmpty()) {
                                        return;
                                    }
                                    pro_bar.setMax(100);  //设置最大值为100
                                    int len = byteList.size();
                                    int k = 1;
                                    //nowpackmun 当前发送包的包数 根据发送包的数量 进度条动态变化
                                    for (int nowpackmun = 0; nowpackmun < len; nowpackmun++) {
                                        byte[] bytes = byteList.get(nowpackmun);
                                        int cureentPackgeNum = nowpackmun;
                                        Log.d("pxk", "run: 4  " + nowpackmun + "******");
                                        writeCharacteristic(bytes);

                                        if (pro_Status < byteList.size()) {
                                            pro_Status = ((cureentPackgeNum * 100) / byteList.size()) + 1;   //进度条进度
                                            Log.d(TAG, "pro_Status==: " + pro_Status);
                                            mMessage = Prohandle.obtainMessage();
                                            mMessage.what = MSG_UPDATE_BAR;
                                            mMessage.arg1 = pro_Status;
                                            Prohandle.sendMessage(mMessage);
                                        }


                                        byte[] a = {bytes[0], bytes[1]};
                                        int count_Second = 0;
                                        while (!checkByte(a, mdataStr)) {
                                            if (count_Second > 20) {
                                                writeCharacteristic(bytes);
                                                count_Second = 0;
                                            } else {
                                                count_Second++;
                                            }
                                            SystemClock.sleep(50);
                                        }

                                        Log.d("ota", "run: **********-> " + k + " mdataStr = " + mdataStr[0] + " : " + mdataStr[1]);
                                        k++;
                                    }


                                    // 所有包都发送完   发送刷机包 进入刷机模式 开始刷机
                                    byte[] startfrelsh = new byte[20];
                                    startfrelsh[1] = (byte) 0XFF;
                                    startfrelsh[0] = (byte) 0X02;
                                    for (int i = 0; i < 16; i++) {
                                        startfrelsh[i + 2] = buffer[i];
                                    }
                                    startfrelsh[18] = getXor(buffer);
                                    writeCharacteristic(startfrelsh);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                running = false;
                                mMessage = Prohandle.obtainMessage();
                                mMessage.what = MSG_UPDATE_BAR;
                                mMessage.arg1 = 0;
                                Prohandle.sendMessage(mMessage);
                            } else {
                                running = false;
                            }
                        }
                    }
                    ).start();
                } else {
                    String toast = getString(R.string.please_connect_network);
                    Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
                }


            }
        });


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    private byte[] checkVersion;//版本

    public void setData(byte[] dataStr) {
        mdataStr = dataStr;
        Log.d("data", "setData: " + Arrays.toString(dataStr));
    }

    private String version;

    public void setVersion(byte[] ver) {
        checkVersion = ver;
        Log.d("ota", "setVersion: " + Bytes2HexString(checkVersion));
        int len = checkVersion.length;
        if (len > 3) {
            byte[] bb = new byte[1];  //定义一个临时数组  存储版本信息
            bb[0] = checkVersion[2];   //第三个字节赋给 临时数组
            BigInteger big = new BigInteger(bb);
            String str = big.toString(16);
            version = getVersion(Integer.parseInt(str));
            Prohandle.sendEmptyMessage(MSG_UPDATE_VER);
//            tv_currentversion.setText(getString(R.string.current_version) + version);
        }
        Log.d("ota", "setVersion: " + version);
        if (mAvailable) {
            DownloadBin();
        }
    }

    //判断收到的回执 是否等于发过包的前两个字节
    private boolean checkByte(byte[] in, byte[] base) {
        if (null != in && null != base) {
            int inLen = in.length;
            int baseLen = base.length;
            if (inLen == baseLen) {
                boolean eq = true;
                for (int i = 0; i < inLen; i++) {
                    if (in[i] != base[i]) {
                        eq = false;
                    }
                }
                return eq;
            }
        }
        return false;
    }

    //判断网络连接
    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isAvailable = false;
        if (cm == null) {

        } else {
            //如果只需判断当前是否有可用网络 则只要添加下面一行
            if (cm.getActiveNetworkInfo() != null) {
                isAvailable = cm.getActiveNetworkInfo().isAvailable();
            }

            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return isAvailable;
    }


    //解析  当前版本号
    private String getVersion(int b) {
        byte c = (byte) (b << 4);
        String a = byteToBit(c);
        int last = a.length();
        String temp = a.substring(0, 1) + "." + a.substring(1, last - 1);
        return String.valueOf(Float.parseFloat(temp));
    }

    private String byteToBit(byte b) {
        return "" + (byte) ((b >> 7) & 0x1) +
                (byte) ((b >> 6) & 0x1) +
                (byte) ((b >> 5) & 0x1) +
                (byte) ((b >> 4) & 0x1) +
                (byte) ((b >> 3) & 0x1) +
                (byte) ((b >> 2) & 0x1) +
                (byte) ((b >> 1) & 0x1) +
                (byte) ((b >> 0) & 0x1);
    }

    private void resetCheckVersion() {
        Activity act = getActivity();
        if (null != act && act instanceof MainActivity) {
            ((MainActivity) act).resetVer();
            checkVersion = null;
        }
    }
}
