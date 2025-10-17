package com.example.cozyiot;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.TextView;

import com.example.cozyiot.func.SynchronizedMqttConnector;

public class indoorStatusConfigActivity extends AppCompatActivity {

    private SeekBar tempBar; private SeekBar humidityBar; private SeekBar COBar;
    private TextView tempTV; private TextView humTV; private TextView COTV; private TextView backBtn; private TextView saveBtn;
    private int finalTempValue; private int finalHumValue; private int finalCOValue;

    private SynchronizedMqttConnector connector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_indoor_status_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        connector = SynchronizedMqttConnector.getInstance();
        
        //뒤로가기 버튼 초기화;
        backBtn = findViewById(R.id.btn_back);

        //저장 버튼 초기화
        saveBtn = findViewById(R.id.btn_save);

        // seekBar 초기화
        tempBar = findViewById(R.id.sb_temperature);
        humidityBar = findViewById(R.id.sb_humidity);
        COBar = findViewById(R.id.sb_co);

        // TV TextView 초기화
        tempTV = findViewById(R.id.tv_temp_value);
        humTV = findViewById(R.id.tv_humidity_value);
        COTV = findViewById(R.id.tv_co_value);

        // 초기 값 설정 (onCreate 시점의 초기 progress 값을 TextView에 반영)
        tempTV.setText(String.valueOf(tempBar.getProgress()));
        humTV.setText(String.valueOf(humidityBar.getProgress()));
        COTV.setText(String.valueOf(COBar.getProgress()));

        // 각 값들 초기화
        finalTempValue = tempBar.getProgress();
        finalHumValue = humidityBar.getProgress();
        finalCOValue = COBar.getProgress();

        //뒤로가기 버튼
        backBtn.setOnClickListener(v -> {
            finish();
        });

        tempBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempTV.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                finalTempValue = seekBar.getProgress();
            }
        });

        humidityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                humTV.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                finalHumValue = seekBar.getProgress();
            }
        });

        COBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                COTV.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                finalCOValue = seekBar.getProgress();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("final values -> ", "temp: "+finalTempValue+", humidity: "+finalHumValue+", CO: "+finalCOValue);
                //TODO 각 값들을 서버에 전송하는 코드 or 로컬에 저장하는 코드 추가

            }
        });
    }
}