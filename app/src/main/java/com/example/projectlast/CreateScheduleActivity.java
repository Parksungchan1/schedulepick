package com.example.projectlast;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateScheduleActivity extends AppCompatActivity {

    private TextView tvStartDate, tvStartTime, tvStartMonth;
    private TextView tvEndDate, tvEndTime, tvEndMonth;
    private TextView btnStartAm, btnStartPm, btnEndAm, btnEndPm;
    private TextView tvCalMonthName, tvCalYear, tvCategory;
    private GridView gvScheduleCalendar;
    private LinearLayout layoutGroupPills, layoutScheduleParticipants;

    private Calendar startCalendar   = Calendar.getInstance();
    private Calendar endCalendar     = Calendar.getInstance();
    private Calendar displayCalendar = Calendar.getInstance();
    private boolean startIsAm = true;
    private boolean endIsAm   = true;

    private Calendar rangeStart = null;
    private Calendar rangeEnd   = null;

    private String selectedCategory = null;
    private final List<String> selectedGroupIds = new ArrayList<>();
    private final List<String[]> selectedParticipants = new ArrayList<>(); // [uid, nickname]

    private ScheduleCalendarAdapter calendarAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String[] CATEGORIES = {"회사", "학교", "팀모임", "개인", "기타"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        bindViews();
        setupDateTimePickers();
        setupAmPmToggles();
        setupCalendar();
        setupCategory();
        setupParticipantButton();
        setupCreateButton();
        loadUserGroups();
    }

    private void bindViews() {
        tvStartDate   = findViewById(R.id.tv_start_date);
        tvStartTime   = findViewById(R.id.tv_start_time);
        tvStartMonth  = findViewById(R.id.tv_start_month);
        tvEndDate     = findViewById(R.id.tv_end_date);
        tvEndTime     = findViewById(R.id.tv_end_time);
        tvEndMonth    = findViewById(R.id.tv_end_month);
        btnStartAm    = findViewById(R.id.btn_start_am);
        btnStartPm    = findViewById(R.id.btn_start_pm);
        btnEndAm      = findViewById(R.id.btn_end_am);
        btnEndPm      = findViewById(R.id.btn_end_pm);
        tvCalMonthName = findViewById(R.id.tv_cal_month_name);
        tvCalYear      = findViewById(R.id.tv_cal_year);
        tvCategory     = findViewById(R.id.tv_category);
        gvScheduleCalendar         = findViewById(R.id.gv_schedule_calendar);
        layoutGroupPills           = findViewById(R.id.layout_group_pills);
        layoutScheduleParticipants = findViewById(R.id.layout_schedule_participants);
    }

    private void setupDateTimePickers() {
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        tvStartTime.setOnClickListener(v -> showTimePicker(true));
        tvEndTime.setOnClickListener(v -> showTimePicker(false));

        findViewById(R.id.btn_start_month_prev).setOnClickListener(v -> {
            startCalendar.add(Calendar.MONTH, -1);
            updateStartDateDisplay();
        });
        findViewById(R.id.btn_start_month_next).setOnClickListener(v -> {
            startCalendar.add(Calendar.MONTH, 1);
            updateStartDateDisplay();
        });
        findViewById(R.id.btn_end_month_prev).setOnClickListener(v -> {
            endCalendar.add(Calendar.MONTH, -1);
            updateEndDateDisplay();
        });
        findViewById(R.id.btn_end_month_next).setOnClickListener(v -> {
            endCalendar.add(Calendar.MONTH, 1);
            updateEndDateDisplay();
        });
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = isStart ? startCalendar : endCalendar;
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    if (isStart) {
                        rangeStart = (Calendar) cal.clone();
                        updateStartDateDisplay();
                    } else {
                        rangeEnd = (Calendar) cal.clone();
                        updateEndDateDisplay();
                    }
                    calendarAdapter.notifyDataSetChanged();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker(boolean isStart) {
        Calendar cal = isStart ? startCalendar : endCalendar;
        new TimePickerDialog(this,
                (view, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    if (isStart) {
                        startIsAm = (hour < 12);
                        updateStartTimeDisplay();
                        updateAmPmToggle(btnStartAm, btnStartPm, startIsAm);
                    } else {
                        endIsAm = (hour < 12);
                        updateEndTimeDisplay();
                        updateAmPmToggle(btnEndAm, btnEndPm, endIsAm);
                    }
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
        ).show();
    }

    private void updateStartDateDisplay() {
        SimpleDateFormat monthFmt = new SimpleDateFormat("MM월", Locale.KOREA);
        SimpleDateFormat dateFmt  = new SimpleDateFormat("MM/dd", Locale.KOREA);
        tvStartMonth.setText(monthFmt.format(startCalendar.getTime()));
        tvStartDate.setText(dateFmt.format(startCalendar.getTime()));
    }

    private void updateEndDateDisplay() {
        SimpleDateFormat monthFmt = new SimpleDateFormat("MM월", Locale.KOREA);
        SimpleDateFormat dateFmt  = new SimpleDateFormat("MM/dd", Locale.KOREA);
        tvEndMonth.setText(monthFmt.format(endCalendar.getTime()));
        tvEndDate.setText(dateFmt.format(endCalendar.getTime()));
    }

    private void updateStartTimeDisplay() {
        int hour = startCalendar.get(Calendar.HOUR_OF_DAY);
        int min  = startCalendar.get(Calendar.MINUTE);
        int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;
        tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", displayHour, min));
    }

    private void updateEndTimeDisplay() {
        int hour = endCalendar.get(Calendar.HOUR_OF_DAY);
        int min  = endCalendar.get(Calendar.MINUTE);
        int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;
        tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", displayHour, min));
    }

    private void setupAmPmToggles() {
        btnStartAm.setOnClickListener(v -> {
            if (!startIsAm) {
                startIsAm = true;
                startCalendar.add(Calendar.HOUR_OF_DAY, -12);
                updateStartTimeDisplay();
                updateAmPmToggle(btnStartAm, btnStartPm, true);
            }
        });
        btnStartPm.setOnClickListener(v -> {
            if (startIsAm) {
                startIsAm = false;
                startCalendar.add(Calendar.HOUR_OF_DAY, 12);
                updateStartTimeDisplay();
                updateAmPmToggle(btnStartAm, btnStartPm, false);
            }
        });
        btnEndAm.setOnClickListener(v -> {
            if (!endIsAm) {
                endIsAm = true;
                endCalendar.add(Calendar.HOUR_OF_DAY, -12);
                updateEndTimeDisplay();
                updateAmPmToggle(btnEndAm, btnEndPm, true);
            }
        });
        btnEndPm.setOnClickListener(v -> {
            if (endIsAm) {
                endIsAm = false;
                endCalendar.add(Calendar.HOUR_OF_DAY, 12);
                updateEndTimeDisplay();
                updateAmPmToggle(btnEndAm, btnEndPm, false);
            }
        });
    }

    private void updateAmPmToggle(TextView amBtn, TextView pmBtn, boolean isAm) {
        if (isAm) {
            amBtn.setBackgroundResource(R.drawable.bg_calendar_tab_active);
            amBtn.setTextColor(Color.parseColor("#827EFF"));
            pmBtn.setBackground(null);
            pmBtn.setTextColor(Color.WHITE);
        } else {
            pmBtn.setBackgroundResource(R.drawable.bg_calendar_tab_active);
            pmBtn.setTextColor(Color.parseColor("#827EFF"));
            amBtn.setBackground(null);
            amBtn.setTextColor(Color.WHITE);
        }
    }

    private void setupCalendar() {
        calendarAdapter = new ScheduleCalendarAdapter();
        gvScheduleCalendar.setAdapter(calendarAdapter);
        updateCalendarHeader();

        findViewById(R.id.btn_cal_prev).setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, -1);
            updateCalendarHeader();
            calendarAdapter.notifyDataSetChanged();
        });
        findViewById(R.id.btn_cal_next).setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, 1);
            updateCalendarHeader();
            calendarAdapter.notifyDataSetChanged();
        });

        gvScheduleCalendar.setOnItemClickListener((parent, view, position, id) -> {
            Object tag = view.getTag();
            if (tag == null) return;
            Calendar tapped = (Calendar) tag;

            if (rangeStart == null || (rangeStart != null && rangeEnd != null)) {
                rangeStart = (Calendar) tapped.clone();
                rangeEnd   = null;
                startCalendar.set(tapped.get(Calendar.YEAR), tapped.get(Calendar.MONTH), tapped.get(Calendar.DAY_OF_MONTH));
                updateStartDateDisplay();
            } else {
                if (tapped.before(rangeStart)) {
                    rangeEnd   = (Calendar) rangeStart.clone();
                    rangeStart = (Calendar) tapped.clone();
                } else {
                    rangeEnd = (Calendar) tapped.clone();
                }
                endCalendar.set(rangeEnd.get(Calendar.YEAR), rangeEnd.get(Calendar.MONTH), rangeEnd.get(Calendar.DAY_OF_MONTH));
                updateEndDateDisplay();
            }
            calendarAdapter.notifyDataSetChanged();
        });
    }

    private void updateCalendarHeader() {
        String[] months = {"JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
                           "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"};
        tvCalMonthName.setText(months[displayCalendar.get(Calendar.MONTH)]);
        tvCalYear.setText(String.valueOf(displayCalendar.get(Calendar.YEAR)));
    }

    private void setupCategory() {
        findViewById(R.id.btn_category).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                    .setTitle("카테고리 선택")
                    .setItems(CATEGORIES, (dialog, which) -> {
                        selectedCategory = CATEGORIES[which];
                        tvCategory.setText(selectedCategory);
                        tvCategory.setTextColor(Color.parseColor("#827EFF"));
                    })
                    .show()
        );
    }

    private void loadUserGroups() {
        // 테스트 모드
        android.content.SharedPreferences prefs =
                getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            for (Group g : TestData.getGroups()) {
                addGroupPill(g.getGroupId(), g.getGroupName());
            }
            // 그룹 캘린더에서 진입 시 해당 그룹 자동 선택
            String groupIdExtra = getIntent().getStringExtra("groupId");
            if (groupIdExtra != null && !groupIdExtra.isEmpty()) {
                selectedGroupIds.add(groupIdExtra);
            }
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("groups")
                .whereArrayContains("members", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String gName = doc.getString("groupName");
                        addGroupPill(doc.getId(), gName != null ? gName : "그룹");
                    }
                });
    }

    private void addGroupPill(String groupId, String groupName) {
        boolean[] selected = {false};

        TextView pill = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(29));
        lp.setMarginEnd(dp(8));
        pill.setLayoutParams(lp);
        pill.setPadding(dp(12), 0, dp(12), 0);
        pill.setText(groupName);
        pill.setTextSize(11f);
        pill.setGravity(Gravity.CENTER);
        pill.setBackgroundResource(R.drawable.bg_schedule_group_pill);
        pill.setTextColor(Color.parseColor("#827EFF"));

        pill.setOnClickListener(v -> {
            selected[0] = !selected[0];
            if (selected[0]) {
                selectedGroupIds.add(groupId);
                pill.setBackgroundColor(Color.parseColor("#827EFF"));
                pill.setTextColor(Color.WHITE);
            } else {
                selectedGroupIds.remove(groupId);
                pill.setBackgroundResource(R.drawable.bg_schedule_group_pill);
                pill.setTextColor(Color.parseColor("#827EFF"));
            }
        });

        layoutGroupPills.addView(pill);
    }

    private void setupParticipantButton() {
        findViewById(R.id.btn_add_schedule_participant).setOnClickListener(v -> showParticipantPicker());
    }

    private void showParticipantPicker() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        boolean isTest = prefs.getBoolean("test_login", false);

        if (isTest) {
            String myId = prefs.getString("test_id", "");
            String groupIdExtra = getIntent().getStringExtra("groupId");
            List<Friend> friends;
            if (groupIdExtra != null && !groupIdExtra.isEmpty()) {
                // 그룹 모드: 해당 그룹 멤버만
                friends = TestData.getGroupMembersExcluding(groupIdExtra, myId);
            } else {
                friends = TestData.getFriends(myId);
            }
            showFriendPickerDialog(friends);
        } else {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;
            db.collection("users").document(user.getUid())
                .collection("friends").get()
                .addOnSuccessListener(snap -> {
                    List<Friend> friends = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String uid  = doc.getString("uid");
                        String nick = doc.getString("nickname");
                        String email = doc.getString("email");
                        friends.add(new Friend(uid != null ? uid : doc.getId(), nick, email));
                    }
                    showFriendPickerDialog(friends);
                });
        }
    }

    private void showFriendPickerDialog(List<Friend> friends) {
        if (friends.isEmpty()) {
            Toast.makeText(this, "추가할 친구가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> addedIds = new ArrayList<>();
        for (String[] p : selectedParticipants) addedIds.add(p[0]);

        List<Friend> available = new ArrayList<>();
        for (Friend f : friends) {
            if (!addedIds.contains(f.getUid())) available.add(f);
        }

        if (available.isEmpty()) {
            Toast.makeText(this, "모든 친구가 이미 추가되었습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[available.size()];
        boolean[] checked = new boolean[available.size()];
        for (int i = 0; i < available.size(); i++) {
            names[i] = available.get(i).getNickname();
        }

        new AlertDialog.Builder(this)
            .setTitle("참여자 추가")
            .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("추가", (dialog, which) -> {
                for (int i = 0; i < available.size(); i++) {
                    if (checked[i]) {
                        Friend f = available.get(i);
                        selectedParticipants.add(new String[]{f.getUid(), f.getNickname()});
                        addParticipantCircle(f.getUid(), f.getNickname());
                    }
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void addParticipantCircle(String uid, String nickname) {
        ImageView avatar = new ImageView(this);
        int sizePx = dp(40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        if (layoutScheduleParticipants.getChildCount() > 0) {
            lp.setMarginStart(dp(-8));
        } else {
            lp.setMarginEnd(dp(4));
        }
        avatar.setLayoutParams(lp);
        avatar.setBackgroundResource(R.drawable.bg_profile_circle);
        avatar.setImageResource(TestData.getProfileDrawable(uid));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setContentDescription(nickname);
        layoutScheduleParticipants.addView(avatar);
    }

    private void setupCreateButton() {
        findViewById(R.id.btn_create_schedule).setOnClickListener(v -> createSchedule());
    }

    private void createSchedule() {
        EditText etName = findViewById(R.id.et_schedule_name);
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "일정 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rangeStart == null) {
            Toast.makeText(this, "날짜를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupIdExtra = getIntent().getStringExtra("groupId");
        boolean isGroupMode = groupIdExtra != null && !groupIdExtra.isEmpty();

        // 테스트 모드: Schedule 객체 생성 후 runtimeSchedules에 저장
        android.content.SharedPreferences prefs =
                getSharedPreferences("auth", android.content.Context.MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            String testId = prefs.getString("test_id", "");
            Schedule newSc = new Schedule();
            String runtimeId = "runtime_" + System.currentTimeMillis();
            newSc.setScheduleId(runtimeId);
            newSc.setTitle(name);
            newSc.setCategory(selectedCategory != null ? selectedCategory : "");
            newSc.setStartTime(new com.google.firebase.Timestamp(startCalendar.getTime()));
            newSc.setEndTime(new com.google.firebase.Timestamp(endCalendar.getTime()));
            newSc.setCreatedBy(testId);

            List<String> participants = new ArrayList<>();
            participants.add(testId);
            for (String[] p : selectedParticipants) {
                if (!participants.contains(p[0])) participants.add(p[0]);
            }
            if (isGroupMode) {
                newSc.setGroupId(groupIdExtra);
            } else if (!selectedGroupIds.isEmpty()) {
                newSc.setGroupId(selectedGroupIds.get(0));
            }
            newSc.setParticipants(participants);
            TestData.runtimeSchedules.add(newSc);

            if (isGroupMode && !selectedParticipants.isEmpty()) {
                // 참여자가 있을 때만 지도 페이지로 이동
                android.content.Intent mapIntent =
                        new android.content.Intent(this, MapActivity.class);
                mapIntent.putExtra("groupId", groupIdExtra);
                mapIntent.putExtra("scheduleId", runtimeId);
                mapIntent.putExtra("participantUids", participants.toArray(new String[0]));
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "일정이 생성되었습니다.", Toast.LENGTH_SHORT).show();
            }
            finish();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        TextView btnCreate = findViewById(R.id.btn_create_schedule);
        btnCreate.setEnabled(false);

        // 그룹 모드: 해당 그룹을 sharedGroups에 포함
        if (isGroupMode && !selectedGroupIds.contains(groupIdExtra)) {
            selectedGroupIds.add(groupIdExtra);
        }

        List<String> participants = new ArrayList<>();
        participants.add(user.getUid());
        for (String[] p : selectedParticipants) {
            if (!participants.contains(p[0])) participants.add(p[0]);
        }

        Map<String, Object> schedule = new HashMap<>();
        schedule.put("title", name);
        schedule.put("startTime", new com.google.firebase.Timestamp(startCalendar.getTime()));
        schedule.put("endTime", new com.google.firebase.Timestamp(endCalendar.getTime()));
        schedule.put("category", selectedCategory != null ? selectedCategory : "");
        schedule.put("sharedGroups", selectedGroupIds);
        schedule.put("participants", participants);
        schedule.put("createdBy", user.getUid());
        schedule.put("createdAt", FieldValue.serverTimestamp());

        db.collection("schedules").add(schedule)
                .addOnSuccessListener(ref -> {
                    if (isGroupMode) {
                        android.content.Intent mapIntent =
                                new android.content.Intent(this, MapActivity.class);
                        mapIntent.putExtra("scheduleId", ref.getId());
                        mapIntent.putExtra("groupId", groupIdExtra);
                        startActivity(mapIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "일정이 생성되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class ScheduleCalendarAdapter extends BaseAdapter {

        private final List<Calendar> days = new ArrayList<>();

        ScheduleCalendarAdapter() { buildDays(); }

        private void buildDays() {
            days.clear();
            Calendar cal = (Calendar) displayCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1;
            for (int i = 0; i < firstDow; i++) days.add(null);
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int d = 1; d <= maxDay; d++) {
                Calendar day = (Calendar) cal.clone();
                day.set(Calendar.DAY_OF_MONTH, d);
                days.add(day);
            }
            while (days.size() % 7 != 0) days.add(null);
        }

        @Override public void notifyDataSetChanged() { buildDays(); super.notifyDataSetChanged(); }
        @Override public int getCount() { return days.size(); }
        @Override public Object getItem(int pos) { return days.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            TextView tv;
            if (convertView instanceof TextView) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(CreateScheduleActivity.this);
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(11f);
                GridView.LayoutParams lp = new GridView.LayoutParams(
                        GridView.LayoutParams.MATCH_PARENT, dp(30));
                tv.setLayoutParams(lp);
            }

            Calendar day = days.get(position);
            tv.setTag(day);

            if (day == null) {
                tv.setText("");
                tv.setBackground(null);
                return tv;
            }

            tv.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

            int dow = day.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SUNDAY)        tv.setTextColor(Color.parseColor("#FF5858"));
            else if (dow == Calendar.SATURDAY) tv.setTextColor(Color.parseColor("#526AE4"));
            else                               tv.setTextColor(Color.parseColor("#383838"));

            boolean isRangeStart = rangeStart != null && isSameDay(day, rangeStart);
            boolean isRangeEnd   = rangeEnd   != null && isSameDay(day, rangeEnd);
            boolean inRange      = rangeStart != null && rangeEnd != null
                    && !day.before(rangeStart) && !day.after(rangeEnd);

            if (isRangeStart || isRangeEnd) {
                tv.setBackgroundResource(R.drawable.bg_today_circle);
                tv.setTextColor(Color.WHITE);
            } else if (inRange) {
                tv.setBackgroundColor(Color.parseColor("#EAE9FF"));
            } else {
                tv.setBackground(null);
            }

            return tv;
        }

        private boolean isSameDay(Calendar a, Calendar b) {
            return a.get(Calendar.YEAR)  == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
        }
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
