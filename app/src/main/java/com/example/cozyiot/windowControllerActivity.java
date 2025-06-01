package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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
    private static String userName;
    private static String userPassword;
    private static String IPAddress;

    Button connectBtn, disConnectBtn, openBtn, closeBtn, btnConfirmLocation;
    Switch autoSwitch;
    ImageView windowState;
    TextView huminityView, backBtn, weatherView;

    private static boolean isConnect = false;
    private static boolean isopen = false;
    private static boolean multiThreadRun = false;

    private static String huminity;
    private static String temperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_window_controller);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        if(!preferences.getAll().isEmpty()){
            userName = preferences.getString("userName", "");
            userPassword = preferences.getString("userPassword", "");
            IPAddress = preferences.getString("IPAddress", "");
        }

        connectBtn = findViewById(R.id.btn_connect);
        disConnectBtn = findViewById(R.id.btn_disconnect);
        openBtn = findViewById(R.id.btn_open);
        closeBtn = findViewById(R.id.btn_close);
        windowState = findViewById(R.id.window_state);
        huminityView = findViewById(R.id.huminity_view);
        backBtn = findViewById(R.id.btn_back);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        weatherView = findViewById(R.id.weather_view); // 기상 정보 표시할 TextView

        if(!isopen){
            windowState.setImageResource(R.drawable.window_status_close);
        } else {
            windowState.setImageResource(R.drawable.window_status_open);
        }

        openBtn.setOnClickListener(v -> {
            if(isConnect){
                if(!isopen){
                    String topic = "window/motor_request";
                    String message = "open";
                    MqttConnector.publish(message, topic);
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
                    MqttConnector.publish(message, topic);
                    isopen = false;
                    Toast.makeText(this, "창문을 폐쇠합니다.", Toast.LENGTH_SHORT).show();
                    windowState.setImageResource(R.drawable.window_status_close);
                } else {
                    Toast.makeText(this, "이미 창문이 닫혀있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        connectBtn.setOnClickListener(v -> {
            if (!isConnect) {
                MqttConnector.createMqttClient(IPAddress, userName, userPassword);
                Toast.makeText(this, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                isConnect = true;
                multiThreadRun = true;

                Thread huminityThread = new Thread(() -> {
                    Log.i("multiThread", "start multiThread");
                    while (multiThreadRun) {
                        MqttConnector.subscribe("pico/dht22");
                        String JsonMessage = MqttConnector.getLatestMassage();

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

                huminityThread.start();
            } else {
                Toast.makeText(this, "이미 연결되어 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        disConnectBtn.setOnClickListener(v ->{
            MqttConnector.disconnect();
            Toast.makeText(this, "연결을 해제했습니다.", Toast.LENGTH_SHORT).show();
            isConnect = false;
            multiThreadRun = false;
        });

        backBtn.setOnClickListener(v ->{
            finish();
        });

        btnConfirmLocation.setOnClickListener(v -> {
            Intent intent = new Intent(windowControllerActivity.this, MapActivity.class);
            startActivity(intent);
        });
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

                String finalText = "날씨: " + weather + "\n온도: " + temp + "°C";

                runOnUiThread(() -> weatherView.setText(finalText));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> weatherView.setText("날씨 정보를 불러올 수 없습니다."));
            }
        }).start();
    }
}