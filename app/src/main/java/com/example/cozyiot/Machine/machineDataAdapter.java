package com.example.cozyiot.Machine;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.example.cozyiot.HomeActivity;
import com.example.cozyiot.R;
import com.example.cozyiot.windowControllerActivity;

public class machineDataAdapter extends RecyclerView.Adapter<machineDataAdapter.ViewHolder> {
    private List<machineData> machineDataList;
    private Context context;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView machineName;
//        public ImageView machineImage;
//        public ImageView machinePower;

        public ViewHolder(View view){
            super(view);
            machineName = view.findViewById(R.id.machine_name);
//            machineImage = view.findViewById(R.id.machine_img);
        }
    }

    public machineDataAdapter(Context context ,List<machineData> machineDataList){
        this.machineDataList = machineDataList;
        this.context = context;
    }

    @Override
    public machineDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        holder.machineName.setText(machineDataList.get(position).getMachineName());

        holder.itemView.setOnClickListener(v ->{
            Intent intent = new Intent(context, windowControllerActivity.class);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount(){
        return machineDataList.size();
    }
}
