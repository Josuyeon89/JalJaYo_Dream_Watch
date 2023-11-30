package com.example.retrofit_test;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.retrofit_test.databinding.ActivityMainBinding;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {

    private PowerManager.WakeLock wakeLock;
    private Button server_connection_button, sensors_measurement_button;
    private ActivityMainBinding binding;
    public static Context context;
    public static Retrofit retrofit;
    private Intent measureService, serverConnectionService;
    public static ApiService retroApiService;
    private static final int REQUEST_BLUETOOTH_ADVERTISE_PERMISSION = 1;
    private boolean isMeasuring = false; // 측정 상태를 나타내는 변수


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakeLockTag");
        wakeLock.acquire();

        // BLUETOOTH_ADVERTISE 권한이 있는지 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한을 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},
                    REQUEST_BLUETOOTH_ADVERTISE_PERMISSION);
        } else {
            // 권한이 있는 경우 Bluetooth 광고를 시작하고 관리하는 코드를 실행
            startBluetoothAdvertising();
        }

        // 앱 추가 권한 설정
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BODY_SENSORS, android.Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Main activity context 가져오기
        context = this;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent intent;

        if (mBluetoothAdapter.isEnabled()) {
        } else {
            intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
        }

        // UI 바인딩
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        server_connection_button = binding.serverConnectionButton;

        // 서비스 클래스 인텐트 불러오기
        measureService = new Intent(this, MeasureService.class);
        serverConnectionService = new Intent(this, ServerConnectionService.class);

        //Retrofit 객체 생성
        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(SharedObjects.Base_URL)
                .build();

        retroApiService = retrofit.create(ApiService.class);

        server_connection_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBeacon();
                measureService.putExtra("isMeasuring", true);
                ContextCompat.startForegroundService(context, measureService);
                Intent intent = new Intent(getApplicationContext(), ServerConnectionActivity.class);
                startActivity(intent);
                }

        });
    }


    // 액티비티 종료 시에 WakeLock 해제
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    // 권한 요청 결과를 처리하는 메서드입니다.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_ADVERTISE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 Bluetooth 광고를 시작하고 관리하는 코드를 실행합니다.
                startBluetoothAdvertising();
            } else {
                // 권한이 거부된 경우 처리할 내용을 추가합니다.
            }
        }
    }

    // Bluetooth 광고를 시작하고 관리하는 메서드입니다.
    private void startBluetoothAdvertising() {
    }

    //다시 시작될 경우
    @Override
    protected void onResume() {
        super.onResume();

        if (ServerConnectionService.isServerConnecting) {
        } else {
            isMeasuring = false;

        }
        if (isMeasuring) {
        } else {
            stopService(measureService);
        }


    }


    //서버 연결 상태를 확인하고, 화면에 보여주는 함수
    public void setServerState(boolean state, String watchName) {
        if (!state) {
            if (SharedObjects.toastContents != "Server Error") {
            }
            SharedObjects.toastContents = "Server Error";

            //측정 중단
            if (isMeasuring) {
                measureService.putExtra("isMeasuring", false);
                isMeasuring = false;
                stopService(measureService);
            }

        } else {
            if (SharedObjects.toastContents != "Server Connect") {
                Toast.makeText(MainActivity.this, "[서버 연결 성공]", Toast.LENGTH_SHORT).show();
            }
            SharedObjects.toastContents = "Server Connect";
            SharedObjects.watchName = watchName;
        }
    }

    private void startBeacon() {
        // iBeacon UUID, Major 및 Minor 값을 설정
        String uuid = "EEBFB6CB-AE58-4992-A82F-D7494992E5AD";
        String major = "00";
        String minor = SharedObjects.watchName;
        Log.d(TAG, uuid);
        Log.d(TAG, String.valueOf(major));

        // BeaconParser 설정
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25=B3");

        // Beacon 생성
        Beacon beacon = new Beacon.Builder()
                .setId1(uuid)
                .setId2(major)
                .setId3(minor)
                .setManufacturer(0x004C) // Apple's manufacturer ID
                .setTxPower(-59) // Measured power in dBm
                .setDataFields(Arrays.asList(new Long[]{0L}))
                .build();

        // BeaconTransmitter를 사용하여 iBeacon을 방송
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.setAdvertiseMode(1);
        beaconTransmitter.startAdvertising(beacon);
    }


    public static Context ApplicationContext() {
        return MainActivity.context;
    }

}
