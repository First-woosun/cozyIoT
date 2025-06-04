package com.example.cozyiot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.cozyiot.func.MqttConnector;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class foreGroundService extends Service {

    private String apiKey = "45253cb5ee7d2cf08c1cc1d6b4a811d8";
    private String url;

    private Context context;

    private static final String TAG = "CozyIOT FOREGROUND";

    // Notification
    private static final int NOTI_ID = 1;

    public static MqttConnector auto;
    private float lat;
    private float lon;

    public foreGroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences userInfo = getSharedPreferences("UserInfo", MODE_PRIVATE);
        String Address = userInfo.getString("IPAddress", "");
        String Name = userInfo.getString("userName", "");
        String Password = userInfo.getString("userPassword", "");
        makeConnect(Address,Name,Password);

        SharedPreferences locationPrefs = getSharedPreferences("location_prefs", MODE_PRIVATE);
        lat = locationPrefs.getFloat("latitude", 0f);
        lon = locationPrefs.getFloat("longtitude", 0f);

        createNotification();
        mThread.start();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    private Thread mThread = new Thread("My Thread"){
        @Override
        public void run() {
            super.run();

            boolean weatherFlag = false;
            boolean huminityFlag = false;
            boolean temperatureFlag = false;

            url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=metric&lang=kr";

            auto.connect();
            auto.subscribe("pico/dht22");

            while (true){
                try {
                    URL requestUrl = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    JSONObject response = new JSONObject(result.toString());
                    
                    //자동제어에 필요한 데이터 필드
                    int weatherId = response.getJSONArray("weather").getJSONObject(0).getInt("id");
                    double temp = response.getJSONObject("main").getDouble("temp");
//                    String huminityValue = auto.getLatestMessage();
//                    float huminity = Float.parseFloat(huminityValue);

                    //날씨에 따른 창문 개방 여부
                    if (weatherId < 800 && weatherId >= 200) {
                        // 창문을 열면 안되는 날씨
                        weatherFlag = false;
                    }else {
                        // 창문을 열어도 되는 날씨
                        weatherFlag = true;
                    }

                    // 내부 습도에 따른 창문 개방 여부
//                    if (huminity < 50f || huminity > 60f){
//                        // 환기 필요
//                        huminityFlag = true;
//                    } else {
//                        // 환기 불필요
//                        huminityFlag = false;
//                    }
                    
                    // 자동제어 로직부
                    if (weatherFlag) {
                        auto.publish("window/motor_request", "open");
                    } else {
                        auto.publish("window/motor_request", "close");
                    }
                    
                    Thread.sleep(10000);

                } catch (Exception e) {
                    e.printStackTrace();
                    auto.disconnect();
                    windowControllerActivity.reconnect();
                    break;
                }
            }
        }
    };

    private void createNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("CozyIot");
        builder.setContentText("자동 제어 동작중");

        builder.setColor(Color.WHITE);

        Intent notificationIntent = new Intent(this, windowControllerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);  // 여기 수정됨

        builder.setContentIntent(pendingIntent); // 알림 클릭 시 이동

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }

        notificationManager.notify(NOTI_ID, builder.build());
        Notification notification = builder.build();
        startForeground(NOTI_ID, notification);
    }


//    private void createNotification() {
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
//
//        builder.setSmallIcon(R.mipmap.ic_launcher);
//        builder.setContentTitle("Foreground Service");
//        builder.setContentText("포그라운드 서비스");
//
//        builder.setColor(Color.WHITE);
//
//        Intent notificationIntent = new Intent(this, windowControllerActivity.class);
//        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//        builder.setContentIntent(pendingIntent); // 알림 클릭 시 이동
//
//        // 알림 표시
//        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
//        }
//
////        notificationManager.notify(NOTI_ID, builder.build()); // id : 정의해야하는 각 알림의 고유한 int값
////        Notification notification = builder.build();
//        notificationManager.notify(NOTI_ID, builder.build());
//        Notification notification = builder.build();
//        startForeground(NOTI_ID, notification);
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mThread != null){
            mThread.interrupt();
            mThread = null;
        }

        Log.d(TAG, "onDestroy");
    }

    public static  boolean callDisconnect(){
        auto.disconnect();
        return false;
    }
    public static boolean makeConnect(String Address, String Name, String Password) {
        auto = new MqttConnector(Address, Name, Password);
        return false;
    }
}