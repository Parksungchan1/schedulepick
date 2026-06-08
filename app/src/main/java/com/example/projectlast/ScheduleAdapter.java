package com.example.projectlast;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(Schedule schedule);
    }

    private List<Schedule> list;
    private OnClickListener clickListener;

    public ScheduleAdapter(List<Schedule> list) {
        this.list = list;
    }

    public ScheduleAdapter(List<Schedule> list, OnClickListener listener) {
        this.list = list;
        this.clickListener = listener;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }

    public void updateList(List<Schedule> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule s = list.get(position);
        holder.tvTitle.setText(s.getTitle() != null ? s.getTitle() : "");
        holder.tvDate.setText(s.getFormattedDate());
        holder.tvCategory.setText(s.getCategory() != null ? s.getCategory() : "");

        String location = s.getLocation();
        if (location != null && !location.isEmpty()) {
            holder.tvLocation.setText("📍 " + location);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvCategory, tvLocation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_schedule_title);
            tvDate     = itemView.findViewById(R.id.tv_schedule_date);
            tvCategory = itemView.findViewById(R.id.tv_schedule_category);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
        }
    }
}
