package com.example.retrofit_test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetroClient extends Activity {
    private static final String TAG = "Retrofit Client";
    private ApiService retroApiService;
    private static Context mContext;
    private static Retrofit retrofit;

    public static String baseUrl = SharedObjects.Base_URL;

    private static class SingletonHolder {
        private static RetroClient INSTANCE = new RetroClient(mContext);

    }

    public static RetroClient getInstance(Context context) {
        if (context != null) {
            mContext = context;
        }
        return SingletonHolder.INSTANCE;
    }

    public RetroClient(Context context) {
        baseUrl = SharedObjects.Base_URL;
        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .build();
    }

    public RetroClient createBaseApi() {
        retroApiService = create(ApiService.class);
        return this;
    }

    public <T> T create(final Class<T> service) {
        if (service == null) {
            throw new RuntimeException("Api service is null!");
        }
        return retrofit.create(service);
    }

    public void postDeviceInfo(HashMap<String, Object> values, RetroCallback callback) {
        MainActivity.retroApiService.postWatchDataModel(values).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "========== Success sendSensor retroClient==========");

                    callback.onSuccess(response.code(), response.body());
                    //measureService = new Intent(getApplicationContext(), MeasureService.class);
                } else {
                    Log.d(TAG, "========== not Success sendSensor ==========" + call);
                    callback.onFailure(response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.d(TAG, "========== Failure sendSensor ==========" + t.toString());
                callback.onError(t);
            }
        });
    }




}
