package com.example.cozyiot.Machine;

import static android.content.Context.MODE_PRIVATE;

import android.graphics.Color;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cozyiot.func.*;
import com.example.cozyiot.R;
import com.example.cozyiot.foreGroundService;
import com.example.cozyiot.windowControllerActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class machineDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String PREF_NAME = "MachinePrefs";
    private static final String KEY_ADD_CLICK_COUNT = "addClickCount";
    private static final String KEY_MACHINE_LIST = "machineList";

    private static final int VIEW_TYPE_MACHINE = 0;
    private static final int VIEW_TYPE_ADD = 1;

    private final int[] cardColors = {
            Color.parseColor("#6fdec8"),
            Color.parseColor("#94d1e6"),
            Color.parseColor("#a5a0e6"),
            Color.parseColor("#f88f59"),
    };

    private int addClickCount = 1;
    private Context context;
    private Boolean adminFlag;
    public List<machineData> machineDataList;;
    private SynchronizedMqttConnector connector;

    private final Gson gson = new Gson();

    public interface OnAddClickListener {
        void onAddClicked();
    }

    private OnAddClickListener addClickListener;

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.addClickListener = listener;
    }

    public machineDataAdapter(Context context, Boolean adminFlag, List<machineData> machineDataList, SynchronizedMqttConnector Connector) {
        this.context = context;
        this.adminFlag = adminFlag;
        this.machineDataList = machineDataList;
        this.connector = Connector;


        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        addClickCount = prefs.getInt(KEY_ADD_CLICK_COUNT, 1);
    }


    @Override
    public int getItemCount() {
        return machineDataList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == machineDataList.size()) ? VIEW_TYPE_ADD : VIEW_TYPE_MACHINE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_MACHINE) {
            View view = inflater.inflate(R.layout.item_list, parent, false);
            return new MachineViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_add_button, parent, false);
            return new AddViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MachineViewHolder) {
            MachineViewHolder machineHolder = (MachineViewHolder) holder;
            machineData data = machineDataList.get(position);
            machineHolder.machineName.setText(data.getMachineName());

            int colorIndex = position % cardColors.length;
            machineHolder.cardInnerLayout.setBackgroundColor(cardColors[colorIndex]);

//            SharedPreferences preferences = context.getSharedPreferences("auto", MODE_PRIVATE);
            SharedPreferences preferences = context.getSharedPreferences("UserInfo", MODE_PRIVATE);
//            SharedPreferences.Editor editor = preferences.edit();
//            String userName = preferences.getString("userName", "");
//            String userPassword = preferences.getString("userPassword", "");
//            String IPAddress = preferences.getString("IPAddress", "");
//
//            connector = new MQTTDataFunc(IPAddress, userName, userPassword);

            connector.subscribe("pico", "auto_run");
            connector.publish("pico", "auto_run");
            String autoFlag = connector.getLatestMessage("pico", "auto_run");
            if(autoFlag == null){
                int retryCount = 0;
                while (autoFlag == null && retryCount < 50){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    autoFlag = connector.getLatestMessage("pico", "auto_run");
                }
            }
            if(autoFlag.equals("true")){
                machineHolder.deviceSwitch.setChecked(true);
            } else {
                machineHolder.deviceSwitch.setChecked(false);
            }

//            String autoFlag;
//            if(connector.callData("pico","auto_run").equals("success")){
//                autoFlag = connector.getData("pico", "auto_run");
//                if(!autoFlag.isEmpty() || autoFlag != null){
//                    if(autoFlag.equals("open")){
//                        machineHolder.deviceSwitch.setChecked(true);
//                    } else {
//                        machineHolder.deviceSwitch.setChecked(false);
//                    }
//                } else {
//                    machineHolder.deviceSwitch.setChecked(false);
//                }
//                machineHolder.deviceSwitch.setChecked(false);
//            }

//            String switchFlag = preferences.getString("auto", "false");
//            if(switchFlag.equals("true")){
//                machineHolder.deviceSwitch.setChecked(true);
//            } else {
//                machineHolder.deviceSwitch.setChecked(false);
//            }

            machineHolder.deviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Intent serviceIntent = new Intent(context, foreGroundService.class);
                    Intent controllerIntent = new Intent(context, windowControllerActivity.class);
                    if(isChecked){
                        connector.publish("pico/auto_run", "open");
//                        controllerConnector.publish(topic, message);
//                        editor.putString("auto", "true");
//                        editor.apply();
                        context.startService(serviceIntent);
                    }else{
                        connector.publish("pico/auto_run", "close");
//                        controllerConnector.publish(topic, message);
//                        editor.putString("auto", "false");
//                        editor.apply();
                        context.stopService(serviceIntent);
                    }
                }
            });

            machineHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, windowControllerActivity.class);
                intent.putExtra("admin", this.adminFlag);
                context.startActivity(intent);
            });

            machineHolder.deleteBtn.setOnClickListener(v -> {
                remove(position);
            });


        } else if (holder instanceof AddViewHolder) {
            AddViewHolder addHolder = (AddViewHolder) holder;

            int colorIndex = addClickCount % cardColors.length;
            addHolder.cardInnerLayout.setBackgroundColor(cardColors[colorIndex]);

            addHolder.itemView.setOnClickListener(v -> {
                addClickCount++;
                Log.d("AddViewHolder", String.valueOf(addClickCount));
                //작동확인
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_ADD_CLICK_COUNT, addClickCount);
                editor.apply();
                addClickListener.onAddClicked();
                saveMachineList();
                notifyDataSetChanged();


            });
        }
    }

    public void remove(int position) {
        try {
            machineDataList.remove(position);
            addClickCount--;
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_ADD_CLICK_COUNT, addClickCount);
            editor.apply();
            notifyItemRemoved(position);
            saveMachineList();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    public void saveMachineList() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(machineDataList);
        editor.putString(KEY_MACHINE_LIST, json);
        editor.apply();
    }

    public static class MachineViewHolder extends RecyclerView.ViewHolder {
        public TextView machineName;
        public TextView machineStatus;
        public TextView deleteBtn;
        public View cardInnerLayout;
        public Switch deviceSwitch;

        public MachineViewHolder(View view) {
            super(view);
            machineName = view.findViewById(R.id.machine_name);
            machineStatus = view.findViewById(R.id.device_status);
            deleteBtn = view.findViewById(R.id.delete_btn);
            cardInnerLayout = view.findViewById(R.id.card_inner_layout);
            deviceSwitch = view.findViewById(R.id.device_switch);
        }
    }

    public static class AddViewHolder extends RecyclerView.ViewHolder {
        public View cardInnerLayout;

        public AddViewHolder(View view) {
            super(view);
            cardInnerLayout = view.findViewById(R.id.card_inner_layout);
        }
    }
}