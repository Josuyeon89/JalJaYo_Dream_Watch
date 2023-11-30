package com.example.retrofit_test;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeasureService extends Service implements SensorEventListener {
    private static final String TAG = "MeasureService";
    private long startTime;
    private SensorManager manager;
    private Sensor[] mSensors;
    private static HashMap<Integer, Integer> sensorType;
    Timer sensorTimer;


    public static boolean isMeasuring;


    long before = System.currentTimeMillis();
    long after;
    static boolean start = true;


    static ArrayList<String> accelerometerData = new ArrayList<>(Arrays.asList("0.0", "0.0", "0.0"));;
    static String heartRateData = "0.0";


    static ArrayList<ArrayList<String>> accelerometerBuffer = new ArrayList<>();
    static ArrayList<String> heartRateBuffer = new ArrayList<>();
    ArrayList<String> timeStampBuffer = new ArrayList<>();
    String timeStamp = "";

    @Override
    public void onCreate() {
        super.onCreate();
        isMeasuring = true;
        startTime = System.currentTimeMillis();
        foregroundNotification();
        initSensors();
        sensorTimer = new Timer();
        startTimer();

    }


    @Override
    public void onDestroy() {
        isMeasuring = false;
        sensorTimer.cancel();
        unregister();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //실질적으로 측정을 시작하는 함수
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        //센서 값 가져오기
        measureSensor(sensor, event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    void foregroundNotification() { // foreground 실행 후 신호 전달 (안하면 앱 강제종료 됨)
        NotificationCompat.Builder builder;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "measuring_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Measuring Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setContentTitle("측정시작")
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());
    }

    // 사용할 센서 세팅
    private void initSensors() {
        sensorType = new HashMap<Integer, Integer>();
        sensorType.put(21, 1); // Heart_rate
        sensorType.put(1, 3); // Accelerometer
        manager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mSensors = new Sensor[sensorType.size()];

        Integer i = 0;
        Iterator<Map.Entry<Integer, Integer>> entries = sensorType.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, Integer> entry = entries.next();


            if (entry.getKey() == 1 || entry.getKey() == 4) {
                mSensors[i] = manager.getDefaultSensor(entry.getKey());
                manager.registerListener(this, mSensors[i], SensorManager.SENSOR_DELAY_GAME);
            } else {
                mSensors[i] = manager.getDefaultSensor(entry.getKey());
                manager.registerListener(this, mSensors[i], SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

    }


    public void unregister() { // unregister listener
        manager.unregisterListener(this);
    }

    ArrayList<String> accel = new ArrayList<>();

    public void measureSensor(Sensor sensor, SensorEvent event) {
        after = System.currentTimeMillis();
        long now = (after - before) / 1000;

        ArrayList<String> data = new ArrayList<>();
        if (sensorType.get(sensor.getType()) == 3) {
            data.add(Float.toString(event.values[0]));
            data.add(Float.toString(event.values[1]));
            data.add(Float.toString(event.values[2]));
        }

        //센서 값이 변할 때마다 변수에 저장
        if (sensor.getType() == sensor.TYPE_ACCELEROMETER) {
            accel.add(Float.toString(event.values[0]));
            accel.add(Float.toString(event.values[1]));
            accel.add(Float.toString(event.values[2]));

            accelerometerData = (ArrayList<String>) data.clone();
        } else if (sensor.getType() == sensor.TYPE_HEART_RATE) {
            heartRateData = Float.toString(event.values[0]);
            SharedObjects.accelerometer = (ArrayList<ArrayList<String>>) accelerometerBuffer.clone();
            SharedObjects.heartRateList = (ArrayList<String>) heartRateBuffer.clone();
            ArrayList<Float> timeGap = new ArrayList<>();

            accelerometerBuffer.clear();
            heartRateBuffer.clear();
            if(start) {
            Log.d(TAG, "===========================  " + count + "초 후 ============================ ");
            Log.d(TAG, "-------------------- timestamp --------------------");
            Log.d(TAG, SharedObjects.timeStamp.size() + SharedObjects.timeStamp.toString());
            Log.d(TAG, "=-------------------- accelerometer --------------------");
            Log.d(TAG, SharedObjects.accelerometer.size() + SharedObjects.accelerometer.toString());
            Log.d(TAG, "-------------------- heartRateList --------------------");


                sendSensor(new RetroCallback() {
                    @Override
                    public void onError(Throwable t) {
                        Log.d(TAG, "onError: " + t);
                    }

                    //
                    @Override
                    public void onSuccess(int code, Object receivedData) {
                        Log.d(TAG, "onSuccess: " + code);
//                    ResponseGet data = (ResponseGet) receivedData;
                    }

                    //
                    @Override
                    public void onFailure(int code) {
                        Log.d(TAG, "BAD Request: " + code);
                    }
//
                });

            count = count + now;
            before = after;
        }
        }
    }
    float count = 0;

    private void startTimer() {

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                long date = System.currentTimeMillis();
                Date mDate = new Date(date);
                SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                timeStamp = sdfNow.format(mDate);

                //50hz로 버퍼에 센서 데이터 저장
                accelerometerBuffer.add(accelerometerData);

                timeStampBuffer.add(timeStamp);
                heartRateBuffer.add(heartRateData);
            }
        };

        sensorTimer.schedule(task, 0, 20); //0.020초마다 실행 -> 약 50Hz

    }

    public void sendSensor(RetroCallback callback) {
        HashMap<String, Object> values = new HashMap<String, Object>();

        values.put("heartRate", SharedObjects.heartRateList);
        values.put("accelerometer", SharedObjects.accelerometer);

        WatchDataModel data = new WatchDataModel(values);
        // Thread 풀 생성
        ExecutorService executor = Executors.newCachedThreadPool();

        // Thread 실행 작업 정의
        executor.submit(() -> {
            MainActivity.retroApiService.postWatchDataModel(values).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "========== Success sendSensor==========");
                    } else {
                        Log.d(TAG, "========== not Success sendSensor ==========" + call);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.d(TAG, "========== Failure sendSensor ==========" + t.toString());
                    callback.onError(t);
                }
            });
        });


    }
}
