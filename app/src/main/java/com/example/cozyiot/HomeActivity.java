package com.example.cozyiot;

import static android.view.View.INVISIBLE;

import com.example.cozyiot.Machine.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.widget.*;

import com.example.cozyiot.func.MqttConnector;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;
public class HomeActivity extends AppCompatActivity {

    private MqttConnector homeConnector;

    private ImageButton machineAddBtn;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private ImageButton sidebarBtn;
    TextView cityName; ImageView weatherStatus; TextView temperature;

    private RecyclerView recyclerView;
    private machineDataAdapter machineDataAdapter;
    private TextView navLogout;
    private List<machineData> machineDataList;

    private SharedPreferences preferences;
    private SharedPreferences location;

    private static boolean connectFlag;

    private boolean adminFlag;

    private String Address; private  String Name; private String Password;
    private String city; private float latitude; private float longtitude;

    private boolean threadFlag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        cityName = findViewById(R.id.tv_city_name);
        weatherStatus = findViewById(R.id.tv_weather_status);
        temperature = findViewById(R.id.tv_temperature);

        Intent intent = getIntent();

        adminFlag = intent.getBooleanExtra("admin", false);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        location = getSharedPreferences("location_prefs", MODE_PRIVATE);

        city = location.getString("cityName", "");
        latitude = location.getFloat("latitude", 0f);
        longtitude = location.getFloat("longtitude", 0f);

        //계정 유형에 따른 데이터 load
        if(adminFlag){
            Address = "218.49.196.80:1883";
            Name = "cozydow";
            Password = "1234";
        } else {
            Address = preferences.getString("IPAddress", "");
            Name = preferences.getString("userName", "");
            Password = preferences.getString("userPassword", "");
        }

        if(!location.getAll().isEmpty()){
            city = location.getString("cityName", "");
            latitude = location.getFloat("latitude", 0f);
            longtitude = location.getFloat("longtitude", 0f);
            cityName.setText(city);

            loadWeatherFromSavedLocation(latitude, longtitude);
        } else {
            cityName.setText("???");
            weatherStatus.setVisibility(INVISIBLE);
            temperature.setText("0°C");
        }

        homeConnector = new MqttConnector(Address, Name, Password);

        //machineAddBtn = findViewById(R.id.btn_add_item);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        Type type = new TypeToken<List<machineData>>() {}.getType();
        List<machineData> machineDataList;
        SharedPreferences prefs = getSharedPreferences("MachinePrefs", MODE_PRIVATE);
        String json = prefs.getString("machineList", null);
        Gson gson = new Gson();

        if (json != null) {
            machineDataList = gson.fromJson(json, type);
        } else {
            machineDataList = new ArrayList<>();
            machineDataList.add(new machineData("window")); // 기본값
        }
        // 모듈이름 받아와서 모듈 이름으로 추가하는 방식으로
        machineDataAdapter = new machineDataAdapter(HomeActivity.this, adminFlag, machineDataList);
        recyclerView.setAdapter(machineDataAdapter);

        machineDataAdapter.setOnAddClickListener(() -> {
            // 새 기기 리스트에 추가
            machineDataList.add(new machineData("새 기기"));
            for (machineData data : machineDataAdapter.machineDataList) {
                Log.d("MachineList", "이름: " + data.getMachineName());
            }

            machineDataAdapter.saveMachineList();

            // 어댑터에 알리기 - 새 항목 추가됨
            machineDataAdapter.notifyItemInserted(machineDataList.size() - 1);
        });



        sidebarBtn = findViewById(R.id.btn_sidebar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
//        MaterialToolbar toolbar = findViewById(R.id.toolbar);

//        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
//                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        sidebarBtn.setOnClickListener(v -> {
            if(!drawerLayout.isDrawerOpen(GravityCompat.END)){
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if(id == R.id.nav_user_info_config){
                if(adminFlag){
                    Toast.makeText(this, "관리자 계정은 변경할 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Intent userInfoIntent = new Intent(this, UserInfoConfigActivity.class);
                    userInfoIntent.putExtra("admin", false);
                    startActivity(userInfoIntent);
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
        navLogout = findViewById(R.id.nav_logout)   ;
        navLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("userName", "");
            editor.putString("userPassword", "");
            editor.putBoolean("autoLogin", false); // 여기서 자동로그인 끄기
            editor.apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed(){
        if(drawerLayout.isDrawerOpen(GravityCompat.END)){
            drawerLayout.closeDrawer(GravityCompat.END);;
        }else{
            threadFlag = false;
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        city = location.getString("cityName", "");
        latitude = location.getFloat("latitude", 0f);
        longtitude = location.getFloat("longtitude", 0f);
        cityName.setText(city);

        loadWeatherFromSavedLocation(latitude, longtitude);

        super.onResume();
    }


    private void loadWeatherFromSavedLocation(float lat, float lon) {
        String apiKey = "45253cb5ee7d2cf08c1cc1d6b4a811d8";
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon +
                "&appid=" + apiKey +
                "&units=metric&lang=kr";

        new Thread(() -> {
            if(threadFlag){
                try {
                    URL requestUrl = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    JSONObject response = new JSONObject(result.toString());
                    int weatherId = response.getJSONArray("weather").getJSONObject(0).getInt("id");
                    double temp = response.getJSONObject("main").getDouble("temp");

                    String temp_now = temp+"°C";

                    if(weatherId < 600 && weatherId >= 200){
                        runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.rainnyday));
                    } else if (weatherId >= 600 && weatherId <700) {
                        runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.snowday));
                    } else if (weatherId > 800){
                        runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.cloudyday));
                    } else {
                        runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.clearday));
                    }

                    runOnUiThread(() -> temperature.setText(temp_now));

                    Thread.sleep(30000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}