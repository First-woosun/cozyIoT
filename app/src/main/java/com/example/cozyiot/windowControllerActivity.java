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
    private static SharedPreferences auto;
    private static SharedPreferences.Editor editor;
    private static String userName;
    private static String userPassword;
    private static String IPAddress;

    //안드로이드 세그먼트 선언
    Button openBtn; Button closeBtn;
    Switch autoSwitch;
    ImageView windowState;
    TextView huminityView; TextView backBtn;

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

    //습도 데이터를 저장할 변수
    private static String huminity;

    //온도 데이터를 저장할 변수
    private static String temperature;

    private MqttConnector controllerConnector;

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

        controllerConnector = new MqttConnector(IPAddress, userName, userPassword);
        isConnect = controllerConnector.connect();

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

        //현재 창문 상태에 따라 창문 이미지 설정
        //TODO 서버에 저장된 window status 값을 읽어와 업데이트 하도록 수정
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
                    controllerConnector.publish(topic, message);
                    editor.putString("auto", "true");
                    editor.apply();
                    startService(serviceIntent);
                }else{
                    String topic = "window/auto_motor_request";
                    String message = "false";
                    controllerConnector.publish(topic, message);
                    editor.putString("auto", "false");
                    editor.apply();
                    stopService(serviceIntent);
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
                    controllerConnector.publish(topic, message);
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

        //뒤로가기 버튼
        backBtn.setOnClickListener(v ->{
            controllerConnector.disconnect();
            multiThreadRun = false;
//            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        controllerConnector.disconnect();
        multiThreadRun = false;
//        startActivity(new Intent(this, HomeActivity.class));
        finish();
        super.onBackPressed();
    }
}