package com.example.cozyiot;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.*;

public class MainActivity extends AppCompatActivity {
    //안드로이드 세그먼트 선언
    Button connectBtn; Button disConnectBtn; Button openBtn; Button closeBtn;
    Switch autoSwitch;
    ImageView windowState;
    TextView huminityView;

    //MQTT 클라이언트가 연결되어 있는지 확인하는 flag(추후 수정)
    private static boolean isConnect = false;

    //창문의 현재 상태를 파악하는 flag (추후 수정)
    private static boolean isopen = false;

    //습도 데이터 처리를 위한 멀티스레드 작동 flag
    private static boolean multiThreadRun = false;

    //습도 데이터를 저장할 변수
    private static String huminity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        connectBtn = findViewById(R.id.btn_connect);
        disConnectBtn = findViewById(R.id.btn_disconnect);
        openBtn = findViewById(R.id.btn_open);
        closeBtn = findViewById(R.id.btn_close);
        windowState = findViewById(R.id.window_state);
        huminityView = findViewById(R.id.huminity_view);

        //습도를 받아오기 위한 스레드 메소드
        Thread huminityThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("스레드 동작중");
                while (multiThreadRun){
                    MqttConnector.subscribe("window/motor_request");
                    huminity = MqttConnector.getLatestMassage() + "%";
                    runOnUiThread(() -> huminityView.setText(huminity));
                    System.out.println(huminity);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println("멀티스레드 종료");
            }
        });

        //현재 창문 상태에 따라 창문 이미지 설정
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
        connectBtn.setOnClickListener(v ->{
            if(!isConnect){
                MqttConnector.createMqttClient();
                Toast.makeText(this, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                isConnect = true;
                multiThreadRun = true;
                huminityThread.start();
//                System.out.println(MqttConnector.getLatestMassage());
            } else {
                Toast.makeText(this, "이미 연결되어 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //MQTT 클라이언트 제거 버튼
        disConnectBtn.setOnClickListener(v ->{
            MqttConnector.disconnect();
            Toast.makeText(this, "연결을 해제했습니다.", Toast.LENGTH_SHORT).show();
            isConnect = false;
            multiThreadRun = false;
        });
    }
}