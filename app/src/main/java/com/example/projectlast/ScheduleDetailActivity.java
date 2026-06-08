package com.example.projectlast;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ScheduleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_ID = "scheduleId";

    private TextView tvTitle, tvCategory, tvGroupPill, tvDate, tvTime, tvLocation, tvMembers, tvMemo;
    private TextView tvEdit, btnDelete;
    private FirebaseFirestore db;
    private String scheduleId;
    private String creatorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_detail);

        db         = FirebaseFirestore.getInstance();
        scheduleId = getIntent().getStringExtra(EXTRA_SCHEDULE_ID);

        tvTitle     = findViewById(R.id.tv_title);
        tvCategory  = findViewById(R.id.tv_category);
        tvGroupPill = findViewById(R.id.tv_group_pill);
        tvDate      = findViewById(R.id.tv_date);
        tvTime      = findViewById(R.id.tv_time);
        tvLocation  = findViewById(R.id.tv_location);
        tvMembers   = findViewById(R.id.tv_members);
        tvMemo      = findViewById(R.id.tv_memo);
        tvEdit      = findViewById(R.id.tv_edit);
        btnDelete   = findViewById(R.id.btn_delete);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        tvEdit.setOnClickListener(v ->
                Toast.makeText(this, "수정 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show());
        btnDelete.setOnClickListener(v -> deleteSchedule());

        if (scheduleId != null) loadSchedule();
    }

    private void loadSchedule() {
        db.collection("schedules").document(scheduleId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    tvTitle.setText(doc.getString("title"));
                    tvCategory.setText(doc.getString("category") != null ? doc.getString("category") : "개인");
                    tvDate.setText(doc.getString("date") != null ? doc.getString("date") : "-");
                    tvTime.setText(doc.getString("time") != null ? doc.getString("time") : "-");
                    tvLocation.setText(doc.getString("location") != null ? doc.getString("location") : "-");
                    tvMemo.setText(doc.getString("memo") != null ? doc.getString("memo") : "-");

                    String groupName = doc.getString("groupName");
                    if (groupName != null && !groupName.isEmpty()) {
                        tvGroupPill.setText(groupName);
                        tvGroupPill.setVisibility(View.VISIBLE);
                    }

                    creatorId = doc.getString("creatorId");
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (uid != null && uid.equals(creatorId)) {
                        tvEdit.setVisibility(View.VISIBLE);
                        btnDelete.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void deleteSchedule() {
        db.collection("schedules").document(scheduleId).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
