package com.example.cozyiot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cozyiot.func.MqttConnector;


public class UserInfoConfigActivity extends AppCompatActivity {

    private SharedPreferences preferences;

    EditText userNameInput; EditText userPasswordInput; EditText wifiNameInput; EditText wifiPasswordInput; EditText IPAddressInput;
    Button saveBtn; Button resetBtn; TextView backBtn;

    private static String userName;
    private static String userPassword;
    private static String wifiName;
    private static String wifiPassword;
    private static String IPAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info_config);

        userNameInput = findViewById(R.id.edit_username);
        userPasswordInput = findViewById(R.id.edit_password);
        wifiNameInput = findViewById(R.id.edit_wifiname);
        wifiPasswordInput = findViewById(R.id.edit_wifipassword);
        IPAddressInput = findViewById(R.id.edit_IPAddress);
        saveBtn = findViewById(R.id.btn_save);
        resetBtn = findViewById(R.id.btn_reset);
        backBtn = findViewById(R.id.btn_back);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // 기존에 저장된 정보가 있다면 Edittext에 표시
        if(!preferences.getAll().isEmpty()){
            userNameInput.setText(preferences.getString("userName", ""));
            userPasswordInput.setText(preferences.getString("userPassword", ""));
            wifiNameInput.setText(preferences.getString("wifiName", ""));
            wifiPasswordInput.setText(preferences.getString("wifiPassword", ""));
            IPAddressInput.setText(preferences.getString("IPAddress",""));;
        }

        //사용자 정보 저장 버튼
        saveBtn.setOnClickListener(v -> {
            userName = userNameInput.getText().toString();
            userPassword = userPasswordInput.getText().toString();
            wifiName = wifiNameInput.getText().toString();
            wifiPassword = wifiPasswordInput.getText().toString();
            IPAddress = IPAddressInput.getText().toString();

            boolean NameFlag = userName.isEmpty();
            boolean passwordFlag = userPassword.isEmpty();
            boolean wifiNameFlag = wifiName.isEmpty();
            boolean wifiPasswordFlag = wifiPassword.isEmpty();
            boolean IPAddressFlag = IPAddress.isEmpty();

            if(!NameFlag && !passwordFlag && !wifiNameFlag && !wifiPasswordFlag && !IPAddressFlag){
                editor.putString("userName", userName);
                editor.putString("userPassword", userPassword);
                editor.putString("wifiName", wifiName);
                editor.putString("wifiPassword", wifiPassword);
                editor.putString("IPAddress", IPAddress);
                editor.apply();
                Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();

                //사용자 정보를 서버에 전송
                MqttConnector.createMqttClient("218.49.196.80:1883", "cozydow", "1234");
                MqttConnector.publish(userName, "userInfo/name");
                MqttConnector.publish(userPassword, "userInfo/password");
                MqttConnector.publish(wifiName, "userInfo/wifiName");
                MqttConnector.publish(wifiPassword, "userInfo/wifiPassword");
                MqttConnector.publish(IPAddress, "userInfo/IPAddress");
                MqttConnector.publish("save", "userInfo/config");
                MqttConnector.disconnect();

            } else {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 사용자 정보 초기화 버튼
        resetBtn.setOnClickListener(v -> {
            if(!preferences.getAll().isEmpty()){
                editor.clear();
                editor.apply();

                userNameInput.setText("");
                userPasswordInput.setText("");
                wifiNameInput.setText("");
                wifiPasswordInput.setText("");
                IPAddressInput.setText("");

                MqttConnector.createMqttClient("218.49.196.80:1883", "cozydow", "1234");
                MqttConnector.publish("reset:" , "userInfo/config");
                MqttConnector.publish("userName:" , "userInfo/name");

            } else {
                Toast.makeText(this, "저장된 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        backBtn.setOnClickListener(v -> {
            finish();
        });
    }
}