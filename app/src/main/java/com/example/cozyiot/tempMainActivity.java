package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.*;

import com.example.cozyiot.func.MqttConnector;

public class tempMainActivity extends AppCompatActivity {

    Button userInfoConfig; Button windowControl;
    SharedPreferences preferences;

    private static boolean connectFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_temp_main);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);

        userInfoConfig = findViewById(R.id.user_config);
        windowControl = findViewById(R.id.window_control);

        userInfoConfig.setOnClickListener(v -> {
            startActivity(new Intent(tempMainActivity.this, UserInfoConfigActivity.class));
        });

        windowControl.setOnClickListener(v -> {
            if(!preferences.getAll().isEmpty()){
                String userName = preferences.getString("userName", "");
                String userPassword = preferences.getString("userPassword", "");
                String IPAddress = preferences.getString("IPAddress", "");
                connectFlag = MqttConnector.createMqttClient(IPAddress ,userName, userPassword);
                MqttConnector.disconnect();
                if(connectFlag){
                    startActivity(new Intent(tempMainActivity.this, windowControllerActivity.class));
                } else {
                    Toast.makeText(tempMainActivity.this, "연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.d("ConnectionFlag", "잘못된 사용자 정보");
                }

            } else {
                Toast.makeText(tempMainActivity.this, "사용자 정보가 등록되어 있지 않습니다.\n등록 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                Log.e("userInfo", "사용자 정보 미기입");
            }
        });
    }
}