package com.example.projectlast;

import com.google.firebase.Timestamp;

public class FriendRequest {
    private String requestId;
    private String fromUid;
    private String toUid;
    private String fromNickname;
    private String fromEmail;
    private String status;
    private Timestamp timestamp;

    public FriendRequest() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String v) { requestId = v; }
    public String getFromUid() { return fromUid; }
    public void setFromUid(String v) { fromUid = v; }
    public String getToUid() { return toUid; }
    public void setToUid(String v) { toUid = v; }
    public String getFromNickname() { return fromNickname; }
    public void setFromNickname(String v) { fromNickname = v; }
    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String v) { fromEmail = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp v) { timestamp = v; }
}
