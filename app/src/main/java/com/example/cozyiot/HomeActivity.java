package com.example.cozyiot;

import static android.view.View.INVISIBLE;

import com.example.cozyiot.Machine.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.widget.*;

import com.example.cozyiot.func.*;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeActivity extends AppCompatActivity {

    private MQTTDataFunc homeConnector;
    private SynchronizedMqttConnector Connector;

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
    private String city; private String weatherID; private String temper;

    private boolean threadFlag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        Connector = SynchronizedMqttConnector.getInstance();

        cityName = findViewById(R.id.tv_city_name);
        weatherStatus = findViewById(R.id.tv_weather_status);
        temperature = findViewById(R.id.tv_temperature);

        Intent intent = getIntent();

        adminFlag = intent.getBooleanExtra("admin", false);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        location = getSharedPreferences("location_prefs", MODE_PRIVATE);

//        city = location.getString("cityName", "");
//        latitude = location.getFloat("latitude", 0f);
//        longtitude = location.getFloat("longtitude", 0f);

        //계정 유형에 따른 데이터 load
        if(adminFlag){
            Address = "218.49.196.80:1883";
            Name = "cozydow";
            Password = "1234";
            System.out.println("관리자");
        } else {
            Address = preferences.getString("IPAddress", "");
            Name = preferences.getString("userName", "");
            Password = preferences.getString("userPassword", "");
            System.out.println("유저");
        }

        loadWeatherAndCityName();

//        if(!location.getAll().isEmpty()){
//            city = location.getString("cityName", "");
//            latitude = location.getFloat("latitude", 0f);
//            longtitude = location.getFloat("longtitude", 0f);
//            cityName.setText(city);
//
//            loadWeatherFromSavedLocation(latitude, longtitude);
//            city = homeConnector.callData("cityName");
//            latitude = Float.parseFloat(homeConnector.callData("latitude"));
//            longitude = Float.parseFloat(homeConnector.callData("longitude"));
//
//            loadWeatherFromSavedLocation(latitude, longitude);
//        } else {
//            cityName.setText("???");
//            weatherStatus.setVisibility(INVISIBLE);
//            temperature.setText("0°C");
//        }

//        homeConnector = new MqttConnector(Address, Name, Password);
//        machineAddBtn = findViewById(R.id.btn_add_item);

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
        machineDataAdapter = new machineDataAdapter(HomeActivity.this, adminFlag, machineDataList, Connector);
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
            //식물 정보 추가 페이지 진입
            } else if (id == R.id.nav_Plant_config) {
                Intent indoorConditionIntent = new Intent(this, indoorStatusConfigActivity.class);
                startActivity(indoorConditionIntent);
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

        // Android 13 이상에서 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }

    @Override
    public void onBackPressed(){
        if(drawerLayout.isDrawerOpen(GravityCompat.END)){
            drawerLayout.closeDrawer(GravityCompat.END);;
        }else{
            threadFlag = false;
            Connector.disconnect();
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        loadWeatherAndCityName();

        super.onResume();
    }


    private void loadWeatherAndCityName() {
        new Thread(() -> {
            if(threadFlag){
                try {
                    //도시이름 받아오기
                    Connector.subscribe("userInfo", "cityName");
                    Connector.publish("userInfo", "cityName");
                    Connector.subscribe("userInfo", "weather");
                    Connector.publish("userInfo", "weather");

                    Thread.sleep(200);

                    city = Connector.getLatestMessage("userInfo", "cityName");
                    String weatherCollection = Connector.getLatestMessage("userInfo", "weather");

                    runOnUiThread(() -> cityName.setText(city));

                    if(weatherCollection != null){
                        JsonParser jsonParser = new JsonParser();
                        JsonObject jsonCollection = (JsonObject) jsonParser.parse(weatherCollection);

                        String temper = String.valueOf(jsonCollection.get("temperature"));
                        int weatherId = Integer.parseInt(String.valueOf(jsonCollection.get("description")));


                        if(weatherId < 600 && weatherId >= 200){
                            runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.rainnyday));
                        } else if (weatherId >= 600 && weatherId <700) {
                            runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.snowday));
                        } else if (weatherId > 800){
                            runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.cloudyday));
                        } else {
                            runOnUiThread(() -> weatherStatus.setImageResource(R.drawable.clearday));
                        }

                        runOnUiThread(() -> temperature.setText(temper));
                    }

                    Thread.sleep(30000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String decodeUnicode(String unicodeStr) {
        StringBuilder sb = new StringBuilder();
        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(unicodeStr);

        int lastEnd = 0;
        while (matcher.find()) {
            // 매치되지 않은 일반 문자 처리
            sb.append(unicodeStr, lastEnd, matcher.start());

            // 16진수를 문자로 변환
            int code = Integer.parseInt(matcher.group(1), 16);
            sb.append((char) code);

            lastEnd = matcher.end();
        }
        sb.append(unicodeStr.substring(lastEnd));
        sb.deleteCharAt(0);
        sb.deleteCharAt(sb.length()-1);

        return sb.toString();
    }
}