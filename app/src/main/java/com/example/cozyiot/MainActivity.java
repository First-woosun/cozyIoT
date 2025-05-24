package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.*;

import com.example.cozyiot.func.MqttConnector;

public class MainActivity extends AppCompatActivity {

    Button loginBtn; Button signUpBtn;
    TextView userNameInput; TextView passwordInput;

    SharedPreferences preferences;

    private static boolean connectFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        loginBtn = findViewById(R.id.buttonLogin);
        signUpBtn = findViewById(R.id.buttonSignup);
        userNameInput = findViewById(R.id.editTextUsername);
        passwordInput = findViewById(R.id.editTextPassword);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);

        if(!preferences.getAll().isEmpty()){
            String name = preferences.getString("userName", "");
            String pass = preferences.getString("userPassword", "");
            String address = preferences.getString("IPAddress", "");

            connectFlag = MqttConnector.createMqttClient(address, name, pass);
            MqttConnector.disconnect();

            if(connectFlag){
                Toast.makeText(this, "자동 로그인 성공", Toast.LENGTH_SHORT).show();
                Log.d("Auto Login", "connecting Success");
                startActivity(new Intent(MainActivity.this, tempMainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "로그인 에러", Toast.LENGTH_SHORT).show();
                Log.e("Auto Login", "Connecting Error");
            }
        }

        loginBtn.setOnClickListener(v -> {
            String nameInput = userNameInput.getText().toString();
            String passInput = passwordInput.getText().toString();
            String address = preferences.getString("IPAddress", "");

            connectFlag = MqttConnector.createMqttClient(address, nameInput, passInput);
            MqttConnector.disconnect();

            if(connectFlag){
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                Log.d("Login", "Login Success");
                startActivity(new Intent(MainActivity.this, tempMainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show();
                Log.e("Login", "Login Fail");
            }
        });

        signUpBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserInfoConfigActivity.class));
        });
    }
}