package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.*;
import com.example.cozyiot.func.MqttConnector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class windowControllerActivity extends AppCompatActivity {

    private static SharedPreferences preferences;
    private static SharedPreferences auto;
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
<<<<<<< Updated upstream

    private MqttConnector controllerConnector;

=======
    private boolean moving = false;  // 또는 false

    private int dpToPx(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
        controllerConnector = new MqttConnector(IPAddress, userName, userPassword);
        isConnect = controllerConnector.connect();
=======

        if (foreGroundService.auto == null) {
            foreGroundService.makeConnect(IPAddress,userName,userPassword);
            isConnect = foreGroundService.callconnect();
            foreGroundService.subwindow();
        }
        else{isConnect = foreGroundService.callconnect();}

>>>>>>> Stashed changes

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

        if(isConnect){
            Thread huminityThread = new Thread(() -> {
                multiThreadRun = isConnect;
                Log.i("multiThread", "start multiThread");
                while (multiThreadRun) {
                    controllerConnector.subscribe("pico/dht22");
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
            huminityThread.start(); // 새 스레드를 시작
        }

        if(!isopen){
            windowState.setImageResource(R.drawable.window_status_close);
        } else {
            windowState.setImageResource(R.drawable.window_status_open);
        }


        //창문 자동 제어
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent serviceIntent = new Intent(windowControllerActivity.this, foreGroundService.class);
                if(isChecked){
                    String topic = "window/auto_motor_request";
                    String message = "true";
                    foreGroundService.auto.publish(topic, message);
                    editor.putString("auto", "true");
<<<<<<< Updated upstream
                    editor.apply();
                    isConnect = controllerConnector.disconnect();
=======
                    editor.apply();  //자동 모드 활성화 하여 저장
//                    multiThreadRun = false;
>>>>>>> Stashed changes
                    startService(serviceIntent);
                }else{
                    String topic = "window/auto_motor_request";
                    String message = "false";
                    foreGroundService.auto.publish(topic, message);
                    editor.putString("auto", "false");
                    editor.apply();
                    stopService(serviceIntent);
<<<<<<< Updated upstream
                    isConnect = controllerConnector.connect();
=======
//                    startHuminityThread();
                    Log.d("manual", "conncet");
>>>>>>> Stashed changes
                }
            }
        });

        //창문 개방 버튼

        openBtn.setOnClickListener(v -> {
            if(isConnect){
                if(!isopen){
                    String topic = "window/motor_request";
                    String message = "open";
                    foreGroundService.auto.publish(topic, message);
                    isopen = true;
                    Toast.makeText(this, "창문을 개방합니다.", Toast.LENGTH_SHORT).show();
                    windowState.setImageResource(R.drawable.window_status_open);
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
<<<<<<< Updated upstream
                    controllerConnector.publish(topic, message);
                    isopen = false;
=======
                    foreGroundService.auto.publish(topic, message);
>>>>>>> Stashed changes
                    Toast.makeText(this, "창문을 폐쇠합니다.", Toast.LENGTH_SHORT).show();
                    windowState.setImageResource(R.drawable.window_status_close);
                } else {
                    Toast.makeText(this, "이미 창문이 닫혀있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //뒤로가기 버튼
        backBtn.setOnClickListener(v ->{
            controllerConnector.disconnect();
            multiThreadRun = false;
//            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnConfirmLocation.setOnClickListener(v -> {
            Intent mapIntent = new Intent(windowControllerActivity.this, MapActivity.class);
            startActivity(mapIntent);
        });
    }
<<<<<<< Updated upstream

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
=======
    private void reconnectAfterDelay(long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isConnect = foreGroundService.auto.connect();
            Log.d("manual", "connect 재연결됨");
        }, delayMillis);
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
>>>>>>> Stashed changes
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
                String location = response.getJSONArray("city").getJSONObject(0).getString("description");

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
        controllerConnector.disconnect();
        multiThreadRun = false;
//        startActivity(new Intent(this, HomeActivity.class));
        finish();
        super.onBackPressed();
    }
<<<<<<< Updated upstream
=======

    public static void reconnect(){
        foreGroundService.auto.connect();
    }

    private void startHuminityThread() {
        Thread thread = new Thread(() -> {
            multiThreadRun = true;
            Log.i("multiThread", "start multiThread");

            while (multiThreadRun) {
                Log.i("thread", "run");
                String JsonMessage = foreGroundService.auto.getLatestMessage("pico/dht22");

                try {
                    JSONObject jsonObject = new JSONObject(JsonMessage);
                    temperature = jsonObject.getString("temp");
                    huminity = jsonObject.getString("hum") + "%";
                    Log.i("threadcheck","humtemp");

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

>>>>>>> Stashed changes
}