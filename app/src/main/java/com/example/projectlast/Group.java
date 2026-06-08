package com.example.projectlast;

import java.util.List;

public class Group {
    private String groupId;
    private String groupName;
    private String leadMemberName;
    private int memberCount;
    private String startDate;

    public Group(String groupId, String groupName, String leadMemberName, int memberCount, String startDate) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.leadMemberName = leadMemberName;
        this.memberCount = memberCount;
        this.startDate = startDate;
    }

    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getLeadMemberName() { return leadMemberName; }
    public int getMemberCount() { return memberCount; }
    public String getStartDate() { return startDate; }

    public String getMembersText() {
        if (memberCount <= 1) return leadMemberName;
        return leadMemberName + " 외 " + (memberCount - 1) + "인";
    }
}
