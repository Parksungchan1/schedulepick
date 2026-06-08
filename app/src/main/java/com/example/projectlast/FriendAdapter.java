package com.example.projectlast;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<Friend> list;

    public FriendAdapter(List<Friend> list) {
        this.list = list;
    }

    public void updateList(List<Friend> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend f = list.get(position);
        holder.tvNickname.setText(f.getNickname() != null ? f.getNickname() : "");
        holder.tvEmail.setText(f.getEmail() != null ? f.getEmail() : "");
        holder.ivProfile.setImageResource(TestData.getProfileDrawable(f.getUid()));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvNickname, tvEmail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile  = itemView.findViewById(R.id.iv_friend_profile);
            tvNickname = itemView.findViewById(R.id.tv_friend_nickname);
            tvEmail    = itemView.findViewById(R.id.tv_friend_email);
        }
    }
}
