package com.example.cozyiot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cozyiot.func.MqttConnector;

public class signUpActivity extends AppCompatActivity {

    TextView UsernameInput; TextView UserpasswordInput; TextView WIFINameInput; TextView WIFIPasswordInput; TextView IPAddressInput;
    Button signUpBtn;

    private static MqttConnector signUpConnector;

    private String Username;
    private String Password;
    private String WIFIName;
    private String WIFIPassword;
    private String IPAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences preference = getSharedPreferences("UserInfo", MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();

        signUpConnector = new MqttConnector("218.49.196.80:1883", "cozydow", "1234");

        UsernameInput = findViewById(R.id.editTextUsername);
        UserpasswordInput = findViewById(R.id.editTextPassword);
        WIFINameInput = findViewById(R.id.editTextWIFIName);
        WIFIPasswordInput = findViewById(R.id.editTextWIFIPassword);
        IPAddressInput = findViewById(R.id.editTextIPAddress);

        signUpBtn = findViewById(R.id.signup_Btn);

        signUpBtn.setOnClickListener(v -> {
            Username = UsernameInput.getText().toString();
            Password = UserpasswordInput.getText().toString();
            WIFIName = WIFINameInput.getText().toString();
            WIFIPassword = WIFIPasswordInput.getText().toString();
            IPAddress = IPAddressInput.getText().toString();

            if(Username.isEmpty() && Password.isEmpty() && WIFIName.isEmpty() && WIFIPassword.isEmpty() && IPAddress.isEmpty()){
                Toast.makeText(this, "모든 값을 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                editor.putString("userName", Username);
                editor.putString("userPassword", Password);
                editor.putString("wifiName", WIFIName);
                editor.putString("wifiPassword", WIFIPassword);
                editor.putString("IPAddress", IPAddress);
                editor.apply();
                Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();

                //사용자 정보를 서버에 전송
                signUpConnector.connect();
                signUpConnector.publish("userInfo/name", Username);
                signUpConnector.publish("userInfo/password", Password);
                signUpConnector.publish("userInfo/wifiName", WIFIName);
                signUpConnector.publish("userInfo/wifiPassword", WIFIPassword);
                signUpConnector.publish("userInfo/IPAddress", IPAddress);
                signUpConnector.publish("userInfo/config", "save");
                signUpConnector.disconnect();

                Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show();

                finish();
            }
        });
    }
}