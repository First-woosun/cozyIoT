package com.example.cozyiot;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.cozyiot.func.SynchronizedMqttConnector;

public class foreGroundService extends Service {

    private Context context;

    private static String mq_135;  // 공기질;
    private static String temp; // 온도
    private static String hum;  // 습도
    private static String bh1750;// 광량
    private static String rain;

    private static final String TAG = "CozyIOT FOREGROUND";

    // Foreground 알림
    private static final int NOTI_ID = 1;

    // 사용자 알람
    private static final int ALERT_ID = 2001;
    private static final long ALERT_COOLDOWN = 5 * 60 * 1000; // 5분
    private long lastNotificationTime = 0;

    private static SynchronizedMqttConnector connector;

    private static String windowStatusApp;
    private static String windowStatusNow;

    public foreGroundService() {}

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connector = SynchronizedMqttConnector.getInstance();

        createForegroundNotification();
        mThread.start();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        windowStatusApp = intent.getStringExtra("windowStatus");
        return super.onStartCommand(intent, flags, startId);
    }

    private Thread mThread = new Thread("My Thread") {
        @Override
        public void run() {
            super.run();

            while (true) {
                try {
                    Log.i("foreGroundService", "run");
                    connector.subscribe("pico", "temp");
                    connector.publish("pico", "temp");
                    connector.subscribe("pico","hum");
                    connector.publish("pico", "hum");
                    connector.subscribe("pico", "mq_135");
                    connector.publish("pico","mq_135");
                    connector.subscribe("pico", "bh1750");
                    connector.publish("pico", "bh1750");
                    connector.subscribe("pico", "rain");
                    connector.publish("pico", "rain");
                    connector.subscribe("pico", "window_status");
                    connector.publish("pico", "window_status");


                    Thread.sleep(200);

                    temp = connector.getLatestMessage("pico", "temp");
                    hum = connector.getLatestMessage("pico", "hum");
                    mq_135 = connector.getLatestMessage("pico", "mq_135");
                    bh1750 = connector.getLatestMessage("pico", "bh1750");
                    rain = connector.getLatestMessage("pico", "rain");

                    windowStatusNow = connector.getLatestMessage("pico", "window_status");

                    if(!windowStatusApp.equals(windowStatusNow)){
                        if(windowStatusNow.equals("Open") && windowStatusApp.equals("Close")){
                            triggerAlertNotification("창문 개방", "창문을 개방했습니다.");
                            windowStatusApp = windowStatusNow;
                        } else if (windowStatusNow.equals("Close") && windowStatusApp.equals("Open")) {
                            triggerAlertNotification("창문 폐쇄", "창문을 폐쇄했습니다.");
                            windowStatusApp = windowStatusNow;
                        }
                    }

                    if (!mq_135.isEmpty()) {
                        try {
                            long airValue = Long.parseLong(mq_135);
                            if (airValue >= 20000.0) {
                                triggerAlertNotification("공기질 경고!", "공기질 나쁨. 환기가 필요합니다. (현재 공기질 :"+airValue+")");
                            } else if (airValue >= 35000.0 ){
                                triggerAlertNotification("공기질 경고!", "공기질 매우 나쁨. 환기가 필요합니다. (현재 공기질 :"+airValue+")");
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "mq_135 파싱 오류: " + mq_135);
                        } catch (Exception e){
                            Log.e(TAG, "에러 발생");
                            e.printStackTrace();
                        }
                    }

                    if (!hum.isEmpty()){
                        try{
                            float humValue = Float.parseFloat(hum);
                            if(humValue >= 60){
                                triggerAlertNotification("습도 경고", "실내가 습합니다. 환기가 필요합니다. (현재 습도 : )"+humValue+")");
                            }
                        } catch (Exception e){
                            Log.e(TAG, "에러 발생");
                            e.printStackTrace();
                        }
                    }

                    if (!rain.isEmpty()){
                        try{
                            float rainValue = Float.parseFloat(rain);
                            if(rainValue > 0.5){
                                triggerAlertNotification("강우 감지", "강우가 감지되었습니다. 창문은 닫는것을 추천합니다.");
                            }
                        } catch (Exception e){
                            Log.e(TAG, "에러 발생");
                            e.printStackTrace();
                        }
                    }

                    Thread.sleep(300000); // 5분마다 측정

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    // Foreground Notification (항상 표시)
    private void createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("CozyIot");
        builder.setContentText("자동 제어 동작중");
        builder.setColor(Color.WHITE);

        Intent notificationIntent = new Intent(this, windowControllerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }

        Notification notification = builder.build();
        startForeground(NOTI_ID, notification);
    }

    // 사용자 경고 알림
    private void triggerAlertNotification(String title, String message) {
        long currentTime = System.currentTimeMillis();

        // 5분 쿨다운 적용
        if (currentTime - lastNotificationTime < ALERT_COOLDOWN) {
            Log.d(TAG, "쿨다운 중: 알림 생략");
            return;
        }
        lastNotificationTime = currentTime;

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "alert_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "경고 알림", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, windowControllerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Android 13 이상은 권한 체크 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "알림 권한이 없음");
                return;
            }
        }

        manager.notify(ALERT_ID, builder.build());
        Log.d(TAG, "알림 발생: " + title + " - " + message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }

        Log.d(TAG, "onDestroy");
    }
}
