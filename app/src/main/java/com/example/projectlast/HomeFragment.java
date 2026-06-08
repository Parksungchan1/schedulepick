package com.example.projectlast;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class HomeFragment extends Fragment {

    private TextView btnCreateGroup, btnViewSchedule;
    private RecyclerView rvMyGroups, rvMySchedules;
    private GroupAdapter groupAdapter;
    private ScheduleAdapter scheduleAdapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        btnCreateGroup  = view.findViewById(R.id.btn_create_group);
        btnViewSchedule = view.findViewById(R.id.btn_view_schedule);
        rvMyGroups      = view.findViewById(R.id.rv_my_groups);
        rvMySchedules   = view.findViewById(R.id.rv_my_schedules);

        rvMyGroups.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMySchedules.setLayoutManager(new LinearLayoutManager(getContext()));

        groupAdapter = new GroupAdapter(new ArrayList<>(), group -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToGroupCalendar(group.getGroupId());
            }
        });
        rvMyGroups.setAdapter(groupAdapter);

        scheduleAdapter = new ScheduleAdapter(new ArrayList<>(), schedule ->
                ScheduleDetailSheet.newInstance(schedule)
                        .show(getChildFragmentManager(), "detail"));
        rvMySchedules.setAdapter(scheduleAdapter);

        // 상단 네비 버튼
        view.findViewById(R.id.tv_friends).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFriends();
            }
        });
        view.findViewById(R.id.tv_mypage).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToMypage();
            }
        });
        view.findViewById(R.id.layout_logo).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
        });

        btnCreateGroup.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateGroupActivity.class)));

        btnViewSchedule.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCalendar();
            }
        });

        loadMyGroups();
        loadMySchedules();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadMyGroups();
            loadMySchedules();
        }
    }

    private void loadMyGroups() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            groupAdapter.updateList(TestData.getGroups());
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        db.collection("groups")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Group> groups = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String groupId   = doc.getId();
                        String groupName = doc.getString("groupName");
                        List<?> members  = (List<?>) doc.get("members");
                        int count        = members != null ? members.size() : 0;
                        groups.add(new Group(groupId, groupName != null ? groupName : "", "", count, ""));
                    }
                    groupAdapter.updateList(groups);
                });
    }

    private void loadMySchedules() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");

            // 런타임 생성 일정 포함 전체 일정
            List<Schedule> all = TestData.getAllSchedulesForUser(testId);

            // 오늘 00:00 이후 일정만 필터
            java.util.Calendar todayMidnight = java.util.Calendar.getInstance();
            todayMidnight.set(java.util.Calendar.HOUR_OF_DAY, 0);
            todayMidnight.set(java.util.Calendar.MINUTE, 0);
            todayMidnight.set(java.util.Calendar.SECOND, 0);
            todayMidnight.set(java.util.Calendar.MILLISECOND, 0);

            List<Schedule> upcoming = new ArrayList<>();
            for (Schedule sc : all) {
                if (sc.getStartTime() == null) continue;
                java.util.Calendar scCal = java.util.Calendar.getInstance();
                scCal.setTime(sc.getStartTime().toDate());
                if (!scCal.before(todayMidnight)) upcoming.add(sc);
            }

            // 가장 가까운 일정 순 정렬
            upcoming.sort((a, b) -> {
                if (a.getStartTime() == null) return 1;
                if (b.getStartTime() == null) return -1;
                return a.getStartTime().toDate().compareTo(b.getStartTime().toDate());
            });

            scheduleAdapter.updateList(upcoming);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        db.collection("schedules")
                .whereArrayContains("participants", uid)
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Schedule> schedules = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Schedule s = doc.toObject(Schedule.class);
                        if (s != null) {
                            s.setScheduleId(doc.getId());
                            schedules.add(s);
                        }
                    }
                    scheduleAdapter.updateList(schedules);
                });
    }
}
