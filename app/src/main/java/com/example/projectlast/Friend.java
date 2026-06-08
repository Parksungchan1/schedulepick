package com.example.projectlast;

public class Friend {
    private String uid;
    private String nickname;
    private String email;

    public Friend() {}

    public Friend(String uid, String nickname, String email) {
        this.uid = uid;
        this.nickname = nickname;
        this.email = email;
    }

    public String getUid() { return uid; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
}
