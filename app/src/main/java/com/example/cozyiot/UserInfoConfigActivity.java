package com.example.cozyiot;

import android.app.appsearch.StorageInfo;
import android.content.Intent;
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
    private SharedPreferences location;

    EditText userNameInput; EditText userPasswordInput; EditText wifiNameInput; EditText wifiPasswordInput; EditText IPAddressInput; EditText locationInput;
    Button saveBtn; Button resetBtn; Button locationBtn;
    TextView backBtn;

    private static MqttConnector infoConnector;

    private static String userName;
    private static String userPassword;
    private static String wifiName;
    private static String wifiPassword;
    private static String IPAddress;
    private static String locationData;

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
        locationInput = findViewById(R.id.edit_location);
        saveBtn = findViewById(R.id.btn_save);
        resetBtn = findViewById(R.id.btn_reset);
        backBtn = findViewById(R.id.btn_back);
        locationBtn = findViewById(R.id.btn_location);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        location = getSharedPreferences("location_prefs", MODE_PRIVATE);

        infoConnector = new MqttConnector("218.49.196.80:1883", "cozydow", "1234");
        infoConnector.connect();

        // 기존에 저장된 정보를 Edittext에 표시
        if(!preferences.getAll().isEmpty()){
            userNameInput.setText(preferences.getString("userName", ""));
            userPasswordInput.setText(preferences.getString("userPassword", ""));
            wifiNameInput.setText(preferences.getString("wifiName", ""));
            wifiPasswordInput.setText(preferences.getString("wifiPassword", ""));
            IPAddressInput.setText(preferences.getString("IPAddress",""));
        }

        if(!location.getAll().isEmpty()){
            float latitude = location.getFloat("latitude", 0f);
            float longitude = location.getFloat("longitude", 0f);
            String location = String.valueOf(latitude) + ", "+ String.valueOf(longitude);
            locationInput.setText(location);
        }
        
        //위치 정보 설정 버튼
        locationBtn.setOnClickListener(v ->{
            startActivity(new Intent(this, MapActivity.class));
            finish();
        });

        //사용자 정보 저장 버튼
        saveBtn.setOnClickListener(v -> {
            userName = userNameInput.getText().toString();
            userPassword = userPasswordInput.getText().toString();
            wifiName = wifiNameInput.getText().toString();
            wifiPassword = wifiPasswordInput.getText().toString();
            IPAddress = IPAddressInput.getText().toString();
            locationData = locationInput.getText().toString();

            boolean NameFlag = userName.isEmpty();
            boolean passwordFlag = userPassword.isEmpty();
            boolean wifiNameFlag = wifiName.isEmpty();
            boolean wifiPasswordFlag = wifiPassword.isEmpty();
            boolean IPAddressFlag = IPAddress.isEmpty();
            boolean locationFlag = locationData.isEmpty();

            if(!NameFlag && !passwordFlag && !wifiNameFlag && !wifiPasswordFlag && !IPAddressFlag && !locationFlag){
                editor.putString("userName", userName);
                editor.putString("userPassword", userPassword);
                editor.putString("wifiName", wifiName);
                editor.putString("wifiPassword", wifiPassword);
                editor.putString("IPAddress", IPAddress);
                editor.putString("Location", locationData);
                editor.apply();
                Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();

                //사용자 정보를 서버에 전송
                infoConnector.publish("userInfo/name", userName);
                infoConnector.publish("userInfo/password", userPassword);
                infoConnector.publish("userInfo/wifiName", wifiName);
                infoConnector.publish("userInfo/wifiPassword", wifiPassword);
                infoConnector.publish("userInfo/IPAddress", IPAddress);
                infoConnector.publish("userInfo/config", "save");
                infoConnector.disconnect();

            } else {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 사용자 정보 초기화 버튼
        resetBtn.setOnClickListener(v -> {
            String resetUserName = userNameInput.getText().toString();
            if(!preferences.getAll().isEmpty()){
                editor.clear();
                editor.apply();

                infoConnector.publish("reset:" , "userInfo/config");
                if ( resetUserName != null && ! resetUserName.isEmpty()) {
                infoConnector.publish( resetUserName, "userInfo/name");
                }
                infoConnector.disconnect();
                userNameInput.setText("");
                userPasswordInput.setText("");
                wifiNameInput.setText("");
                wifiPasswordInput.setText("");
                IPAddressInput.setText("");
                locationInput.setText("");
            } else {
                Toast.makeText(this, "저장된 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        backBtn.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        infoConnector.disconnect();
        super.onBackPressed();
    }
}