package com.example.cozyiot;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.*;

import com.example.cozyiot.func.MqttConnector;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ImageButton machineAddBtn;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private ImageButton sidebarBtn;

    private RecyclerView recyclerView;
    private machineDataAdapter machineDataAdapter;
    private TextView navLogout;
    private List<machineData> machineDataList;

    private SharedPreferences preferences;

    private static boolean connectFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);

        String Address = preferences.getString("IPAddress", "");
        String Name = preferences.getString("userName", "");
        String Password = preferences.getString("userPassword", "");

        MqttConnector.createMqttClient(Address, Name, Password);

        machineAddBtn = findViewById(R.id.btn_add_item);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        machineDataList = new ArrayList<>();
        machineDataList.add(new machineData("거실 창문"));

        machineDataAdapter = new machineDataAdapter(HomeActivity.this, machineDataList);
        recyclerView.setAdapter(machineDataAdapter);

        machineAddBtn.setOnClickListener(v -> {
            machineDataList.add(new machineData("기기 추가 예시"));
            recyclerView.setAdapter(machineDataAdapter);
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
                startActivity(new Intent(this, UserInfoConfigActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
        navLogout = findViewById(R.id.nav_logout)   ;
        navLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
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
            super.onBackPressed();
        }
    }
}