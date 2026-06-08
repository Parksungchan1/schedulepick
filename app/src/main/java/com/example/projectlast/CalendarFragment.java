package com.example.projectlast;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private ActivityResultLauncher<android.content.Intent> createScheduleLauncher;

    private int currentYear, currentMonth;

    private TextView tvMonthName, tvYear, tvSelectedDate, tvWeeklyUsername;
    private TextView btnWeekly, btnMonthly;
    private GridView gvCalendar;
    private LinearLayout layoutWeekly, layoutWeekDays, layoutTimeRows;
    private RelativeLayout layoutEvents;
    private ImageView ivWeeklyProfile;
    private RecyclerView rvDaySchedules;

    private boolean isWeeklyMode = false;
    // day → list of dot colors (purple=개인, orange=그룹)
    private final Map<Integer, List<Integer>> scheduleDots = new HashMap<>();
    private ScheduleAdapter scheduleAdapter;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final String[] MONTH_NAMES = {
        "JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
        "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createScheduleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (isAdded() && getView() != null) updateCalendar();
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) updateCalendar();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMonthName       = view.findViewById(R.id.tv_month_name);
        tvYear            = view.findViewById(R.id.tv_year);
        tvSelectedDate    = view.findViewById(R.id.tv_selected_date);
        tvWeeklyUsername  = view.findViewById(R.id.tv_weekly_username);
        btnWeekly         = view.findViewById(R.id.btn_weekly);
        btnMonthly        = view.findViewById(R.id.btn_monthly);
        gvCalendar        = view.findViewById(R.id.gv_calendar);
        layoutWeekly      = view.findViewById(R.id.layout_weekly);
        layoutWeekDays    = view.findViewById(R.id.layout_week_days);
        layoutTimeRows    = view.findViewById(R.id.layout_time_rows);
        layoutEvents      = view.findViewById(R.id.layout_events);
        ivWeeklyProfile   = view.findViewById(R.id.iv_weekly_profile);
        rvDaySchedules    = view.findViewById(R.id.rv_day_schedules);

        rvDaySchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduleAdapter = new ScheduleAdapter(new ArrayList<>(), schedule ->
                ScheduleDetailSheet.newInstance(schedule)
                        .show(getChildFragmentManager(), "detail"));
        rvDaySchedules.setAdapter(scheduleAdapter);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Calendar today = Calendar.getInstance();
        currentYear  = today.get(Calendar.YEAR);
        currentMonth = today.get(Calendar.MONTH);

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

        btnWeekly.setOnClickListener(v -> setMode(true));
        btnMonthly.setOnClickListener(v -> setMode(false));

        // 초기 상태: Monthly 활성화
        btnMonthly.setBackgroundResource(R.drawable.bg_calendar_tab_active);
        btnMonthly.setTextColor(Color.parseColor("#827EFF"));
        btnWeekly.setBackground(null);
        btnWeekly.setTextColor(Color.parseColor("#FFFFFF"));

        view.findViewById(R.id.fab_add_schedule).setOnClickListener(v ->
                createScheduleLauncher.launch(
                        new android.content.Intent(requireContext(), CreateScheduleActivity.class)));
    }

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

    // 월 변경 시 → 개인+그룹 일정 점 로드 후 그리드 렌더링
    private void updateCalendar() {
        tvMonthName.setText(MONTH_NAMES[currentMonth]);
        tvYear.setText(String.valueOf(currentYear));
        scheduleDots.clear();

        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);

        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");
            // 내가 참여한 모든 일정(개인+그룹) 로드
            for (Schedule sc : TestData.getAllSchedulesForUser(testId)) {
                if (sc.getStartTime() == null) continue;
                Calendar cal = Calendar.getInstance();
                cal.setTime(sc.getStartTime().toDate());
                if (cal.get(Calendar.YEAR) != currentYear
                        || cal.get(Calendar.MONTH) != currentMonth) continue;
                int d = cal.get(Calendar.DAY_OF_MONTH);
                if (!scheduleDots.containsKey(d)) scheduleDots.put(d, new ArrayList<>());
                int color = (sc.getGroupId() != null)
                        ? Color.parseColor("#FF7043") : Color.parseColor("#827EFF");
                if (!scheduleDots.get(d).contains(color)) scheduleDots.get(d).add(color);
            }
            renderCalendarGrid();
            return;
        }

        if (currentUser == null) { renderCalendarGrid(); return; }

        Calendar monthStart = Calendar.getInstance();
        monthStart.set(currentYear, currentMonth, 1, 0, 0, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        Calendar monthEnd = Calendar.getInstance();
        monthEnd.set(currentYear, currentMonth,
                monthStart.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        monthEnd.set(Calendar.MILLISECOND, 999);

        db.collection("schedules")
                .whereArrayContains("participants", currentUser.getUid())
                .whereGreaterThanOrEqualTo("startTime", new Timestamp(monthStart.getTime()))
                .whereLessThanOrEqualTo("startTime", new Timestamp(monthEnd.getTime()))
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Timestamp ts = doc.getTimestamp("startTime");
                        if (ts == null) continue;
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(ts.toDate());
                        int d = cal.get(Calendar.DAY_OF_MONTH);
                        if (!scheduleDots.containsKey(d)) scheduleDots.put(d, new ArrayList<>());
                        String gid = doc.getString("groupId");
                        int color = (gid != null)
                                ? Color.parseColor("#FF7043") : Color.parseColor("#827EFF");
                        if (!scheduleDots.get(d).contains(color)) scheduleDots.get(d).add(color);
                    }
                    renderCalendarGrid();
                })
                .addOnFailureListener(e -> renderCalendarGrid());
    }

    private void renderCalendarGrid() {
        List<CalendarDay> days = buildMonthDays();
        CalendarAdapter adapter = new CalendarAdapter(requireContext(), days, scheduleDots);
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

    private List<CalendarDay> buildMonthDays() {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int prevMonthDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = firstDayOfWeek - 1; i >= 0; i--)
            days.add(new CalendarDay(prevMonthDays - i, false));

        Calendar today = Calendar.getInstance();
        for (int d = 1; d <= daysInMonth; d++) {
            boolean isToday = (currentYear == today.get(Calendar.YEAR)
                    && currentMonth == today.get(Calendar.MONTH)
                    && d == today.get(Calendar.DAY_OF_MONTH));
            days.add(new CalendarDay(d, true, isToday));
        }

        int remaining = 42 - days.size();
        for (int d = 1; d <= remaining; d++)
            days.add(new CalendarDay(d, false));

        return days;
    }

    private void buildWeekView() {
        layoutWeekDays.removeAllViews();
        layoutTimeRows.removeAllViews();
        layoutEvents.removeAllViews();

        Calendar weekStart = Calendar.getInstance();
        int dayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK) - 1;
        weekStart.add(Calendar.DAY_OF_MONTH, -dayOfWeek);

        SimpleDateFormat sdf = new SimpleDateFormat("d", Locale.getDefault());
        String[] dayLabels = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

        for (int i = 0; i < 7; i++) {
            LinearLayout dayCol = new LinearLayout(getContext());
            dayCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            dayCol.setOrientation(LinearLayout.VERTICAL);
            dayCol.setGravity(android.view.Gravity.CENTER);

            TextView tvDay = new TextView(getContext());
            tvDay.setText(dayLabels[i]);
            tvDay.setTextSize(10f);
            tvDay.setTextColor(Color.parseColor(i == 0 ? "#FF5858" : i == 6 ? "#526AE4" : "#535353"));
            tvDay.setGravity(android.view.Gravity.CENTER);

            TextView tvDate = new TextView(getContext());
            tvDate.setText(sdf.format(weekStart.getTime()));
            tvDate.setTextSize(10f);
            tvDate.setTextColor(Color.parseColor("#000000"));
            tvDate.setGravity(android.view.Gravity.CENTER);

            dayCol.addView(tvDay);
            dayCol.addView(tvDate);
            layoutWeekDays.addView(dayCol);
            weekStart.add(Calendar.DAY_OF_MONTH, 1);
        }

        int rowHeightPx = dp(40);
        int startHour = 8;
        int endHour   = 21;

        for (int hour = startHour; hour <= endHour; hour++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, rowHeightPx));
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvTime = new TextView(getContext());
            tvTime.setLayoutParams(new LinearLayout.LayoutParams(dp(30),
                    LinearLayout.LayoutParams.MATCH_PARENT));
            tvTime.setText(String.format(Locale.getDefault(), "%02d", hour));
            tvTime.setTextSize(9f);
            tvTime.setTextColor(Color.parseColor("#A2A2A2"));
            tvTime.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
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

        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            tvWeeklyUsername.setText(prefs.getString("test_nickname", "") + "님");
        } else if (currentUser != null && db != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String nickname = doc.getString("nickname");
                            tvWeeklyUsername.setText((nickname != null ? nickname : "") + "님");
                        }
                    });
        }

        loadWeeklyEvents();
    }

    private void loadWeeklyEvents() {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);

        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");
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
            weekEnd.set(Calendar.SECOND, 59);

            // 개인 + 그룹 일정 모두 표시
            for (Schedule sc : TestData.getAllSchedulesForUser(testId)) {
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
                String color = (sc.getGroupId() != null) ? "#FF7043" : "#827EFF";
                addEventBlock(dayIndex, startHr, Math.max(0.5f, endHr - startHr),
                        color, sc.getTitle() != null ? sc.getTitle() : "");
            }
            return;
        }

        if (currentUser == null) return;

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
        weekEnd.set(Calendar.SECOND, 59);

        Timestamp tsStart = new Timestamp(weekStart.getTime());
        Timestamp tsEnd   = new Timestamp(weekEnd.getTime());

        db.collection("schedules")
                .whereArrayContains("participants", currentUser.getUid())
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
                        String gid = doc.getString("groupId");
                        String color = (gid != null) ? "#FF7043" : "#827EFF";
                        addEventBlock(dayIndex, startHr, duration, color,
                                title != null ? title : "");
                    }
                });
    }

    private void addEventBlock(int dayIndex, float startHour, float durationH,
                               String colorHex, String label) {
        int rowHeightPx = dp(40);
        int startOffsetPx = (int) ((startHour - 8) * rowHeightPx);
        int blockHeightPx = (int) (durationH * rowHeightPx);

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

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private void loadSchedulesForDate(int year, int month, int day) {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");
            // 개인+그룹 통합 조회
            scheduleAdapter.updateList(
                    TestData.getAllSchedulesForUserForDate(testId, year, month, day));
            return;
        }

        if (currentUser == null) return;

        Calendar start = Calendar.getInstance();
        start.set(year, month - 1, day, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = Calendar.getInstance();
        end.set(year, month - 1, day, 23, 59, 59);
        end.set(Calendar.MILLISECOND, 999);

        db.collection("schedules")
                .whereArrayContains("participants", currentUser.getUid())
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

    // ── 날짜 데이터 모델 ──
    static class CalendarDay {
        int day;
        boolean isCurrentMonth;
        boolean isToday;

        CalendarDay(int day, boolean isCurrentMonth) {
            this.day = day; this.isCurrentMonth = isCurrentMonth; this.isToday = false;
        }
        CalendarDay(int day, boolean isCurrentMonth, boolean isToday) {
            this.day = day; this.isCurrentMonth = isCurrentMonth; this.isToday = isToday;
        }
    }

    // ── 캘린더 GridView 어댑터 (날짜 + 일정 점 표시) ──
    static class CalendarAdapter extends BaseAdapter {

        private final Context context;
        private final List<CalendarDay> days;
        private final Map<Integer, List<Integer>> dotsMap;

        CalendarAdapter(Context context, List<CalendarDay> days,
                        Map<Integer, List<Integer>> dotsMap) {
            this.context = context;
            this.days = days;
            this.dotsMap = dotsMap;
        }

        @Override public int getCount()              { return days.size(); }
        @Override public Object getItem(int pos)     { return days.get(pos); }
        @Override public long getItemId(int pos)     { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CalendarDay day = days.get(position);

            // 셀 컨테이너
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            cell.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40)));

            // 날짜 원
            FrameLayout dayCircle = new FrameLayout(context);
            LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(dp(26), dp(26));
            circleLp.topMargin = dp(2);
            dayCircle.setLayoutParams(circleLp);

            TextView tvDate = new TextView(context);
            tvDate.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            tvDate.setText(String.valueOf(day.day));
            tvDate.setGravity(android.view.Gravity.CENTER);
            tvDate.setTextSize(10f);

            if (!day.isCurrentMonth) {
                tvDate.setTextColor(Color.parseColor("#A2A2A2"));
            } else {
                int col = position % 7;
                if (col == 0)      tvDate.setTextColor(Color.parseColor("#FF5858"));
                else if (col == 6) tvDate.setTextColor(Color.parseColor("#526AE4"));
                else               tvDate.setTextColor(Color.parseColor("#000000"));
            }

            if (day.isToday) {
                dayCircle.setBackgroundResource(R.drawable.bg_today_circle);
                tvDate.setTextColor(Color.WHITE);
            }

            dayCircle.addView(tvDate);
            cell.addView(dayCircle);

            // 일정 점 (현재 달만)
            if (day.isCurrentMonth) {
                List<Integer> colors = dotsMap.get(day.day);
                if (colors != null && !colors.isEmpty()) {
                    LinearLayout dotsRow = new LinearLayout(context);
                    dotsRow.setOrientation(LinearLayout.HORIZONTAL);
                    dotsRow.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams dotsLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, dp(5));
                    dotsLp.topMargin = dp(2);
                    dotsRow.setLayoutParams(dotsLp);

                    for (int color : colors) {
                        View dot = new View(context);
                        LinearLayout.LayoutParams dotLp =
                                new LinearLayout.LayoutParams(dp(4), dp(4));
                        dotLp.setMargins(dp(1), 0, dp(1), 0);
                        dot.setLayoutParams(dotLp);
                        GradientDrawable circle = new GradientDrawable();
                        circle.setShape(GradientDrawable.OVAL);
                        circle.setColor(color);
                        dot.setBackground(circle);
                        dotsRow.addView(dot);
                    }
                    cell.addView(dotsRow);
                }
            }

            return cell;
        }

        private int dp(int dp) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    context.getResources().getDisplayMetrics());
        }
    }
}
