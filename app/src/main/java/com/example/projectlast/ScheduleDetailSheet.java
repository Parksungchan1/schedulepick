package com.example.projectlast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ScheduleDetailSheet extends BottomSheetDialogFragment {

    private static final String KEY_TITLE        = "title";
    private static final String KEY_DATE         = "date";
    private static final String KEY_CATEGORY     = "category";
    private static final String KEY_LOCATION     = "location";
    private static final String KEY_PARTICIPANTS  = "participants";

    public static ScheduleDetailSheet newInstance(Schedule schedule) {
        ScheduleDetailSheet sheet = new ScheduleDetailSheet();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE,    schedule.getTitle()    != null ? schedule.getTitle()    : "");
        args.putString(KEY_DATE,     schedule.getFormattedDate());
        args.putString(KEY_CATEGORY, schedule.getCategory() != null ? schedule.getCategory() : "");
        args.putString(KEY_LOCATION, schedule.getLocation() != null ? schedule.getLocation() : "");

        // 참여자 UID 목록을 닉네임으로 변환
        List<String> uids = schedule.getParticipants();
        if (uids != null && !uids.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String uid : uids) {
                String name = TestData.getNickname(uid);
                // TestData에 없으면 uid 그대로 표시
                names.add(name.equals(uid) ? uid : name);
            }
            args.putString(KEY_PARTICIPANTS, android.text.TextUtils.join(", ", names));
        } else {
            args.putString(KEY_PARTICIPANTS, "");
        }

        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_schedule_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        ((TextView) view.findViewById(R.id.tv_detail_title)).setText(args.getString(KEY_TITLE));
        ((TextView) view.findViewById(R.id.tv_detail_date)).setText(args.getString(KEY_DATE));
        ((TextView) view.findViewById(R.id.tv_detail_category)).setText(args.getString(KEY_CATEGORY));

        String location = args.getString(KEY_LOCATION, "");
        LinearLayout layoutLocation = view.findViewById(R.id.layout_detail_location);
        if (location != null && !location.isEmpty()) {
            ((TextView) view.findViewById(R.id.tv_detail_location)).setText(location);
            layoutLocation.setVisibility(View.VISIBLE);
        } else {
            layoutLocation.setVisibility(View.GONE);
        }

        String participants = args.getString(KEY_PARTICIPANTS, "");
        LinearLayout layoutParticipants = view.findViewById(R.id.layout_detail_participants);
        if (participants != null && !participants.isEmpty()) {
            ((TextView) view.findViewById(R.id.tv_detail_participants)).setText(participants);
            layoutParticipants.setVisibility(View.VISIBLE);
        } else {
            layoutParticipants.setVisibility(View.GONE);
        }

        view.findViewById(R.id.btn_detail_close).setOnClickListener(v -> dismiss());
    }
}
