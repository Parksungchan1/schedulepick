package com.example.projectlast;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendFragment extends Fragment {

    private EditText etSearchUser;
    private TextView btnSendRequest;
    private RecyclerView rvFriendRequests, rvGroupInvites, rvFriendList;
    private LinearLayout layoutEmptyInvite;

    private FriendRequestAdapter requestAdapter;
    private FriendAdapter friendAdapter;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        etSearchUser      = view.findViewById(R.id.et_search_user);
        btnSendRequest    = view.findViewById(R.id.btn_send_request);
        rvFriendRequests  = view.findViewById(R.id.rv_friend_requests);
        rvGroupInvites    = view.findViewById(R.id.rv_group_invites);
        rvFriendList      = view.findViewById(R.id.rv_friend_list);
        layoutEmptyInvite = view.findViewById(R.id.layout_empty_invite);

        rvFriendRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGroupInvites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriendList.setLayoutManager(new LinearLayoutManager(getContext()));

        requestAdapter = new FriendRequestAdapter(new ArrayList<>(), new FriendRequestAdapter.RequestCallback() {
            @Override public void onAccept(FriendRequest request) { acceptRequest(request); }
            @Override public void onReject(FriendRequest request) { rejectRequest(request); }
        });
        rvFriendRequests.setAdapter(requestAdapter);

        friendAdapter = new FriendAdapter(new ArrayList<>());
        rvFriendList.setAdapter(friendAdapter);

        view.findViewById(R.id.layout_logo).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).navigateToHome();
        });

        btnSendRequest.setOnClickListener(v -> {
            String query = etSearchUser.getText().toString().trim();
            if (TextUtils.isEmpty(query)) {
                Toast.makeText(getContext(), "이메일 또는 닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            sendFriendRequest(query);
        });

        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");
            friendAdapter.updateList(TestData.getFriends(testId));
            requestAdapter.updateList(new ArrayList<>());
            rvGroupInvites.setVisibility(View.GONE);
            layoutEmptyInvite.setVisibility(View.VISIBLE);
            return;
        }

        loadFriendRequests();
        loadGroupInvites();
        loadFriendList();
    }

    private void sendFriendRequest(String query) {
        if (currentUser == null) return;

        db.collection("users")
                .whereEqualTo("email", query)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        onUserFound(snapshot.getDocuments().get(0));
                        return;
                    }
                    // 이메일 검색 실패 시 닉네임으로 재검색
                    db.collection("users")
                            .whereEqualTo("nickname", query)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                if (snap2.isEmpty()) {
                                    Toast.makeText(getContext(), "사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    onUserFound(snap2.getDocuments().get(0));
                                }
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void onUserFound(DocumentSnapshot targetDoc) {
        if (currentUser == null) return;
        String toUid = targetDoc.getId();

        if (toUid.equals(currentUser.getUid())) {
            Toast.makeText(getContext(), "자기 자신에게는 요청할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이미 친구인지 확인
        db.collection("users").document(currentUser.getUid())
                .collection("friends").document(toUid).get()
                .addOnSuccessListener(friendDoc -> {
                    if (friendDoc.exists()) {
                        Toast.makeText(getContext(), "이미 친구입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 요청 전송
                    db.collection("users").document(currentUser.getUid()).get()
                            .addOnSuccessListener(myDoc -> {
                                Map<String, Object> request = new HashMap<>();
                                request.put("fromUid", currentUser.getUid());
                                request.put("toUid", toUid);
                                request.put("fromNickname", myDoc.getString("nickname"));
                                request.put("fromEmail", myDoc.getString("email"));
                                request.put("status", "pending");
                                request.put("timestamp", FieldValue.serverTimestamp());

                                db.collection("friendRequests").add(request)
                                        .addOnSuccessListener(ref -> {
                                            etSearchUser.setText("");
                                            Toast.makeText(getContext(), "친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(getContext(), "요청 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            });
                });
    }

    private void loadFriendRequests() {
        if (currentUser == null) return;

        db.collection("friendRequests")
                .whereEqualTo("toUid", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        FriendRequest req = doc.toObject(FriendRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            requests.add(req);
                        }
                    }
                    requestAdapter.updateList(requests);
                });
    }

    private void acceptRequest(FriendRequest request) {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(myDoc -> {
                    WriteBatch batch = db.batch();

                    // 내 친구 목록에 추가
                    Map<String, Object> myFriendEntry = new HashMap<>();
                    myFriendEntry.put("nickname", request.getFromNickname());
                    myFriendEntry.put("email", request.getFromEmail());
                    myFriendEntry.put("uid", request.getFromUid());
                    batch.set(db.collection("users").document(currentUser.getUid())
                            .collection("friends").document(request.getFromUid()), myFriendEntry);

                    // 상대방 친구 목록에 추가
                    Map<String, Object> theirFriendEntry = new HashMap<>();
                    theirFriendEntry.put("nickname", myDoc.getString("nickname"));
                    theirFriendEntry.put("email", myDoc.getString("email"));
                    theirFriendEntry.put("uid", currentUser.getUid());
                    batch.set(db.collection("users").document(request.getFromUid())
                            .collection("friends").document(currentUser.getUid()), theirFriendEntry);

                    // 요청 상태 업데이트
                    batch.update(db.collection("friendRequests").document(request.getRequestId()),
                            "status", "accepted");

                    batch.commit().addOnSuccessListener(v -> {
                        Toast.makeText(getContext(), "친구 요청을 수락했습니다.", Toast.LENGTH_SHORT).show();
                        loadFriendRequests();
                        loadFriendList();
                    });
                });
    }

    private void rejectRequest(FriendRequest request) {
        db.collection("friendRequests").document(request.getRequestId())
                .update("status", "rejected")
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "친구 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                });
    }

    private void loadGroupInvites() {
        if (currentUser == null) {
            rvGroupInvites.setVisibility(View.GONE);
            layoutEmptyInvite.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("groupInvitations")
                .whereEqualTo("toUid", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        rvGroupInvites.setVisibility(View.GONE);
                        layoutEmptyInvite.setVisibility(View.VISIBLE);
                        return;
                    }

                    layoutEmptyInvite.setVisibility(View.GONE);
                    rvGroupInvites.setVisibility(View.VISIBLE);

                    // 각 초대를 동적으로 표시 (간단한 LinearLayout 방식)
                    LinearLayout container = new LinearLayout(getContext());
                    container.setOrientation(LinearLayout.VERTICAL);
                    rvGroupInvites.addView(container); // 임시: 실제로는 어댑터 사용 권장

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        String inviteId   = doc.getId();
                        String groupId    = doc.getString("groupId");
                        String groupName  = doc.getString("groupName");
                        String fromUid    = doc.getString("fromUid");

                        android.widget.LinearLayout row = new android.widget.LinearLayout(getContext());
                        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        row.setPadding(0, 16, 0, 16);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                        android.widget.TextView tvInfo = new android.widget.TextView(getContext());
                        android.widget.LinearLayout.LayoutParams lp =
                                new android.widget.LinearLayout.LayoutParams(0,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        tvInfo.setLayoutParams(lp);
                        tvInfo.setText((groupName != null ? groupName : "그룹") + " 초대");
                        tvInfo.setTextSize(14f);
                        tvInfo.setTextColor(android.graphics.Color.parseColor("#000000"));

                        android.widget.TextView btnAccept = new android.widget.TextView(getContext());
                        android.widget.LinearLayout.LayoutParams btnLp =
                                new android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                        btnLp.setMarginEnd(8);
                        btnAccept.setLayoutParams(btnLp);
                        btnAccept.setText("수락");
                        btnAccept.setTextSize(12f);
                        btnAccept.setTextColor(android.graphics.Color.WHITE);
                        btnAccept.setBackgroundColor(android.graphics.Color.parseColor("#827EFF"));
                        btnAccept.setPadding(20, 8, 20, 8);
                        btnAccept.setClickable(true);
                        btnAccept.setFocusable(true);
                        btnAccept.setOnClickListener(v -> acceptGroupInvite(inviteId, groupId, row));

                        android.widget.TextView btnReject = new android.widget.TextView(getContext());
                        btnReject.setText("거절");
                        btnReject.setTextSize(12f);
                        btnReject.setTextColor(android.graphics.Color.parseColor("#FF5858"));
                        btnReject.setBackgroundColor(android.graphics.Color.parseColor("#FFE8E8"));
                        btnReject.setPadding(20, 8, 20, 8);
                        btnReject.setClickable(true);
                        btnReject.setFocusable(true);
                        btnReject.setOnClickListener(v -> rejectGroupInvite(inviteId, row));

                        row.addView(tvInfo);
                        row.addView(btnAccept);
                        row.addView(btnReject);
                        container.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    rvGroupInvites.setVisibility(View.GONE);
                    layoutEmptyInvite.setVisibility(View.VISIBLE);
                });
    }

    private void acceptGroupInvite(String inviteId, String groupId, View row) {
        if (currentUser == null || groupId == null) return;

        WriteBatch batch = db.batch();
        // 그룹 멤버 추가
        batch.update(db.collection("groups").document(groupId),
                "members", FieldValue.arrayUnion(currentUser.getUid()));
        // 초대 상태 업데이트
        batch.update(db.collection("groupInvitations").document(inviteId),
                "status", "accepted");
        batch.commit().addOnSuccessListener(v -> {
            Toast.makeText(getContext(), "그룹에 참여했습니다!", Toast.LENGTH_SHORT).show();
            ((android.view.ViewGroup) row.getParent()).removeView(row);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "수락 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectGroupInvite(String inviteId, View row) {
        db.collection("groupInvitations").document(inviteId)
                .update("status", "rejected")
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "초대를 거절했습니다.", Toast.LENGTH_SHORT).show();
                    ((android.view.ViewGroup) row.getParent()).removeView(row);
                });
    }

    private void loadFriendList() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .collection("friends")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Friend> friends = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String uid      = doc.getString("uid");
                        String nickname = doc.getString("nickname");
                        String email    = doc.getString("email");
                        friends.add(new Friend(uid != null ? uid : doc.getId(), nickname, email));
                    }
                    friendAdapter.updateList(friends);
                });
    }
}
