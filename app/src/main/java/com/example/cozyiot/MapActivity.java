package com.example.cozyiot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.cozyiot.func.MqttConnector;

import org.json.JSONObject;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private Button confirmButton;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        preferences = getSharedPreferences("locationInfo", MODE_PRIVATE);
        editor = preferences.edit();

        confirmButton = findViewById(R.id.btn_confirm_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);


        // 위치선택 누르면 위도경도 정보보냄
        confirmButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                try {
//                    // JSON 형식으로 위치 정보 생성
//                    JSONObject locationData = new JSONObject();
//                    locationData.put("deviceId", "window001");
//                    locationData.put("latitude", selectedLatLng.latitude);
//                    locationData.put("longitude", selectedLatLng.longitude);

                    editor.putString("latitude", String.valueOf(selectedLatLng.latitude));
                    editor.putString("longitude", String.valueOf(selectedLatLng.longitude));
                    editor.apply();
//
//                    String topic = "window/location_update";
//                    MqttConnector.publish(locationData.toString(), topic);

                    Toast.makeText(this, "위치 정보 저장", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, UserInfoConfigActivity.class));
                    finish();
                } catch (Exception e) {
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "위치를 먼저 선택하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 초기 위치 설정 (예: 서울)
        LatLng defaultLocation = new LatLng(37.5665, 126.9780);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        // 지도 클릭 시 위치 저장
        mMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            mMap.clear(); // 이전 마커 제거
            mMap.addMarker(new MarkerOptions().position(latLng).title("선택한 위치"));
        });
    }
}