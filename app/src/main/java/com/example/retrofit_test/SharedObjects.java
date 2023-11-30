package com.example.retrofit_test;

import java.util.ArrayList;

public class SharedObjects {

    public static String serverIp = "192.168.1.38";
    public static String serverPort = "8800";

    public static String Base_URL = "http://"+ serverIp + ":" + serverPort;

    public static String toastContents = "Server Error";
    public static RetroClient retroClient = RetroClient.getInstance(MainActivity.ApplicationContext()).createBaseApi();
    public static String watchName="00";

    //Sensor
    public static ArrayList<ArrayList<String>> accelerometer = new ArrayList<>();
    public static ArrayList<String> timeStamp = new ArrayList<>();
    public static ArrayList<String> heartRateList = new ArrayList<>();


}
