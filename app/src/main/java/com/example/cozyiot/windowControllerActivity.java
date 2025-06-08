package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.*;
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
    private static SharedPreferences windowStatus;
    private static SharedPreferences.Editor windowEditor;
    private static SharedPreferences.Editor editor;
    private static String userName;
    private static String userPassword;
    private static String IPAddress;

    //안드로이드 세그먼트 선언
    Button openBtn; Button closeBtn; Button btnConfirmLocation;
    Switch autoSwitch;
    ImageView windowState;
    TextView huminityView, backBtn, weatherView;

    //MQTT 클라이언트가 연결되어 있는지 확인하는 flag(추후 수정)
    //TODO 서버에 저장된 flag값을 읽어와 저장하도록 수정
    private static boolean isConnect;

    //창문의 현재 상태를 파악하는 flag (추후 수정)
    //TODO 서버에 저장된 flag값을 읽어와 저장하도록 수정
    private static boolean isopen;

    //관리자 게정 여부
    private static  boolean adminFlag;

    //습도 데이터 처리를 위한 멀티스레드 작동 flag
    private static boolean multiThreadRun;

    private static String huminity;
    private static String temperature;;
    private boolean moving = false;  // 또는 false
    private static MqttConnector controllerConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_window_controller);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        auto = getSharedPreferences("auto", MODE_PRIVATE);
        editor = auto.edit();

        Intent intent = getIntent();
        adminFlag = intent.getBooleanExtra("admin", false);
        openBtn = findViewById(R.id.btn_open);
        closeBtn = findViewById(R.id.btn_close);
        windowState = findViewById(R.id.window_state);
        huminityView = findViewById(R.id.huminity_view);
        backBtn = findViewById(R.id.btn_back);
        autoSwitch = findViewById(R.id.switch_auto);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        weatherView = findViewById(R.id.weather_view);

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


        if (foreGroundService.auto != null) {
            controllerConnector = foreGroundService.auto;
            isConnect = controllerConnector.connect();
        } else {
            foreGroundService.makeConnect(IPAddress,userName,userPassword);
            controllerConnector = foreGroundService.auto;
            isConnect = controllerConnector.connect();
        }

        controllerConnector.subscribe("window/auto_motor_request");
        String autoFlag = auto.getString("auto", "false");
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

        windowStatus = getSharedPreferences("windowPrefs", MODE_PRIVATE);
        windowEditor = windowStatus.edit();
        isopen = windowStatus.getBoolean("status", false);

        if(!isopen){
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
                    String topic = "window/auto_motor_request";
                    String message = "true";
                    controllerConnector.publish(topic, message);
                    editor.putString("auto", "true");
                    editor.apply();
                    isConnect = foreGroundService.callDisconnect();
//                    multiThreadRun = false;
                    startService(serviceIntent);
                }else{
                    String topic = "window/auto_motor_request";
                    String message = "false";
                    controllerConnector.publish(topic, message);
                    editor.putString("auto", "false");
                    editor.apply();
                    try {
                        Log.d("sleep", "잠깐 자쇼 ㅋㅋ");
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stopService(serviceIntent);
                    isConnect = controllerConnector.connect();
//                    startHuminityThread();
                    Log.d("manual", "conncet");
                }
            }
        });

        //창문 개방 버튼

        openBtn.setOnClickListener(v -> {
            if(isConnect){
                if(!isopen){
                    String topic = "window/motor_request";
                    String message = "open";
                    controllerConnector.publish(topic, message);
                    isopen = true;
                    Toast.makeText(this, "창문을 개방합니다.", Toast.LENGTH_SHORT).show();
                    if(moving == false){
                        set_status_window("open");
                    }


                    windowEditor.putBoolean("status", true);
                } else {
                    Toast.makeText(this, "이미 창문이 열려있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        closeBtn.setOnClickListener(v ->{
            if(isConnect){
                if(isopen){
                    String topic = "window/motor_request";
                    String message = "close";
                    controllerConnector.publish(topic, message);
                    Toast.makeText(this, "창문을 폐쇠합니다.", Toast.LENGTH_SHORT).show();
                    if(moving == false){
                        set_status_window("close");
                    }
                    windowEditor.putBoolean("status", false);
                } else {
                    Toast.makeText(this, "이미 창문이 닫혀있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //뒤로가기 버튼
        backBtn.setOnClickListener(v ->{
            multiThreadRun = false;
//            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnConfirmLocation.setOnClickListener(v -> {
            Intent mapIntent = new Intent(windowControllerActivity.this, MapActivity.class);
            startActivity(mapIntent);
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
                                isopen = true;
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
                                isopen = false;
                                moving = false;  // 3초 애니메이션 끝난 후 실행
                            }
                        })
                        .start();
            }
        }
        moving = true;
    }
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences locationPrefs = getSharedPreferences("location_prefs", MODE_PRIVATE);
        float latitude = locationPrefs.getFloat("latitude", 0f);
        float longitude = locationPrefs.getFloat("longitude", 0f);

        if (latitude != 0f && longitude != 0f) {
            loadWeatherFromSavedLocation(latitude, longitude);
        } else {
            weatherView.setText("위치 정보가 없습니다.");
        }
    }

    private void loadWeatherFromSavedLocation(float lat, float lon) {
        String apiKey = "45253cb5ee7d2cf08c1cc1d6b4a811d8";
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon +
                "&appid=" + apiKey +
                "&units=metric&lang=kr";

        new Thread(() -> {
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
                String weather = response.getJSONArray("weather").getJSONObject(0).getString("description");
                double temp = response.getJSONObject("main").getDouble("temp");
//                String location = response.getJSONArray("city").getJSONObject(0).getString("description");

                String finalText = "날씨: " + weather + "\n온도: " + temp + "°C";

                runOnUiThread(() -> weatherView.setText(finalText));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> weatherView.setText("날씨 정보를 불러올 수 없습니다."));
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        foreGroundService.callDisconnect();
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
            Log.i("multiThread", "start multiThread");
            controllerConnector.subscribe("pico/dht22");

            while (multiThreadRun) {
                Log.i("thread", "run");
                String JsonMessage = controllerConnector.getLatestMessage();

                try {
                    JSONObject jsonObject = new JSONObject(JsonMessage);
                    temperature = jsonObject.getString("temp");
                    huminity = jsonObject.getString("hum") + "%";

                    runOnUiThread(() -> huminityView.setText(huminity));
                } catch (JSONException e) {
                    runOnUiThread(() -> huminityView.setText("데이터 오류"));
                } catch (NullPointerException e) {
                    runOnUiThread(() -> huminityView.setText("0%"));
                }

                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.i("multiThread", "exit multiThread successfully");
        });
        thread.start();
    }

}