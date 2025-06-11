package com.example.cozyiot;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.location.Address;
import android.location.Geocoder;
import android.content.Context;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

                String city = getAddressFromLocation(this, selectedLatLng.latitude, selectedLatLng.longitude);
                editor.putString("cityName", city);

                editor.apply(); // 비동기 저장

                Toast.makeText(this, "위치 저장 완료", Toast.LENGTH_SHORT).show();

                // 현재 액티비티 종료 (선택 사항)
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

    public static String getAddressFromLocation(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.KOREA);
        String result = "주소를 찾을 수 없습니다.";

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
//                result = address.getAddressLine(0); // 전체 주소
                  result = address.getLocality(); // 도시 이름만
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}