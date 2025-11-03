package com.example.cozyiot;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.cozyiot.func.SynchronizedMqttConnector;
import com.google.gson.JsonObject;

public class indoorStatusConfigActivity extends AppCompatActivity {

    private SeekBar tempBar; private SeekBar humidityBar; private SeekBar airConditionBar; private SeekBar luxBar;
    private TextView tempTV; private TextView humTV; private TextView airConditionTV; private TextView luxTv; private TextView backBtn; private TextView saveBtn;
    private int finalTempValue; private int finalHumValue; private int finalAirConditionValue; private int finalLuxValue;

    private SynchronizedMqttConnector connector;

    private SharedPreferences indoorPerf;
    private SharedPreferences.Editor indoorPrefEditor;

    private String[] plants = {"다육이", "선인장", "관엽수", "사용자 지정"};

    private boolean isUserSeeking = false;

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

        // 설정 값들을 로컬에 저장할 preference 선언
        indoorPerf = getSharedPreferences("indoor", MODE_PRIVATE);
        indoorPrefEditor = indoorPerf.edit();

        // MQTT 커넥터 인스턴스 발행
        connector = SynchronizedMqttConnector.getInstance();

        // 뷰 초기화 (순서 변경: 리스너 설정 전에 모두 끝낸다)
        backBtn = findViewById(R.id.btn_back);
        saveBtn = findViewById(R.id.btn_save);
        tempBar = findViewById(R.id.sb_temperature);
        humidityBar = findViewById(R.id.sb_humidity);
        airConditionBar = findViewById(R.id.sb_co);
        luxBar = findViewById(R.id.sb_lux);
        tempTV = findViewById(R.id.tv_temp_value);
        humTV = findViewById(R.id.tv_humidity_value);
        airConditionTV = findViewById(R.id.tv_air_condition_value);
        luxTv = findViewById(R.id.tv_lux_value);

        // Spinner 초기화 (어댑터 설정)
        Spinner plantSpinner = findViewById(R.id.spinner_plant_select);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                plants
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        plantSpinner.setAdapter(adapter);

        // --- [순서 변경] 모든 리스너를 값 로드 전에 설정 ---

        // 뒤로가기 버튼
        backBtn.setOnClickListener(v -> {
            finish();
        });

        // [순서 변경] Spinner 리스너 설정
        plantSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // [수정] 사용자가 SeekBar를 직접 만져서 리스너가 호출된 게 아니라면,
                // Spinner의 프리셋 값을 적용한다.
                if (isUserSeeking) {
                    return; // 사용자가 SeekBar를 조작 중일 때는 프리셋 값을 덮어쓰지 않음
                }

                int temp = 0;
                int hum = 0;
                int lux = 0;
                int co = 10;

                switch (position){
                    case 0: // 다육이
                        temp = 18; hum = 45; lux = 65;
                        break;
                    case 1: // 선인장
                        temp = 25; hum = 30; lux = 65;
                        break;
                    case 2: // 관엽식물
                        temp = 20; hum = 60; lux = 30;
                        break;
                    case 3: // 사용자 지정
                        // [수정] '사용자 지정'을 선택한 경우, 프리셋을 적용하는 게 아니라
                        // 현재 SeekBar의 값을 final 변수에 반영해야 하므로
                        // 여기서는 값을 변경하지 않고 현재 값을 읽어온다.
                        temp = tempBar.getProgress();
                        hum = humidityBar.getProgress();
                        lux = luxBar.getProgress();
                        co = airConditionBar.getProgress();
                        break;
                }

                // [수정] '사용자 지정'이 아닐 때만 값을 덮어쓴다.
                if (position != 3) {
                    tempBar.setProgress(temp);
                    tempTV.setText(String.valueOf(temp));
                    humidityBar.setProgress(hum);
                    humTV.setText(String.valueOf(hum));
                    luxBar.setProgress(lux);
                    luxTv.setText(String.valueOf(lux));
                    airConditionBar.setProgress(co);
                    airConditionTV.setText(String.valueOf(co));
                }

                // final 변수는 항상 현재 값으로 업데이트
                finalTempValue = temp;
                finalHumValue = hum;
                finalLuxValue = lux;
                finalAirConditionValue = co;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // [순서 변경] SeekBar 리스너 설정
        // [수정] 사용자가 SeekBar를 건드리면 Spinner를 '사용자 지정'으로 변경
        SeekBar.OnSeekBarChangeListener userSeekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return; // 코드로 변경된 경우는 무시

