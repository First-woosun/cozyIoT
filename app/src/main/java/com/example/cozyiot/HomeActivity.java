package com.example.cozyiot;

import com.example.cozyiot.Machine.*;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.*;

import com.example.cozyiot.func.MqttConnector;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ImageButton sidebarBtn;
    private ImageButton machineAddBtn;
    private RecyclerView recyclerView;
    private machineDataAdapter machineDataAdapter;
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

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        machineDataList = new ArrayList<>();
        machineDataList.add(new machineData("거실 창문"));

        machineDataAdapter = new machineDataAdapter(HomeActivity.this, machineDataList);
        recyclerView.setAdapter(machineDataAdapter);
    }
}