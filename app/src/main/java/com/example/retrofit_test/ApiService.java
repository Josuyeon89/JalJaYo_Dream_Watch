package com.example.retrofit_test;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // 해당 URL요청 -> 리소스 생성, BODY에 전송할 데이터를 담아 서버에 생성
    @POST("/watch/sleepData")
    Call<Void> postWatchDataModel(@Body HashMap<String, Object> data);

}
