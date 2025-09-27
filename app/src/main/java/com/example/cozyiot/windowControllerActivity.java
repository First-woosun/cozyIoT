package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.*;

import com.example.cozyiot.func.MQTTDataFunc;
import com.example.cozyiot.func.MqttConnector;
import com.example.cozyiot.foreGroundService;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class windowControllerActivity extends AppCompatActivity {

    private static SharedPreferences preferences;
    private static SharedPreferences auto;
//    private static SharedPreferences windowStatus;
//    private static SharedPreferences.Editor windowEditor;
//    private static SharedPreferences.Editor editor;
    private static String userName;
    private static String userPassword;
    private static String IPAddress;
    private static String autoFlag;

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

    //MQTT 클라이언트가 연결되어 있는지 확인하는 flag(추후 수정)
    //TODO 서버에 저장된 flag값을 읽어와 저장하도록 수정
    private static boolean isConnect;

    //창문의 현재 상태를 파악하는 flag (추후 수정)
    //TODO 서버에 저장된 flag값을 읽어와 저장하도록 수정
    private static String isopen;

    //관리자 게정 여부
    private static  boolean adminFlag;

    //습도 데이터 처리를 위한 멀티스레드 작동 flag
    private static boolean multiThreadRun;

    private static String huminity;
    private static String temperature;;
    private boolean moving = false;  // 또는 false
    private static MqttConnector controllerConnector;
    private static MQTTDataFunc connector;

    private int dpToPx(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_window_controller);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        auto = getSharedPreferences("auto", MODE_PRIVATE);
//        editor = auto.edit();

        Intent intent = getIntent();
        adminFlag = intent.getBooleanExtra("admin", false);
        openBtn = findViewById(R.id.btn_open);
        closeBtn = findViewById(R.id.btn_close);
        windowState = findViewById(R.id.window_state);
        huminityView = findViewById(R.id.huminity_view);
        backBtn = findViewById(R.id.btn_back);
        autoSwitch = findViewById(R.id.switch_auto);


        if(adminFlag){
            userName = "cozydow";
            userPassword = "1234";
            IPAddress = "218.49.196.80:1883";
        } else {
            if(!preferences.getAll().isEmpty()){
                userName = preferences.getString("userName", "");
                userPassword = preferences.getString("userPassword", "");
                IPAddress = preferences.getString("IPAddress", "");
            } else {
                Toast.makeText(this, "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

//        isConnect = controllerConnector.connect();
        connector = new MQTTDataFunc(IPAddress, userName, userPassword);

//        if (foreGroundService.auto != null) {
//            controllerConnector = foreGroundService.auto;
//            isConnect = controllerConnector.connect();
//        } else {
//            foreGroundService.makeConnect(IPAddress,userName,userPassword);
//            controllerConnector = foreGroundService.auto;
//            isConnect = controllerConnector.connect();
//        }

        if(connector.callData("auto_run").equals("success")){
            autoFlag = connector.getData("pico/auto_run");
        }

        try {
            if(autoFlag.equals("true")){
                autoSwitch.setChecked(true);
            } else if(autoFlag.equals("false")){
                autoSwitch.setChecked(false);
            } else {
                Log.d("auto", "ERROR");
            }
        } catch (NullPointerException nullPointerException) {
            autoSwitch.setChecked(false);
        }

        startHuminityThread();

//        windowStatus = getSharedPreferences("windowPrefs", MODE_PRIVATE);
//        windowEditor = windowStatus.edit();
        if(connector.callData("window_status").equals("success")){
            isopen = connector.getData("window_status");
        }

        if(isopen.equals("close")){
            if(moving == false){
                set_status_window("default");
            }

        } else {
            if(moving == false){
                set_status_window("open");
            }
        }


        //창문 자동 제어
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent serviceIntent = new Intent(windowControllerActivity.this, foreGroundService.class);
                if(isChecked){
                    String topic = "pico/auto_run";
                    String message = "true";
                    connector.pushData(message, topic);
//                    editor.putString("auto", "true");
//                    editor.apply();
//                    isConnect = foreGroundService.callDisconnect();
//                    multiThreadRun = false;
                    startService(serviceIntent);
                }else{
                    String topic = "pico/auto_run";
                    String message = "false";
                    connector.pushData(message, topic);
//                    controllerConnector.publish(topic, message);
//                    editor.putString("auto", "false");
//                    editor.apply();
                    try {
                        Log.d("sleep", "잠깐 자쇼 ㅋㅋ");
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stopService(serviceIntent);
//                    isConnect = controllerConnector.connect();
//                    startHuminityThread();
                    Log.d("manual", "conncet");
                }
            }
        });

        //창문 개방 버튼
        openBtn.setOnClickListener(v -> {
            if(isopen.equals("close")){
                String topic = "pico/motor_request";
                String message = "open";
                String result = connector.pushData(topic, message);
                if(result.equals("success")){
                    Toast.makeText(this, "창문을 개방합니다.", Toast.LENGTH_LONG).show();
                    if(moving == false){
                        set_status_window("open");
                    }
                } else {
                    Toast.makeText(this, "요청에 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "이미 창문이 열려있습니다.", Toast.LENGTH_LONG).show();
            }

//            if(isConnect){
//                if(!isopen){
//                    String topic = "window/motor_request";
//                    String message = "open";
//                    controllerConnector.publish(topic, message);
//                    isopen = true;
//                    Toast.makeText(this, "창문을 개방합니다.", Toast.LENGTH_SHORT).show();
//                    if(moving == false){
//                        set_status_window("open");
//                    }
//
//
//                    windowEditor.putBoolean("status", true);
//                } else {
//                    Toast.makeText(this, "이미 창문이 열려있습니다.", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
//            }
        });

        //창문 닫기 버튼
        closeBtn.setOnClickListener(v ->{
            if(isopen.equals("open")){
                String topic = "pico/motor_request";
                String message = "close";
                String result = connector.pushData(topic, message);
                if(result.equals("success")){
                    Toast.makeText(this, "창문을 닫습니다.", Toast.LENGTH_LONG).show();
                    if(moving == false){
                        set_status_window("close");
                    }
                } else {
                    Toast.makeText(this, "요청에 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "이미 창문이 닫혀있습니다.", Toast.LENGTH_LONG).show();
            }

//            if(isConnect){
//                if(isopen){
//                    String topic = "window/motor_request";
//                    String message = "close";
//                    controllerConnector.publish(topic, message);
//                    Toast.makeText(this, "창문을 폐쇠합니다.", Toast.LENGTH_SHORT).show();
//                    if(moving == false){
//                        set_status_window("close");
//                    }
//                    windowEditor.putBoolean("status", false);
//                } else {
//                    Toast.makeText(this, "이미 창문이 닫혀있습니다.", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
//            }
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

    public static void reconnect(){
        controllerConnector.connect();
    }

    private void startHuminityThread() {
        Thread thread = new Thread(() -> {
            multiThreadRun = true;
            String JsonMessage;
            Log.i("multiThread", "start multiThread");
//            controllerConnector.subscribe("pico/dht22");

            while (multiThreadRun) {
                Log.i("thread", "run");
                if(connector.callData("dht22").equals("success")){
                    JsonMessage = connector.getData("dht22");
                    try {
                        JSONObject jsonObject = new JSONObject(JsonMessage);
                        temperature = jsonObject.getString("temp");
                        huminity = jsonObject.getString("hum") + "%";
//                        Log.i("threadcheck","humtemp");

                        runOnUiThread(() -> {
                            huminityView.setText(huminity);

                            // 습도 이미지 처리
                            ImageView humidityImage = findViewById(R.id.humidityImage);
                            int humValue = 0;
                            try {
                                humValue = Integer.parseInt(huminity.replace("%", ""));
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
                            float tempValue = 0f;
                            try {
                                tempValue = Float.parseFloat(temperature);
                            } catch (NumberFormatException e) {
                                tempValue = 0f;
                            }

                            temperatureView.setText(String.format("%.1f°C", tempValue));FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) temperatureImage.getLayoutParams();
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

                    } catch (JSONException e) {
                        runOnUiThread(() -> huminityView.setText("데이터 오류"));
                    } catch (NullPointerException e) {
                        runOnUiThread(() -> huminityView.setText("0%"));
                    }
                } else {
                    Log.e("dht22", "데이터 요청 에러");
                }
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.i("multiThread", "exit multiThread successfully");
        });
        thread.start();
    }

}