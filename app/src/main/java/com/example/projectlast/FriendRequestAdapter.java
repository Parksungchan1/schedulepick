package com.example.projectlast;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    public interface RequestCallback {
        void onAccept(FriendRequest request);
        void onReject(FriendRequest request);
    }

    private List<FriendRequest> list;
    private final RequestCallback callback;

    public FriendRequestAdapter(List<FriendRequest> list, RequestCallback callback) {
        this.list = list;
        this.callback = callback;
    }

    public void updateList(List<FriendRequest> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest req = list.get(position);
        holder.tvNickname.setText(req.getFromNickname() != null ? req.getFromNickname() : "");
        holder.tvEmail.setText(req.getFromEmail() != null ? req.getFromEmail() : "");
        holder.btnAccept.setOnClickListener(v -> { if (callback != null) callback.onAccept(req); });
        holder.btnReject.setOnClickListener(v -> { if (callback != null) callback.onReject(req); });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNickname, tvEmail, btnAccept, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNickname = itemView.findViewById(R.id.tv_request_nickname);
            tvEmail    = itemView.findViewById(R.id.tv_request_email);
            btnAccept  = itemView.findViewById(R.id.btn_accept);
            btnReject  = itemView.findViewById(R.id.btn_reject);
        }
    }
}
