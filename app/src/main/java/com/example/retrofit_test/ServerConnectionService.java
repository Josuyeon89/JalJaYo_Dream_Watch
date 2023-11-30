package com.example.retrofit_test;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//import com.example.retrofit_test.MainActivity;
//import com.example.retrofit_test.Model.WatchModel;
//import com.example.retrofit_test.Model.WatchNameModel;
//
//import com.example.retrofit_test.Retrofit.RetroCallback;
//import com.example.retrofit_test.SharedObjects;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ServerConnectionService extends Service {
    Timer timer;
    public static boolean  isServerConnecting = false;


    @Override
    public IBinder onBind(Intent intent) {

        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        timer = new Timer();
        foregroundNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        foregroundNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    void foregroundNotification() { // foreground 실행 후 신호 전달 (안하면 앱 강제종료 됨)
        NotificationCompat.Builder builder;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "server_connection_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Server Connection Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setContentTitle("서버 연결")
                .setContentIntent(pendingIntent);

        startForeground(2, builder.build());
    }
}