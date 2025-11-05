package com.example.cozyiot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.*;

import com.example.cozyiot.func.SynchronizedMqttConnector;

import java.util.Objects;

public class windowControllerActivity extends AppCompatActivity {

    private final int[] tempColors = {
            Color.parseColor("#f88f59"),
            Color.parseColor("#f97247"),
            Color.parseColor("#fb5635"),
            Color.parseColor("#fc3924"),
            Color.parseColor("#fe1d12"),
            Color.parseColor("#ff0000"),
    };
    private final int[] waterColors = {
            Color.parseColor("#94d1e6"),
            Color.parseColor("#5fb3d9"),
            Color.parseColor("#2a96cc"),
            Color.parseColor("#0a78bf"),
            Color.parseColor("#000080"),
    };

    //안드로이드 세그먼트 선언
    Button openBtn; Button closeBtn;
    Switch autoSwitch;
    ImageView windowState;
    TextView huminityView, backBtn;

    //창문의 현재 상태를 파악하는 flag (추후 수정)
    private static String windowStatus;

    private static String isAuto;

    //습도 데이터 처리를 위한 멀티스레드 작동 flag
    private static boolean multiThreadRun;

    private static String huminity;
    private static String temperature;;
    private boolean moving = false;  // 또는 false
    private static SynchronizedMqttConnector connector;

    private int dpToPx(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_window_controller);

        openBtn = findViewById(R.id.btn_open);
        closeBtn = findViewById(R.id.btn_close);
        windowState = findViewById(R.id.window_state);
        huminityView = findViewById(R.id.huminity_view);
        backBtn = findViewById(R.id.btn_back);
        autoSwitch = findViewById(R.id.switch_auto);

        connector = SynchronizedMqttConnector.getInstance();

        startHuminityThread();

        connector.subscribe("pico", "window_status");
        connector.publish("pico", "window_status");
        connector.subscribe("pico", "motor_status");
        connector.subscribe("pico", "auto_run");
        connector.publish("pico", "auto_run");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        windowStatus = connector.getLatestMessage("pico", "window_status");
        isAuto = connector.getLatestMessage("pico", "auto_run");
        Log.i("status", windowStatus);

        if(windowStatus.equals("\"Close\"")){
            if(!moving){
                set_status_window("default");
            }

        } else {
            if(!moving){
                set_status_window("open");
            }
        }

        autoSwitch.setChecked(isAuto.equals("open"));

