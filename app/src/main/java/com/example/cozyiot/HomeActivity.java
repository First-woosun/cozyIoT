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

import android.view.MenuItem;
import android.widget.*;

import com.example.cozyiot.func.MqttConnector;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.utilities.TonalPalette;
import com.google.android.material.navigation.NavigationView;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private MqttConnector homeConnector;

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

    private boolean adminFlag;

    private String Address; private  String Name; private String Password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();

        adminFlag = intent.getBooleanExtra("admin", false);

        preferences = getSharedPreferences("UserInfo", MODE_PRIVATE);

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


        homeConnector = new MqttConnector(Address, Name, Password);

        //machineAddBtn = findViewById(R.id.btn_add_item);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        machineDataList = new ArrayList<>();
        machineDataList.add(new machineData("window"));
        // 모듈이름 받아와서 모듈 이름으로 추가하는 방식으로
        machineDataAdapter = new machineDataAdapter(HomeActivity.this, adminFlag, machineDataList);
        recyclerView.setAdapter(machineDataAdapter);

        machineDataAdapter.setOnAddClickListener(() -> {
            // 새 기기 리스트에 추가
            machineDataList.add(new machineData("새 기기"));
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

    @Override
    protected void onResume() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        machineDataList = new ArrayList<>();
        machineDataList.add(new machineData("window"));
        // 모듈이름 받아와서 모듈 이름으로 추가하는 방식으로
        machineDataAdapter = new machineDataAdapter(HomeActivity.this, adminFlag, machineDataList);
        recyclerView.setAdapter(machineDataAdapter);
        super.onResume();
    }
}