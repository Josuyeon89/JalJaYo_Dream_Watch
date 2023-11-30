package com.example.retrofit_test;

import java.util.HashMap;
import java.util.List;

public class WatchDataModel {
    public final List<String> heartRate; //[값1, 값2, 값3, ---]
    public final List<List<String>> accelerometer;  // //[[x1,y1,z1], [x2,y2,z2],[x3,y3,z3], ---]




    public WatchDataModel(HashMap<String, Object> params) {
        this.heartRate = (List<String>) params.get(1);
        this.accelerometer = (List<List<String>>) params.get(2);

    }

}
