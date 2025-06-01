package com.example.cozyiot;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 확인 버튼 클릭 시 선택된 위치를 SharedPreferences에 저장
        confirmButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                // 위치 정보 저장
                SharedPreferences prefs = getSharedPreferences("location_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("latitude", (float) selectedLatLng.latitude);
                editor.putFloat("longitude", (float) selectedLatLng.longitude);
                editor.apply(); // 비동기 저장

                Toast.makeText(this, "위치 저장 완료", Toast.LENGTH_SHORT).show();

                // 현재 액티비티 종료 (필요 시 다른 화면으로 이동 가능)
                finish();
            } else {
                Toast.makeText(this, "지도를 눌러 위치를 먼저 선택하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // 현재 위치로 지도 이동
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        } else {
                            // 위치를 못 받았을 경우 기본 위치(서울)
                            LatLng defaultLocation = new LatLng(37.5665, 126.9780);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        // 지도 클릭 시 마커 표시 및 위치 저장
        mMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            mMap.clear(); // 기존 마커 제거
            mMap.addMarker(new MarkerOptions().position(latLng).title("선택한 위치"));
        });
    }
}