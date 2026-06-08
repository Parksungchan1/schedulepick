package com.example.projectlast;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class Schedule {
    private String scheduleId;
    private String title;
    private Timestamp startTime;
    private Timestamp endTime;
    private String category;
    private String groupId;
    private String createdBy;
    private String location;
    private List<String> participants;

    public Schedule() {}

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String v) { scheduleId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp v) { startTime = v; }
    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp v) { endTime = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String v) { groupId = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> v) { participants = v; }

    public String getFormattedDate() {
        if (startTime == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA);
        return sdf.format(startTime.toDate());
    }
}
