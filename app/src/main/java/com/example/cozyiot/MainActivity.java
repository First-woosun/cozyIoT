package com.example.cozyiot;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.*;

public class MainActivity extends AppCompatActivity {
    Button connectBtn; Button disConnectBtn; Button openBtn; Button closeBtn;
    Switch autoSwitch;
    ImageView windowState;

    private static boolean isConnect = false;
    private static boolean isopen = false;

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

        connectBtn.setOnClickListener(v ->{
            if(!isConnect){
                MqttConnector.createMqttClient();
                Toast.makeText(this, "연결되었습니다.", Toast.LENGTH_SHORT).show();
                isConnect = true;
            } else {
                Toast.makeText(this, "이미 연결되어 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        disConnectBtn.setOnClickListener(v ->{
            MqttConnector.disconnect();
            Toast.makeText(this, "연결을 해제했습니다.", Toast.LENGTH_SHORT).show();
            isConnect = false;
        });
    }
}