        //창문 자동 제어
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent serviceIntent = new Intent(windowControllerActivity.this, foreGroundService.class);
                serviceIntent.putExtra("windowStatus", windowStatus);
                if(isChecked){
                    connector.autoMotorRequestPublish("true");
                    startService(serviceIntent);
                }else{
                    connector.autoMotorRequestPublish("false");
                    stopService(serviceIntent);
                }
            }
        });

        //창문 개방 버튼
        openBtn.setOnClickListener(v -> {
            connector.publish("pico", "motor_status");
            connector.publish("pico", "window_status");
            String motorStatus = null;
            try {
                while (motorStatus == null){
                    motorStatus = connector.getLatestMessage("pico", "motor_status");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            windowStatus = connector.getLatestMessage("pico", "window_status");
            Log.i("status", motorStatus);
            if(motorStatus.equals("\"False\"")){
                if(windowStatus.equals("\"Close\"")){
                    String topic = "pico/motor_request";
                    String message = "Open";
                    connector.publish(topic, message);
                    Toast.makeText(this, "창문을 개방합니다.", Toast.LENGTH_SHORT).show();
                    if(moving == false){
                        set_status_window("open");
                    }
                } else {
                    Toast.makeText(this, "이미 창문이 열려있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "현재 창문이 동작중입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //창문 닫기 버튼
        closeBtn.setOnClickListener(v ->{
            connector.publish("pico", "motor_status");
            connector.publish("pico", "window_status");
            String motorStatus = null;
            try {
                while (motorStatus == null){
                    motorStatus = connector.getLatestMessage("pico", "motor_status");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            windowStatus = connector.getLatestMessage("pico", "window_status");
            Log.i("status", motorStatus);
            if(motorStatus.equals("\"False\"")){
                if(windowStatus.equals("\"Open\"")){
                    String topic = "pico/motor_request";
                    String message = "Close";
                    connector.publish(topic, message);
                    Toast.makeText(this, "창문을 닫습니다.", Toast.LENGTH_SHORT).show();
                    if(moving == false){
                        set_status_window("close");
                    }
                } else {
                    Toast.makeText(this, "이미 창문이 닫혀있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "창문이 동작중입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //뒤로가기 버튼
        backBtn.setOnClickListener(v ->{
            multiThreadRun = false;
//            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

    }
    private void set_status_window(String status) {
        if (windowState != null) {
            if (status == "open") {
                float deltaX = -250f; // 오른쪽으로 200픽셀 이동
                windowState.animate()
                        .translationXBy(deltaX)
                        .setDuration(3000)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
//                                isopen = "open";
                                moving = false;  // 3초 애니메이션 끝난 후 실행
                            }
                        })
                        .start();
            } else if (status == "default") {
                return; // 아무것도 안 함
            } else {
                float deltaX = 250f; // 왼쪽으로 200픽셀 이동
                windowState.animate()
                        .translationXBy(deltaX)
                        .setDuration(3000)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
//                                isopen = "close";
                                moving = false;  // 3초 애니메이션 끝난 후 실행
                            }
                        })
                        .start();
            }
        }
        moving = true;
    }


    @Override
    public void onBackPressed() {
//        foreGroundService.callDisconnect();
        multiThreadRun = false;
//        startActivity(new Intent(this, HomeActivity.class));
        finish();
        super.onBackPressed();
    }

    private void startHuminityThread() {
        Thread thread = new Thread(() -> {
            multiThreadRun = true;
            Log.i("multiThread", "start multiThread");

            while (multiThreadRun) {
                connector.subscribe("pico", "temp");
                connector.publish("pico", "temp");
                connector.subscribe("pico", "hum");
                connector.publish("pico", "hum");

                try {
                    runOnUiThread(() -> {
                        String currentHumidity = connector.getLatestMessage("pico", "hum")+"%";
                        huminityView.setText(currentHumidity);

                        // 습도 이미지 처리
                        ImageView humidityImage = findViewById(R.id.humidityImage);
                        int humValue = 0;
                        try {
                            humValue = Integer.parseInt(currentHumidity.replace("%", ""));
                        } catch (NumberFormatException e) {
                            humValue = 0;
                        }

                        if (humValue < 10) {
                            humidityImage.setImageResource(R.drawable.sup1); // 낮음
                            humidityImage.setBackgroundColor(waterColors[0]);
                            huminityView.setTextColor(waterColors[0]);
                        } else if (humValue < 20) {
                            humidityImage.setImageResource(R.drawable.sup2);
                            humidityImage.setBackgroundColor(waterColors[1]);
                            huminityView.setTextColor(waterColors[1]);
                        } else if (humValue < 40) {
                            humidityImage.setImageResource(R.drawable.sup3);
                            humidityImage.setBackgroundColor(waterColors[2]);
                            huminityView.setTextColor(waterColors[2]);
                        } else if (humValue < 60) {
                            humidityImage.setImageResource(R.drawable.sup4);
                            humidityImage.setBackgroundColor(waterColors[3]);
                            huminityView.setTextColor(waterColors[3]);
                        } else {
                            humidityImage.setImageResource(R.drawable.sup5); // 높음
                            humidityImage.setBackgroundColor(waterColors[4]);
                            huminityView.setTextColor(waterColors[4]);
                        }

                        // 온도 텍스트 및 이미지 처리 추가
                        TextView temperatureView = findViewById(R.id.temperature_view); // 온도 표시용 TextView (레이아웃에 있어야 함)
                        ImageView temperatureImage = findViewById(R.id.temperatureImage); // 온도 이미지 표시용 ImageView (레이아웃에 있어야 함)
                        ImageView overlayImage = findViewById(R.id.overlayImage);
                        FrameLayout frameLayout = findViewById(R.id.temperatureImageframe);
                        String currentTemperature = connector.getLatestMessage("pico", "temp");

                        float tempValue = 0f;
                        try {
                            tempValue = Float.parseFloat(currentTemperature);
                        } catch (NumberFormatException e) {
                            tempValue = 0f;
                        }

                        temperatureView.setText(String.format("%.1f°C", tempValue));
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) temperatureImage.getLayoutParams();
                        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) overlayImage.getLayoutParams();

                        if (tempValue <= 10) {
                            param.height = dpToPx(45);
                            frameLayout.setBackgroundColor(tempColors[0]);
                            temperatureView.setTextColor(tempColors[0]);
                        } else if (tempValue <= 15) {
                            param.height = dpToPx(35);
                            frameLayout.setBackgroundColor(tempColors[1]);
                            temperatureView.setTextColor(tempColors[1]);
                        } else if (tempValue <= 20) {
                            param.height = dpToPx(25);
                            frameLayout.setBackgroundColor(tempColors[2]);
                            temperatureView.setTextColor(tempColors[2]);
                        } else if (tempValue <= 25) {
                            param.height = dpToPx(15);
                            frameLayout.setBackgroundColor(tempColors[3]);
                            temperatureView.setTextColor(tempColors[3]);;
                        } else if (tempValue <= 30) {
                            param.height = dpToPx(5);
                            frameLayout.setBackgroundColor(tempColors[4]);
                            temperatureView.setTextColor(tempColors[4]);
                        } else {
                            param.height = dpToPx(0);
                            frameLayout.setBackgroundColor(tempColors[5]);
                            temperatureView.setTextColor(tempColors[5]);
                        }

                        temperatureImage.setLayoutParams(params);
                    });

                } catch (NullPointerException e) {
                    runOnUiThread(() -> huminityView.setText("0%"));
                }

                try {
                    Thread.sleep(200000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.i("multiThread", "exit multiThread successfully");
        });
        thread.start();
    }

}