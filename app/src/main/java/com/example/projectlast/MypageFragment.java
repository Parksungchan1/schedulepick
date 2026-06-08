package com.example.projectlast;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MypageFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvMyName, tvMyId, tvEditProfile;
    private TextView tvGroupCount, tvFriendCount, tvScheduleCount;
    private TextView tvInfoName, tvInfoEmail, tvInfoAddress, tvInfoOrg;
    private RecyclerView rvMiniFriends, rvGroupList;

    private FriendAdapter miniFriendAdapter;
    private GroupAdapter groupAdapter;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mypage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        ivProfile        = view.findViewById(R.id.iv_profile);
        tvMyName         = view.findViewById(R.id.tv_my_name);
        tvMyId           = view.findViewById(R.id.tv_my_id);
        tvEditProfile    = view.findViewById(R.id.tv_edit_profile);
        tvGroupCount     = view.findViewById(R.id.tv_group_count);
        tvFriendCount    = view.findViewById(R.id.tv_friend_count);
        tvScheduleCount  = view.findViewById(R.id.tv_schedule_count);
        tvInfoName       = view.findViewById(R.id.tv_info_name);
        tvInfoEmail      = view.findViewById(R.id.tv_info_email);
        tvInfoAddress    = view.findViewById(R.id.tv_info_address);
        tvInfoOrg        = view.findViewById(R.id.tv_info_org);
        rvMiniFriends    = view.findViewById(R.id.rv_mini_friends);
        rvGroupList      = view.findViewById(R.id.rv_group_list);

        rvMiniFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGroupList.setLayoutManager(new LinearLayoutManager(getContext()));

        miniFriendAdapter = new FriendAdapter(new ArrayList<>());
        rvMiniFriends.setAdapter(miniFriendAdapter);

        groupAdapter = new GroupAdapter(new ArrayList<>(), group -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).navigateToGroupCalendar(group.getGroupId());
        });
        rvGroupList.setAdapter(groupAdapter);

        view.findViewById(R.id.iv_logo).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).navigateToHome();
        });
        view.findViewById(R.id.tv_logo).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).navigateToHome();
        });

        tvEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        loadUserInfo();
        loadFriendList();
        loadGroupList();
    }

    private void loadUserInfo() {
        // 테스트 모드: SharedPreferences에서 로드
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            String nickname = prefs.getString("test_nickname", "");
            String address  = prefs.getString("test_address", "");
            String email    = prefs.getString("test_email", "");
            String id       = prefs.getString("test_id", "");
            tvMyName.setText(nickname);
            tvMyId.setText("@" + id);
            tvInfoName.setText(nickname);
            tvInfoEmail.setText(email);
            tvInfoAddress.setText(address);
            tvInfoOrg.setText("");
            tvGroupCount.setText(String.valueOf(TestData.getGroups().size()));
            tvFriendCount.setText(String.valueOf(TestData.getFriends(id).size()));
            tvScheduleCount.setText(String.valueOf(TestData.getSchedules(id).size()));
            ivProfile.setImageResource(TestData.getProfileDrawable(id));
            return;
        }

        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String nickname = doc.getString("nickname");
                    String email    = doc.getString("email");
                    String address  = doc.getString("address");
                    String org      = doc.getString("org");

                    tvMyName.setText(nickname != null ? nickname : "");
                    tvMyId.setText("@" + (email != null ? email.split("@")[0] : ""));
                    tvInfoName.setText(nickname != null ? nickname : "");
                    tvInfoEmail.setText(email != null ? email : "");
                    tvInfoAddress.setText(address != null ? address : "");
                    tvInfoOrg.setText(org != null ? org : "");
                });

        // 그룹 수
        db.collection("groups")
                .whereArrayContains("members", currentUser.getUid())
                .get()
                .addOnSuccessListener(snap -> tvGroupCount.setText(String.valueOf(snap.size())));

        // 일정 수
        db.collection("schedules")
                .whereArrayContains("participants", currentUser.getUid())
                .get()
                .addOnSuccessListener(snap -> tvScheduleCount.setText(String.valueOf(snap.size())));
    }

    private void loadFriendList() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");
            List<Friend> friends = TestData.getFriends(testId);
            miniFriendAdapter.updateList(friends);
            tvFriendCount.setText(String.valueOf(friends.size()));
            return;
        }

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
                    tvFriendCount.setText(String.valueOf(friends.size()));
                    miniFriendAdapter.updateList(friends);
                });
    }

    private void loadGroupList() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            groupAdapter.updateList(TestData.getGroups());
            return;
        }

        if (currentUser == null) return;

        db.collection("groups")
                .whereArrayContains("members", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Group> groups = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("groupName");
                        groups.add(new Group(doc.getId(), name != null ? name : "", "", 0, ""));
                    }
                    groupAdapter.updateList(groups);
                });
    }
}
