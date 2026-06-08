package com.example.projectlast;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GroupFragment extends Fragment {

    public static final String ARG_GROUP_ID = "groupId";

    private String groupId;
    private TextView tvGroupName, tvMemberCount, tvScheduleCount, tvEmptySchedule;
    private RecyclerView rvMembers, rvGroupSchedules;
    private FirebaseFirestore db;

    public static GroupFragment newInstance(String groupId) {
        GroupFragment f = new GroupFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        groupId = getArguments() != null ? getArguments().getString(ARG_GROUP_ID, "") : "";

        tvGroupName      = view.findViewById(R.id.tv_group_name);
        tvMemberCount    = view.findViewById(R.id.tv_group_member_count);
        tvScheduleCount  = view.findViewById(R.id.tv_schedule_count);
        tvEmptySchedule  = view.findViewById(R.id.tv_empty_schedule);
        rvMembers        = view.findViewById(R.id.rv_members);
        rvGroupSchedules = view.findViewById(R.id.rv_group_schedules);

        rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGroupSchedules.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.iv_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.tv_invite).setOnClickListener(v ->
                Toast.makeText(getContext(), "초대 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_group_calendar).setOnClickListener(v -> {
            GroupCalendarFragment f = new GroupCalendarFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.btn_add_schedule).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateScheduleActivity.class)));

        view.findViewById(R.id.tv_leave_group).setOnClickListener(v -> leaveGroup());

        if (!groupId.isEmpty()) loadGroupData();
    }

    private void loadGroupData() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false) && TestData.GROUP_ID.equals(groupId)) {
            List<Group> groups = TestData.getGroups();
            if (!groups.isEmpty()) {
                Group g = groups.get(0);
                tvGroupName.setText(g.getGroupName());
                tvMemberCount.setText("멤버 " + g.getMemberCount() + "명");
            }
            List<Schedule> schedules = TestData.getGroupSchedules();
            int count = schedules.size();
            tvScheduleCount.setText(count + "개");
            if (count == 0) tvEmptySchedule.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvGroupName.setText(doc.getString("groupName"));
                        List<?> members = (List<?>) doc.get("members");
                        int count = members != null ? members.size() : 0;
                        tvMemberCount.setText("멤버 " + count + "명");
                    }
                });

        db.collection("schedules")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(query -> {
                    int count = query.size();
                    tvScheduleCount.setText(count + "개");
                    if (count == 0) tvEmptySchedule.setVisibility(View.VISIBLE);
                });
    }

    private void leaveGroup() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || groupId.isEmpty()) return;

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    List<String> members = (List<String>) doc.get("members");
                    if (members != null) {
                        members.remove(uid);
                        db.collection("groups").document(groupId)
                                .update("members", members)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(), "그룹에서 나갔습니다.", Toast.LENGTH_SHORT).show();
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                });
                    }
                });
    }
}
