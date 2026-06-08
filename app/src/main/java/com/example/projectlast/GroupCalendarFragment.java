package com.example.projectlast;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.projectlast.Group;
import com.example.projectlast.Schedule;
import com.example.projectlast.TestData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupCalendarFragment extends Fragment {

    private static final String ARG_GROUP_ID = "groupId";

    private String groupId;
    private int currentYear, currentMonth;

    private TextView tvGroupName, tvMonthName, tvYear, tvSelectedDate;
    private TextView btnWeekly, btnMonthly;
    private ImageView ivGroupProfile;
    private GridView gvCalendar;
    private LinearLayout layoutWeekly, layoutWeekDays, layoutTimeRows, layoutParticipants;
    private RelativeLayout layoutEvents;
    private RecyclerView rvGroupSchedules;

    private boolean isWeeklyMode = false;
    private FirebaseFirestore db;
    private ScheduleAdapter scheduleAdapter;

    private final Map<String, List<String>> eventColorMap = new HashMap<>();

    private static final String[] MONTH_NAMES = {
        "JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
        "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"
    };

    public static GroupCalendarFragment newInstance(String groupId) {
        GroupCalendarFragment fragment = new GroupCalendarFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_calendar, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadGroupInfo();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGroupName        = view.findViewById(R.id.tv_group_name);
        tvMonthName        = view.findViewById(R.id.tv_month_name);
        tvYear             = view.findViewById(R.id.tv_year);
        tvSelectedDate     = view.findViewById(R.id.tv_selected_date);
        btnWeekly          = view.findViewById(R.id.btn_weekly);
        btnMonthly         = view.findViewById(R.id.btn_monthly);
        ivGroupProfile     = view.findViewById(R.id.iv_group_profile);
        gvCalendar         = view.findViewById(R.id.gv_calendar);
        layoutWeekly       = view.findViewById(R.id.layout_weekly);
        layoutWeekDays     = view.findViewById(R.id.layout_week_days);
        layoutTimeRows     = view.findViewById(R.id.layout_time_rows);
        layoutEvents       = view.findViewById(R.id.layout_events);
        layoutParticipants = view.findViewById(R.id.layout_participants);
        rvGroupSchedules   = view.findViewById(R.id.rv_group_schedules);

        rvGroupSchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduleAdapter = new ScheduleAdapter(new ArrayList<>(), schedule ->
                ScheduleDetailSheet.newInstance(schedule)
                        .show(getChildFragmentManager(), "detail"));
        rvGroupSchedules.setAdapter(scheduleAdapter);
        db = FirebaseFirestore.getInstance();

        Calendar today = Calendar.getInstance();
        currentYear  = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);

        loadGroupInfo();
        updateCalendar();

        view.findViewById(R.id.iv_logo).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).navigateToHome();
        });
        view.findViewById(R.id.tv_logo).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).navigateToHome();
        });

        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            updateCalendar();
        });

        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            updateCalendar();
        });

        btnWeekly.setOnClickListener(v  -> setMode(true));
        btnMonthly.setOnClickListener(v -> setMode(false));

        view.findViewById(R.id.btn_add_participant).setOnClickListener(v -> showInviteDialog());

        // 일정 생성: groupId 전달 → 완료 후 지도 페이지로 이동
        view.findViewById(R.id.fab_add_schedule).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(requireContext(), CreateScheduleActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });

        // 그룹 나가기
        view.findViewById(R.id.btn_leave_group).setOnClickListener(v -> leaveGroup());
    }

    // 테스트 모드에서 처리해야 하는 그룹인지 확인
    private boolean isTestGroup() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean("test_login", false)
                && (TestData.GROUP_ID.equals(groupId)
                    || (groupId != null && groupId.startsWith("runtime_group_")));
    }

    // ── 그룹 정보 로드 ──
    private void loadGroupInfo() {
        if (isTestGroup()) {
            // 기본 테스트 그룹
            if (TestData.GROUP_ID.equals(groupId)) {
                List<Group> groups = TestData.getGroups();
                if (!groups.isEmpty()) tvGroupName.setText(groups.get(0).getGroupName());
                loadParticipantsFromTestData();
            } else {
                // runtime 그룹 — runtimeGroups에서 이름 검색
                for (Group g : TestData.runtimeGroups) {
                    if (groupId.equals(g.getGroupId())) {
                        tvGroupName.setText(g.getGroupName());
                        break;
                    }
                }
                loadParticipantsForCurrentUser();
            }
            loadGroupSchedules();
            return;
        }

        if (groupId == null || groupId.isEmpty()) return;

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("groupName");
                        tvGroupName.setText(name != null ? name : "");
                        List<String> members = (List<String>) doc.get("members");
                        if (members != null) loadParticipants(members);
                    }
                });

        loadGroupSchedules();
    }

    // ── 참여자 아이콘 (테스트 모드 - 기본 그룹) ──
    private void loadParticipantsFromTestData() {
        layoutParticipants.removeAllViews();
        for (Friend f : TestData.getGroupMembers()) {
            addParticipantView(f.getUid(), f.getNickname());
        }
    }

    // ── 참여자 아이콘 (runtime 그룹 - 현재 사용자만) ──
    private void loadParticipantsForCurrentUser() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        String myId = prefs.getString("test_id", "");
        layoutParticipants.removeAllViews();
        addParticipantView(myId, TestData.getNickname(myId));
    }

    // ── 참여자 아이콘 (Firebase 모드) ──
    private void loadParticipants(List<String> memberIds) {
        layoutParticipants.removeAllViews();
        for (int i = 0; i < memberIds.size(); i++) {
            addParticipantView("", null);
        }
    }

    // ── 참여자 원형 아이콘 동적 추가 ──
    public void addParticipantView(String uid, String name) {
        ImageView avatar = new ImageView(getContext());
        int sizePx = dp(28);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        if (layoutParticipants.getChildCount() > 0) {
            lp.setMarginStart(dp(-6));
        }
        avatar.setLayoutParams(lp);
        avatar.setBackgroundResource(R.drawable.bg_profile_circle);
        avatar.setImageResource(TestData.getProfileDrawable(uid));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        layoutParticipants.addView(avatar);
    }

    // ── 모드 전환 ──
    private void setMode(boolean weekly) {
        isWeeklyMode = weekly;
        if (weekly) {
            btnWeekly.setBackgroundResource(R.drawable.bg_calendar_tab_active);
            btnWeekly.setTextColor(Color.parseColor("#827EFF"));
            btnMonthly.setBackground(null);
            btnMonthly.setTextColor(Color.parseColor("#FFFFFF"));
            gvCalendar.setVisibility(View.GONE);
            layoutWeekly.setVisibility(View.VISIBLE);
            buildWeekView();
        } else {
            btnMonthly.setBackgroundResource(R.drawable.bg_calendar_tab_active);
            btnMonthly.setTextColor(Color.parseColor("#827EFF"));
            btnWeekly.setBackground(null);
            btnWeekly.setTextColor(Color.parseColor("#FFFFFF"));
            gvCalendar.setVisibility(View.VISIBLE);
            layoutWeekly.setVisibility(View.GONE);
        }
    }

    // ── 달력 업데이트 ──
    private void updateCalendar() {
        tvMonthName.setText(MONTH_NAMES[currentMonth]);
        tvYear.setText(String.valueOf(currentYear));

        List<CalendarDay> days = buildMonthDays();
        GroupCalendarAdapter adapter = new GroupCalendarAdapter(
                requireContext(), days, eventColorMap, currentYear, currentMonth);
        gvCalendar.setAdapter(adapter);

        gvCalendar.setOnItemClickListener((parent, v, position, id) -> {
            CalendarDay day = days.get(position);
            if (day.isCurrentMonth) {
                String dateStr = currentYear + "년 " + (currentMonth + 1) + "월 " + day.day + "일";
                tvSelectedDate.setText(dateStr);
                loadSchedulesForDate(currentYear, currentMonth + 1, day.day);
            }
        });
    }

    // ── 월 날짜 리스트 생성 ──
    private List<CalendarDay> buildMonthDays() {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int prevMonthDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = firstDayOfWeek - 1; i >= 0; i--) {
            days.add(new CalendarDay(prevMonthDays - i, false));
        }

        Calendar today = Calendar.getInstance();
        for (int d = 1; d <= daysInMonth; d++) {
            boolean isToday = (currentYear == today.get(Calendar.YEAR)
                    && currentMonth == today.get(Calendar.MONTH)
                    && d == today.get(Calendar.DAY_OF_MONTH));
            days.add(new CalendarDay(d, true, isToday));
        }

        int remaining = 42 - days.size();
        for (int d = 1; d <= remaining; d++) {
            days.add(new CalendarDay(d, false));
        }
        return days;
    }

    // ── 주간 뷰 생성 ──
    private void buildWeekView() {
        layoutWeekDays.removeAllViews();
        layoutTimeRows.removeAllViews();
        layoutEvents.removeAllViews();

        // 이번 주 일요일 계산
        Calendar weekStart = Calendar.getInstance();
        int dayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK) - 1; // 0=일
        weekStart.add(Calendar.DAY_OF_MONTH, -dayOfWeek);

        SimpleDateFormat sdf = new SimpleDateFormat("d", Locale.getDefault());
        String[] dayLabels = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

        // ── 요일/날짜 헤더: "Sun / 13" 형식 ──
        // 좌측 30dp 공백은 XML에서 형제 View로 처리 → layoutWeekDays에는 7컬럼만 추가
        for (int i = 0; i < 7; i++) {
            TextView tvHeader = new TextView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            tvHeader.setLayoutParams(lp);

            // "Sun / 13" 형식으로 조합
            String dateNum = sdf.format(weekStart.getTime());
            tvHeader.setText(dayLabels[i] + " / " + dateNum);
            tvHeader.setTextSize(10f);
            tvHeader.setGravity(Gravity.CENTER);

            // 일요일=빨강, 토요일=파랑, 평일=회색
            if (i == 0)      tvHeader.setTextColor(Color.parseColor("#FF5858"));
            else if (i == 6) tvHeader.setTextColor(Color.parseColor("#526AE4"));
            else             tvHeader.setTextColor(Color.parseColor("#535353"));

            // 오늘이 포함된 컬럼은 보라색 강조
            Calendar today = Calendar.getInstance();
            if (weekStart.get(Calendar.YEAR)  == today.get(Calendar.YEAR)
             && weekStart.get(Calendar.MONTH) == today.get(Calendar.MONTH)
             && weekStart.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                tvHeader.setTextColor(Color.parseColor("#827EFF"));
                tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            layoutWeekDays.addView(tvHeader);
            weekStart.add(Calendar.DAY_OF_MONTH, 1);
        }

        // ── 시간 그리드 08:00 ~ 21:00 ──
        for (int hour = 8; hour <= 21; hour++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvTime = new TextView(getContext());
            tvTime.setLayoutParams(new LinearLayout.LayoutParams(dp(30),
                    LinearLayout.LayoutParams.MATCH_PARENT));
            tvTime.setText(String.format(Locale.getDefault(), "%02d", hour));
            tvTime.setTextSize(9f);
            tvTime.setTextColor(Color.parseColor("#A2A2A2"));
            tvTime.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            tvTime.setPadding(0, dp(2), 0, 0);

            LinearLayout gridArea = new LinearLayout(getContext());
            gridArea.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            gridArea.setOrientation(LinearLayout.VERTICAL);

            View divider = new View(getContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
            divider.setBackgroundColor(Color.parseColor("#E6E6E6"));
            gridArea.addView(divider);

            row.addView(tvTime);
            row.addView(gridArea);
            layoutTimeRows.addView(row);
        }

        // TODO: 그룹 일정 이벤트 블록 로드
        loadWeeklyGroupEvents();
    }

    // ── 주간 그룹 이벤트 블록 로드 ──
    private void loadWeeklyGroupEvents() {
        if (isTestGroup()) {
            Calendar weekStart = Calendar.getInstance();
            int dow = weekStart.get(Calendar.DAY_OF_WEEK) - 1;
            weekStart.add(Calendar.DAY_OF_MONTH, -dow);
            Calendar weekEnd = (Calendar) weekStart.clone();
            weekEnd.add(Calendar.DAY_OF_MONTH, 6);

            for (Schedule sc : TestData.getGroupSchedules(groupId)) {
                if (sc.getStartTime() == null) continue;
                Calendar evtCal = Calendar.getInstance();
                evtCal.setTime(sc.getStartTime().toDate());
                if (evtCal.before(weekStart) || evtCal.after(weekEnd)) continue;
                int dayIndex  = evtCal.get(Calendar.DAY_OF_WEEK) - 1;
                float startHr = evtCal.get(Calendar.HOUR_OF_DAY) + evtCal.get(Calendar.MINUTE) / 60f;
                float endHr   = startHr + 1f;
                if (sc.getEndTime() != null) {
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(sc.getEndTime().toDate());
                    endHr = endCal.get(Calendar.HOUR_OF_DAY) + endCal.get(Calendar.MINUTE) / 60f;
                }
                addEventBlock(dayIndex, startHr, Math.max(0.5f, endHr - startHr),
                        "#827EFF", sc.getTitle() != null ? sc.getTitle() : "");
            }
            return;
        }

        if (groupId == null || groupId.isEmpty()) return;

        Calendar weekStart = Calendar.getInstance();
        int dow = weekStart.get(Calendar.DAY_OF_WEEK) - 1;
        weekStart.add(Calendar.DAY_OF_MONTH, -dow);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);

        Timestamp tsStart = new Timestamp(weekStart.getTime());
        Timestamp tsEnd   = new Timestamp(weekEnd.getTime());

        db.collection("schedules")
                .whereArrayContains("sharedGroups", groupId)
                .whereGreaterThanOrEqualTo("startTime", tsStart)
                .whereLessThanOrEqualTo("startTime", tsEnd)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("startTime");
                        if (ts == null) continue;
                        Calendar evtCal = Calendar.getInstance();
                        evtCal.setTime(ts.toDate());
                        int dayIndex  = evtCal.get(Calendar.DAY_OF_WEEK) - 1;
                        float startHr = evtCal.get(Calendar.HOUR_OF_DAY)
                                + evtCal.get(Calendar.MINUTE) / 60f;

                        Timestamp tsE = doc.getTimestamp("endTime");
                        float endHr = startHr + 1f;
                        if (tsE != null) {
                            Calendar endCal = Calendar.getInstance();
                            endCal.setTime(tsE.toDate());
                            endHr = endCal.get(Calendar.HOUR_OF_DAY) + endCal.get(Calendar.MINUTE) / 60f;
                        }
                        float duration = Math.max(0.5f, endHr - startHr);
                        String title = doc.getString("title");
                        addEventBlock(dayIndex, startHr, duration, "#827EFF",
                                title != null ? title : "");
                    }
                });
    }

    /**
     * layout_events에 이벤트 블록 추가
     * @param dayIndex  0=일 ~ 6=토
     * @param startHour 시작 시각 (08 ~ 21)
     * @param durationH 지속 시간 (시간 단위)
     * @param colorHex  블록 색상
     * @param label     일정명
     */
    public void addEventBlock(int dayIndex, float startHour, float durationH,
                              String colorHex, String label) {
        int rowHeightPx    = dp(40);
        int startOffsetPx  = (int) ((startHour - 8) * rowHeightPx);
        int blockHeightPx  = (int) (durationH * rowHeightPx);

        layoutEvents.post(() -> {
            int totalWidth = layoutEvents.getWidth() - dp(30);
            int colWidth   = totalWidth / 7;
            int leftOffset = dp(30) + dayIndex * colWidth + dp(2);

            TextView block = new TextView(getContext());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    colWidth - dp(4), blockHeightPx);
            params.leftMargin = leftOffset;
            params.topMargin  = startOffsetPx;
            block.setLayoutParams(params);
            block.setText(label);
            block.setTextSize(8f);
            block.setTextColor(Color.WHITE);
            block.setPadding(dp(4), dp(4), dp(4), dp(4));
            block.setBackgroundColor(Color.parseColor(colorHex));
            layoutEvents.addView(block);
        });
    }

    // ── 그룹 일정 로드 (달력 이벤트 점 표시용) ──
    private void loadGroupSchedules() {
        if (isTestGroup()) {
            eventColorMap.clear();
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (Schedule sc : TestData.getGroupSchedules(groupId)) {
                if (sc.getStartTime() == null) continue;
                String key = keyFmt.format(sc.getStartTime().toDate());
                if (!eventColorMap.containsKey(key)) eventColorMap.put(key, new ArrayList<>());
                eventColorMap.get(key).add("#827EFF");
            }
            updateCalendar();
            return;
        }

        if (groupId == null || groupId.isEmpty()) return;

        db.collection("schedules")
                .whereArrayContains("sharedGroups", groupId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    eventColorMap.clear();
                    SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("startTime");
                        if (ts == null) continue;
                        String key = keyFmt.format(ts.toDate());
                        if (!eventColorMap.containsKey(key)) {
                            eventColorMap.put(key, new ArrayList<>());
                        }
                        eventColorMap.get(key).add("#827EFF");
                    }
                    updateCalendar();
                });
    }

    private void loadSchedulesForDate(int year, int month, int day) {
        if (isTestGroup()) {
            List<Schedule> result = new ArrayList<>();
            for (Schedule sc : TestData.getGroupSchedules(groupId)) {
                if (sc.getStartTime() == null) continue;
                Calendar c = Calendar.getInstance();
                c.setTime(sc.getStartTime().toDate());
                if (c.get(Calendar.YEAR) == year
                        && c.get(Calendar.MONTH) + 1 == month
                        && c.get(Calendar.DAY_OF_MONTH) == day) {
                    result.add(sc);
                }
            }
            scheduleAdapter.updateList(result);
            return;
        }

        if (groupId == null || groupId.isEmpty()) return;

        Calendar start = Calendar.getInstance();
        start.set(year, month - 1, day, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(year, month - 1, day, 23, 59, 59);
        end.set(Calendar.MILLISECOND, 999);

        db.collection("schedules")
                .whereArrayContains("sharedGroups", groupId)
                .whereGreaterThanOrEqualTo("startTime", new Timestamp(start.getTime()))
                .whereLessThanOrEqualTo("startTime", new Timestamp(end.getTime()))
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

    // ── 그룹 초대 다이얼로그 ─────────────────────────────────────────────────

    private void showInviteDialog() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);

        if (prefs.getBoolean("test_login", false)) {
            // 테스트 모드: 현재 그룹에 없는 친구 목록 표시 (테스트 데이터 4명 모두 이미 멤버라 알림 표시)
            String myId = prefs.getString("test_id", "");
            List<Friend> friends = TestData.getFriends(myId);
            if (friends.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "초대할 수 있는 친구가 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[friends.size()];
            for (int i = 0; i < friends.size(); i++) names[i] = friends.get(i).getNickname();

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("그룹에 초대할 친구 선택")
                    .setItems(names, (dialog, which) ->
                            android.widget.Toast.makeText(getContext(),
                                    friends.get(which).getNickname() + "에게 초대장을 보냈습니다.",
                                    android.widget.Toast.LENGTH_SHORT).show())
                    .setNegativeButton("취소", null)
                    .show();
            return;
        }

        // Firebase 모드
        com.google.firebase.auth.FirebaseUser me =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (me == null || groupId == null) return;

        // 현재 그룹 멤버 목록 조회 → 이미 속한 친구 제외
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> {
                    List<String> currentMembers = (List<String>) groupDoc.get("members");
                    if (currentMembers == null) currentMembers = new java.util.ArrayList<>();
                    final List<String> members = new java.util.ArrayList<>(currentMembers);

                    db.collection("users").document(me.getUid())
                            .collection("friends").get()
                            .addOnSuccessListener(snap -> {
                                List<String> uids  = new java.util.ArrayList<>();
                                List<String> names = new java.util.ArrayList<>();
                                for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                                    String uid  = d.getString("uid");
                                    String nick = d.getString("nickname");
                                    if (uid != null && !members.contains(uid)) {
                                        uids.add(uid);
                                        names.add(nick != null ? nick : uid);
                                    }
                                }
                                if (uids.isEmpty()) {
                                    android.widget.Toast.makeText(getContext(),
                                            "초대할 수 있는 친구가 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                String[] nameArr = names.toArray(new String[0]);
                                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("그룹에 초대할 친구 선택")
                                        .setItems(nameArr, (dialog, which) -> {
                                            String targetUid  = uids.get(which);
                                            String targetName = names.get(which);
                                            // groupInvitations 컬렉션에 초대 문서 생성
                                            java.util.Map<String, Object> invite = new java.util.HashMap<>();
                                            invite.put("groupId",   groupId);
                                            invite.put("groupName", tvGroupName.getText().toString());
                                            invite.put("fromUid",   me.getUid());
                                            invite.put("toUid",     targetUid);
                                            invite.put("status",    "pending");
                                            invite.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                            db.collection("groupInvitations").add(invite)
                                                    .addOnSuccessListener(ref ->
                                                            android.widget.Toast.makeText(getContext(),
                                                                    targetName + "에게 초대장을 보냈습니다.",
                                                                    android.widget.Toast.LENGTH_SHORT).show())
                                                    .addOnFailureListener(e ->
                                                            android.widget.Toast.makeText(getContext(),
                                                                    "초대 실패: " + e.getMessage(),
                                                                    android.widget.Toast.LENGTH_SHORT).show());
                                        })
                                        .setNegativeButton("취소", null)
                                        .show();
                            });
                });
    }

    // ── 그룹 나가기 ──────────────────────────────────────────────────────────

    private void leaveGroup() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            TestData.leftGroups.add(groupId);
            TestData.runtimeGroups.removeIf(g -> g.getGroupId().equals(groupId));
            TestData.runtimeGroupMembers.remove(groupId);
            android.widget.Toast.makeText(getContext(), "그룹에서 나갔습니다.", android.widget.Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
            return;
        }

        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || groupId == null || groupId.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    java.util.List<String> members = (java.util.List<String>) doc.get("members");
                    if (members == null) return;
                    members.remove(user.getUid());
                    FirebaseFirestore.getInstance().collection("groups").document(groupId)
                            .update("members", members)
                            .addOnSuccessListener(v -> {
                                android.widget.Toast.makeText(getContext(),
                                        "그룹에서 나갔습니다.", android.widget.Toast.LENGTH_SHORT).show();
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).navigateToHome();
                                }
                            });
                });
    }

    // dp → px 변환
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    // ── 날짜 데이터 모델 ──
    static class CalendarDay {
        int day;
        boolean isCurrentMonth;
        boolean isToday;

        CalendarDay(int day, boolean isCurrentMonth) {
            this.day = day;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = false;
        }

        CalendarDay(int day, boolean isCurrentMonth, boolean isToday) {
            this.day = day;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
        }
    }

    // ── 그룹 캘린더 GridView 어댑터 (이벤트 색상 점 포함) ──
    static class GroupCalendarAdapter extends BaseAdapter {

        private final Context context;
        private final List<CalendarDay> days;
        private final Map<String, List<String>> eventColorMap;
        private final int year, month; // 표시 중인 연/월 (month는 0-based)

        GroupCalendarAdapter(Context context, List<CalendarDay> days,
                             Map<String, List<String>> eventColorMap,
                             int year, int month) {
            this.context = context;
            this.days = days;
            this.eventColorMap = eventColorMap;
            this.year  = year;
            this.month = month;
        }

        @Override public int getCount()  { return days.size(); }
        @Override public Object getItem(int pos) { return days.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));

            CalendarDay day = days.get(position);

            // 날짜 숫자
            TextView tvDay = new TextView(context);
            tvDay.setText(String.format(Locale.getDefault(), "%d", day.day));
            tvDay.setGravity(Gravity.CENTER);
            tvDay.setTextSize(10f);

            if (!day.isCurrentMonth) {
                tvDay.setTextColor(Color.parseColor("#A2A2A2"));
            } else {
                int col = position % 7;
                if (col == 0)      tvDay.setTextColor(Color.parseColor("#FF5858"));
                else if (col == 6) tvDay.setTextColor(Color.parseColor("#526AE4"));
                else               tvDay.setTextColor(Color.parseColor("#000000"));
            }

            if (day.isToday) {
                tvDay.setBackgroundResource(R.drawable.bg_today_circle);
                tvDay.setTextColor(Color.WHITE);
            }

            cell.addView(tvDay);

            // 이벤트 색상 점 — 해당 날짜에 일정이 있으면 보라 점 표시
            if (day.isCurrentMonth) {
                String dateKey = String.format(Locale.getDefault(), "%d-%02d-%02d",
                        year, month + 1, day.day);
                List<String> colors = eventColorMap.get(dateKey);
                if (colors != null && !colors.isEmpty()) {
                    View dot = new View(context);
                    LinearLayout.LayoutParams dotLp =
                            new LinearLayout.LayoutParams(dp(5), dp(5));
                    dotLp.gravity = Gravity.CENTER_HORIZONTAL;
                    dotLp.topMargin = dp(1);
                    dot.setLayoutParams(dotLp);
                    android.graphics.drawable.GradientDrawable circle =
                            new android.graphics.drawable.GradientDrawable();
                    circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    circle.setColor(Color.parseColor(day.isToday ? "#FFFFFF" : colors.get(0)));
                    dot.setBackground(circle);
                    cell.addView(dot);
                }
            }

            return cell;
        }

        private int dp(int dp) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dp,
                    context.getResources().getDisplayMetrics());
        }
    }
}