                int id = seekBar.getId();
                if (id == R.id.sb_temperature) {
                    tempTV.setText(String.valueOf(progress));
                } else if (id == R.id.sb_humidity) {
                    humTV.setText(String.valueOf(progress));
                } else if (id == R.id.sb_lux) {
                    luxTv.setText(String.valueOf(progress));
                } else if (id == R.id.sb_co) {
                    airConditionTV.setText(String.valueOf(progress));
                }

                // [수정] 사용자가 직접 조작했으므로 Spinner를 '사용자 지정'(3번)으로 변경
                if (plantSpinner.getSelectedItemPosition() != 3) {
                    plantSpinner.setSelection(3);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true; // 사용자가 SeekBar 조작 시작
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int id = seekBar.getId();
                if (id == R.id.sb_temperature) {
                    finalTempValue = seekBar.getProgress();
                } else if (id == R.id.sb_humidity) {
                    finalHumValue = seekBar.getProgress();
                } else if (id == R.id.sb_lux) {
                    finalLuxValue = seekBar.getProgress();
                } else if (id == R.id.sb_co) {
                    finalAirConditionValue = seekBar.getProgress();
                }
                isUserSeeking = false; // 사용자 조작 끝
            }
        };

        tempBar.setOnSeekBarChangeListener(userSeekListener);
        humidityBar.setOnSeekBarChangeListener(userSeekListener);
        luxBar.setOnSeekBarChangeListener(userSeekListener);
        airConditionBar.setOnSeekBarChangeListener(userSeekListener);

        // --- [순서 변경] 모든 리스너 설정 후, 저장된 값을 불러온다 ---

        // 기존에 저장되어있던 pre 값들 load
        String preTemperature = indoorPerf.getString("temperature", "0");
        String preHumidity = indoorPerf.getString("humidity", "0");
        String preLux = indoorPerf.getString("lux", "0");
        String preAirCondition = indoorPerf.getString("airCondition", "0");
        // [수정] Spinner 위치도 불러온다 (기본값: 3번 "사용자 지정")
        int prePlantPosition = indoorPerf.getInt("plantPosition", 3);

        // pre 값 설정 (TextView)
        tempTV.setText(preTemperature);
        humTV.setText(preHumidity);
        airConditionTV.setText(preAirCondition);
        luxTv.setText(preLux);

        // pre 값들을 SeekBar에 반영 (이때 onProgressChanged가 호출되지만 fromUser=false)
        tempBar.setProgress(Integer.parseInt(preTemperature));
        humidityBar.setProgress(Integer.parseInt(preHumidity));
        luxBar.setProgress(Integer.parseInt(preLux));
        airConditionBar.setProgress(Integer.parseInt(preAirCondition));

        // 각 값들 초기화
        finalTempValue = Integer.parseInt(preTemperature);
        finalHumValue = Integer.parseInt(preHumidity);
        finalAirConditionValue = Integer.parseInt(preAirCondition);
        finalLuxValue = Integer.parseInt(preLux);

        // [수정] 저장된 Spinner 위치를 마지막에 설정
        // setSelection(pos, false)는 리스너를 호출하지 *않으려 시도*하지만,
        // (보통은 호출됨) 우리 로직은 호출되어도 안전함.
        plantSpinner.setSelection(prePlantPosition, false);


        // [순서 변경] 저장 버튼 리스너 설정
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("final values -> ", "temp: "+finalTempValue+", humidity: "+finalHumValue+", lux: "+finalLuxValue+" ,airCondition: "+ finalAirConditionValue);

                JsonObject finalCollection = new JsonObject();
                finalCollection.addProperty("temperature", finalTempValue);
                finalCollection.addProperty("huminity", finalHumValue);
                finalCollection.addProperty("lux", finalLuxValue);
                finalCollection.addProperty("airCondition", finalAirConditionValue);
                System.out.println(finalCollection.toString());
                // connector.plantPublish(finalCollection.toString()); // 이전 오류 수정

                // [수정] Spinner의 현재 위치도 저장
                indoorPrefEditor.putInt("plantPosition", plantSpinner.getSelectedItemPosition());

                indoorPrefEditor.putString("temperature", String.valueOf(finalTempValue));
                indoorPrefEditor.putString("humidity", String.valueOf(finalHumValue));
                indoorPrefEditor.putString("lux", String.valueOf(finalLuxValue));
                indoorPrefEditor.putString("airCondition", String.valueOf(finalAirConditionValue));
                indoorPrefEditor.apply();
            }
        });
    }
}