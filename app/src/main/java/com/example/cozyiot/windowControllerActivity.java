package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.*;
import com.example.cozyiot.func.MqttConnector;
import com.example.cozyiot.func.MqttConnectorTest;

import org.json.JSONException;
import org.json.JSONObject;

public class windowControllerActivity extends AppCompatActivity {

    private static SharedPreferences preferences;
    private static String userName;
    private static String userPassword;
    private static String IPAddress;

    //안드로이드 세그먼트 선언
    Button connectBtn; Button disConnectBtn; Button openBtn; Button closeBtn;
    Switch autoSwitch;
    ImageView windowState;
    TextView huminityView; TextView backBtn;

    //MQTT 클라이언트가 연결되어 있는지 확인하는 flag(추후 수정)
    //TODO 서버에 저장된 flag값을 읽어와 저장하도록 수정
    private static boolean isConnect = false;

    //창문의 현재 상태를 파악하는 flag (추후 수정)
    //TODO 서버에 저장된 flag값을 읽어와 저장하도록 수정
    private static boolean isopen = false;

    //습도 데이터 처리를 위한 멀티스레드 작동 flag
    private static boolean multiThreadRun = false;

    //습도 데이터를 저장할 변수
    private static String huminity;

    //온도 데이터를 저장할 변수
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

        //현재 창문 상태에 따라 창문 이미지 설정
        //TODO 서버에 저장된 window status 값을 읽어와 업데이트 하도록 수정
        if(!isopen){
            windowState.setImageResource(R.drawable.window_status_close);
        } else {
            windowState.setImageResource(R.drawable.window_status_open);
        }

        //창문 개방 버튼
        openBtn.setOnClickListener(v -> {
            if(isConnect){
                if(!isopen){
                    String topic = "window/motor_request";
                    String message = "open";
                    MqttConnector.publish(message, topic);
                    isopen = true;
                    Toast.makeText(this, "창문을 개방합니다.", Toast.LENGTH_SHORT).show();
                    windowState.setImageResource(R.drawable.window_status_open);
//                    System.out.println(MqttConnector.getLatestMassage());
                } else {
                    Toast.makeText(this, "이미 창문이 열려있습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "장치가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //창문 패쇄 버튼
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

        //MQTT 클라이언트 생성 버튼
        //TODO 클라이언트 연결은 기기 추가로 이전하고 서버에서 연결된 장치 정보를 읽어와 커넥터를 생성하는 방식으로 수정
        connectBtn.setOnClickListener(v -> {
            if (!isConnect) {
                MqttConnector.createMqttClient(IPAddress, userName, userPassword);
                Toast.makeText(this, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                isConnect = true;
                multiThreadRun = true;

                // dht22의 데이터를 받기 위한 Thread 생성
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

                huminityThread.start(); // 새 스레드를 시작
            } else {
                Toast.makeText(this, "이미 연결되어 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });


        //MQTT 클라이언트 제거 버튼
        //TODO 클라이언트 제거는 장치 연결 해제로 이전
        disConnectBtn.setOnClickListener(v ->{
            MqttConnector.disconnect();
            Toast.makeText(this, "연결을 해제했습니다.", Toast.LENGTH_SHORT).show();
            isConnect = false;
            multiThreadRun = false;
        });

        //뒤로가기 버튼
        backBtn.setOnClickListener(v ->{
            finish();
        });
    }
}