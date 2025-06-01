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

    SharedPreferences preferences;;

    private MqttConnector loginConnector;

    private static boolean connectFlag;
    private static boolean autoLoginFlag;


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
        autoLoginFlag = preferences.getBoolean("autoLogin", false);

        //자동 로그인 로직
        if(autoLoginFlag){
            if(!preferences.getAll().isEmpty()){
                String name = preferences.getString("userName", "");
                String pass = preferences.getString("userPassword", "");
                String address = preferences.getString("IPAddress", "");
                loginConnector = new MqttConnector(address, name, pass);
                connectFlag = loginConnector.connect();
                if(connectFlag){
                    Toast.makeText(this, "자동 로그인 성공", Toast.LENGTH_SHORT).show();
                    Log.d("Auto Login", "connecting Success");
                    loginConnector.disconnect();
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "로그인 에러", Toast.LENGTH_SHORT).show();
                    Log.e("Auto Login", "Connecting Error");
                }
            }
        }

        // 사용자 로그인 로직
        loginBtn.setOnClickListener(v -> {
            String nameInput = userNameInput.getText().toString();
            String passInput = passwordInput.getText().toString();
            String address = preferences.getString("IPAddress", "");

            //백도어(?)
            if(nameInput.equals("cozydow")){
                loginConnector = new MqttConnector("218.49.196.80:1883", nameInput, passInput);
                connectFlag = loginConnector.connect();
                if(connectFlag){
                    Toast.makeText(this, "관리자 계정 로그인", Toast.LENGTH_SHORT).show();
                    Log.i("Login", "관리자 로그인");
                    loginConnector.disconnect();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("admin", true);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    Log.e("Login", "Login Fail");
                }
            } else {
                //일반 사용자 로그인
                loginConnector = new MqttConnector(address, nameInput, passInput);
                connectFlag = loginConnector.connect();

                if(connectFlag){
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("autoLogin", true); // 자동 로그인 설정
                    editor.apply();
                    Log.d("Login", "Login Success");
                    loginConnector.disconnect();
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.putExtra("admin", false);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    Log.e("Login", "Login Fail");
                }
            }
        });

        //회원가입 버튼
        signUpBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, signUpActivity.class));
        });
    }
}