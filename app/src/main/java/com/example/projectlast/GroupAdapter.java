package com.example.projectlast;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private List<Group> groupList;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public GroupAdapter(List<Group> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.tvGroupName.setText(group.getGroupName());
        holder.tvMembers.setText(group.getMembersText());
        holder.tvDate.setText(group.getStartDate());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGroupClick(group);
        });
    }

    @Override
    public int getItemCount() {
        return groupList != null ? groupList.size() : 0;
    }

    public void updateList(List<Group> newList) {
        this.groupList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvMembers, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvMembers   = itemView.findViewById(R.id.tv_members);
            tvDate      = itemView.findViewById(R.id.tv_date);
        }
    }
}
