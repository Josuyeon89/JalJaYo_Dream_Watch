package com.example.retrofit_test;

import static com.example.retrofit_test.SharedObjects.accelerometer;
import static com.example.retrofit_test.SharedObjects.heartRateList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;


import android.util.Log;
import android.widget.Button;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ServerConnectionActivity extends Activity  {
    private static final String TAG = "ServerConnectionActivity";
    private Button server_connection_button;
    public static Context context;
    private Intent serverConnectionService, measureService;

    private boolean isMeasuring = false; // 측정 상태를 나타내는 변수




    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACTION_DATA_RECEIVED")) {
                if (isMeasuring) { // 측정 중인 경우에만 데이터 처리
                    sendDeviceInfo();

                    ArrayList<ArrayList<String>> accelerometerData = (ArrayList<ArrayList<String>>)
                            intent.getSerializableExtra("ACCELEROMETER_DATA");
                    ArrayList<String> heartRateData = intent.getStringArrayListExtra("HEART_RATE_DATA");

                    heartRateList = (ArrayList<String>) heartRateData.clone();
                    accelerometer = (ArrayList<ArrayList<String>>) accelerometerData.clone();
                    sendDeviceInfo();
                    Log.d("heartRateList", "Heart Rate Data222: " + heartRateList.toString());
                    Log.d("accelerometer", "acc Rate Data222: " + accelerometer.toString());
                }
            }
        }
    };




    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(dataReceiver);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_start_activity_main);

        // BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter("ACTION_DATA_RECEIVED");
        registerReceiver(dataReceiver, filter);



        context = getApplicationContext();
        serverConnectionService = new Intent(this, ServerConnectionService.class);
        measureService = new Intent(this, MeasureService.class);
        server_connection_button = findViewById(R.id.server_connection_button);

        //연결 버튼 눌렀을 때
        server_connection_button.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(TAG, "startForegroundService");
                startForegroundService(measureService);
            } else {
                Log.d(TAG, "startService");
                startService(measureService);
            }

            SharedObjects.Base_URL = "http://" + SharedObjects.serverIp + ":" + SharedObjects.serverPort;

            MainActivity.retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(SharedObjects.Base_URL)
                    .build();

            MainActivity.retroApiService = MainActivity.retrofit.create(ApiService.class);

            sendDeviceInfo();

        });


    }

    //서버로 디바이스 정보를 전달하는 함수
    public void sendDeviceInfo() {
        HashMap<String, Object> values = new HashMap<String, Object>();

        values.put("heartRate", SharedObjects.heartRateList);
        values.put("accelerometer", SharedObjects.accelerometer);
        WatchDataModel data = new WatchDataModel(values);
        Log.d(TAG, values.toString());
        SharedObjects.retroClient.postDeviceInfo(values, new RetroCallback() {
                    //500
                    @Override
                    public void onError(Throwable t) {
                        stopService(serverConnectionService);
                        ServerConnectionService.isServerConnecting = false;
                        ((MainActivity) MainActivity.context).setServerState(false, SharedObjects.watchName);
                        finish();
//
                    }
//
                    @Override
                    public void onSuccess(int code, Object receivedData) {
                        ServerConnectionService.isServerConnecting = true;
                        startForegroundService(serverConnectionService);
                        finish();
                    }
//
                    @Override
                    public void onFailure(int code) {
                        stopService(serverConnectionService);
                        ServerConnectionService.isServerConnecting = false;
                        ((MainActivity) MainActivity.context).setServerState(false, SharedObjects.watchName);
                        finish();
                    }
//
                }
        );
    }
}