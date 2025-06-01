package com.example.cozyiot.Machine;

import android.graphics.Color;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cozyiot.R;
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
    private List<machineData> machineDataList;
    private final Gson gson = new Gson();

    public interface OnAddClickListener {
        void onAddClicked();
    }

    private OnAddClickListener addClickListener;

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.addClickListener = listener;
    }

    public machineDataAdapter(Context context, List<machineData> machineDataList) {
        this.context = context;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        addClickCount = prefs.getInt(KEY_ADD_CLICK_COUNT, 1);

        String json = prefs.getString(KEY_MACHINE_LIST, null);
        if (json != null) {
            Type type = new TypeToken<List<machineData>>() {}.getType();
            this.machineDataList = gson.fromJson(json, type);
        } else {
            this.machineDataList = machineDataList;
        }
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

            machineHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, windowControllerActivity.class);
                context.startActivity(intent);
            });

            machineHolder.itemView.setOnLongClickListener(v -> {
                remove(position);
                return true;
            });

        } else if (holder instanceof AddViewHolder) {
            AddViewHolder addHolder = (AddViewHolder) holder;

            int colorIndex = addClickCount % cardColors.length;
            addHolder.cardInnerLayout.setBackgroundColor(cardColors[colorIndex]);

            addHolder.itemView.setOnClickListener(v -> {
                if (addClickListener != null) {
                    addClickCount++;

                    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(KEY_ADD_CLICK_COUNT, addClickCount);
                    editor.apply();

                    addClickListener.onAddClicked();
                    saveMachineList();
                    notifyItemChanged(getItemCount() - 1);
                }
            });
        }
    }

    public void remove(int position) {
        try {
            machineDataList.remove(position);
            notifyItemRemoved(position);
            saveMachineList();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    private void saveMachineList() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(machineDataList);
        editor.putString(KEY_MACHINE_LIST, json);
        editor.apply();
    }

    public static class MachineViewHolder extends RecyclerView.ViewHolder {
        public TextView machineName;
        public View cardInnerLayout;

        public MachineViewHolder(View view) {
            super(view);
            machineName = view.findViewById(R.id.machine_name);
            cardInnerLayout = view.findViewById(R.id.card_inner_layout);
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