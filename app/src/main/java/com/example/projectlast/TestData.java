package com.example.projectlast;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestData {

    public static final String GROUP_ID = "test_group_1";

    // 런타임에 사용자가 생성한 일정/그룹 (앱 세션 동안 유지)
    public static final List<Schedule> runtimeSchedules = new ArrayList<>();
    public static final List<Group> runtimeGroups = new ArrayList<>();
    // runtime 그룹 멤버 UIDs: groupId → List<uid>
    public static final java.util.Map<String, List<String>> runtimeGroupMembers = new java.util.HashMap<>();

    // uid → [nickname, address, email, lat, lng]
    private static final Map<String, String[]> USERS = new LinkedHashMap<>();
    static {
        USERS.put("park", new String[]{"박성찬", "충남 천안시 동남구 만남로 43",           "park@test.com", "36.8151", "127.1139"});
        USERS.put("yoon", new String[]{"윤어진", "충남 천안시 동남구 충절로 295-14",       "yoon@test.com", "36.8089", "127.1442"});
        USERS.put("koo",  new String[]{"구민정", "충남 천안시 서북구 두정로 289 두정역",   "koo@test.com",  "36.8471", "127.1313"});
        USERS.put("hong", new String[]{"홍길동", "충남 천안시 동남구 대흥로 239",          "hong@test.com", "36.8066", "127.1522"});
    }

    public static int getProfileDrawable(String id) {
        switch (id) {
            case "park": return R.drawable.ic_profile_cat;
            case "yoon": return R.drawable.ic_profile_dog;
            case "koo":  return R.drawable.ic_profile_rabbit;
            case "hong": return R.drawable.ic_profile_bear;
            default:     return R.drawable.ic_default_profile;
        }
    }

    public static String getNickname(String id) {
        String[] d = USERS.get(id); return d != null ? d[0] : id;
    }
    public static String getAddress(String id) {
        String[] d = USERS.get(id); return d != null ? d[1] : "";
    }
    public static String getEmail(String id) {
        String[] d = USERS.get(id); return d != null ? d[2] : "";
    }
    public static double getLat(String id) {
        String[] d = USERS.get(id); return d != null ? Double.parseDouble(d[3]) : 0;
    }
    public static double getLng(String id) {
        String[] d = USERS.get(id); return d != null ? Double.parseDouble(d[4]) : 0;
    }

    // ── 테스트용 목업 카페 목록 (천안 중심부) ──────────────────────────────
    public static class MockCafe {
        public final String name, address;
        public final double lat, lng, distance;
        public MockCafe(String name, String address, double lat, double lng, double distance) {
            this.name = name; this.address = address;
            this.lat = lat; this.lng = lng; this.distance = distance;
        }
    }

    public static List<MockCafe> getMockCafes() {
        List<MockCafe> list = new ArrayList<>();
        list.add(new MockCafe("스타벅스 천안신부점",   "충남 천안시 동남구 신부동 432-3",  36.8194, 127.1148, 120));
        list.add(new MockCafe("투썸플레이스 천안터미널점","충남 천안시 동남구 대흥로 108",  36.8155, 127.1195, 280));
        list.add(new MockCafe("메가커피 천안청수점",    "충남 천안시 서북구 청수14로 59",   36.8302, 127.1265, 410));
        list.add(new MockCafe("이디야커피 천안두정점",  "충남 천안시 서북구 두정로 124",    36.8388, 127.1310, 530));
        list.add(new MockCafe("할리스 천안아산역점",    "충남 천안시 서북구 봉정로 88",     36.7966, 127.1044, 680));
        list.add(new MockCafe("공차 천안신부점",        "충남 천안시 동남구 신부동 398",    36.8171, 127.1132, 210));
        return list;
    }

    // ── 그룹 ──────────────────────────────────────────────────
    public static List<Group> getGroups() {
        List<Group> list = new ArrayList<>();
        list.add(new Group(GROUP_ID, "팀 프로젝트 팀", "박성찬", 4, "2026.05"));
        list.addAll(runtimeGroups);
        return list;
    }

    // ── 그룹 내 다른 멤버 목록 (나 제외) ────────────────────────
    public static List<Friend> getGroupMembersExcluding(String groupId, String myId) {
        if (GROUP_ID.equals(groupId)) {
            return getFriends(myId); // 기본 그룹은 전체 친구
        }
        List<String> memberUids = runtimeGroupMembers.get(groupId);
        if (memberUids == null) return new ArrayList<>();
        List<Friend> result = new ArrayList<>();
        for (String uid : memberUids) {
            if (!uid.equals(myId)) {
                result.add(new Friend(uid, getNickname(uid), getEmail(uid)));
            }
        }
        return result;
    }

    // ── 친구 목록 (나를 제외한 나머지 3명) ──────────────────────
    public static List<Friend> getFriends(String myId) {
        List<Friend> list = new ArrayList<>();
        for (Map.Entry<String, String[]> e : USERS.entrySet()) {
            if (!e.getKey().equals(myId)) {
                list.add(new Friend(e.getKey(), e.getValue()[0], e.getValue()[2]));
            }
        }
        return list;
    }

    // ── 그룹 멤버 전원 ────────────────────────────────────────
    public static List<Friend> getGroupMembers() {
        List<Friend> list = new ArrayList<>();
        for (Map.Entry<String, String[]> e : USERS.entrySet()) {
            list.add(new Friend(e.getKey(), e.getValue()[0], e.getValue()[2]));
        }
        return list;
    }

    // ── 일정 목록 ─────────────────────────────────────────────
    // 백석대학교 주소 (팀모임 일정 공통 장소)
    private static final String LOC_BAEKSEOK = "충남 천안시 서북구 백석로 1";
    // 천안 종합버스터미널 (신부동 터미널)
    private static final String LOC_TERMINAL = "충남 천안시 동남구 신부동 313-1";

    public static List<Schedule> getSchedules(String userId) {
        List<Schedule> list = new ArrayList<>();
        int y = Calendar.getInstance().get(Calendar.YEAR);

        switch (userId) {
            case "yoon":
                list.add(s("y1", "1차 팀플 발표 준비", y,6,3, 14,0, y,6,3, 17,0,  "팀모임", GROUP_ID, "yoon", LOC_BAEKSEOK));
                list.add(s("y2", "팀 회의",            y,6,5, 18,0, y,6,5, 19,0,  "팀모임", GROUP_ID, "yoon", LOC_BAEKSEOK));
                list.add(s("y3", "2차 팀플 작업",       y,6,10,14,0, y,6,10,18,0,  "팀모임", GROUP_ID, "yoon", LOC_BAEKSEOK));
                list.add(s("y4", "팀플 중간 점검",      y,6,14,16,0, y,6,14,17,30, "팀모임", GROUP_ID, "yoon", LOC_BAEKSEOK));
                list.add(s("y5", "최종 발표 준비",      y,6,18,13,0, y,6,18,16,0,  "팀모임", GROUP_ID, "yoon", LOC_BAEKSEOK));
                list.add(s("y6", "발표 리허설",         y,6,20,10,0, y,6,20,12,0,  "팀모임", GROUP_ID, "yoon", LOC_TERMINAL));
                break;
            case "koo":
                list.add(s("k1", "랩미팅",             y,6,3, 15,0, y,6,3, 16,0,  "학교", null, "koo", null));
                list.add(s("k2", "논문 리뷰",           y,6,6, 14,0, y,6,6, 15,30, "학교", null, "koo", null));
                list.add(s("k3", "연구 발표 준비",      y,6,10,10,0, y,6,10,12,0,  "학교", null, "koo", null));
                list.add(s("k4", "랩미팅",             y,6,17,15,0, y,6,17,16,0,  "학교", null, "koo", null));
                list.add(s("k5", "지도교수 면담",       y,6,20,11,0, y,6,20,12,0,  "학교", null, "koo", null));
                list.add(s("k6", "학회 논문 제출 준비", y,6,24,13,0, y,6,24,17,0,  "학교", null, "koo", null));
                break;
            case "park":
                list.add(s("p1", "팀 회의",            y,6,5, 18,0, y,6,5, 19,0,  "팀모임", GROUP_ID, "park", LOC_BAEKSEOK));
                list.add(s("p2", "2차 팀플 작업",       y,6,10,14,0, y,6,10,18,0,  "팀모임", GROUP_ID, "park", LOC_BAEKSEOK));
                list.add(s("p3", "최종 발표 준비",      y,6,18,13,0, y,6,18,16,0,  "팀모임", GROUP_ID, "park", LOC_BAEKSEOK));
                break;
            case "hong":
                list.add(s("h1", "팀 회의",            y,6,5, 18,0, y,6,5, 19,0,  "팀모임", GROUP_ID, "hong", LOC_BAEKSEOK));
                list.add(s("h2", "2차 팀플 작업",       y,6,10,14,0, y,6,10,18,0,  "팀모임", GROUP_ID, "hong", LOC_BAEKSEOK));
                list.add(s("h3", "최종 발표 준비",      y,6,18,13,0, y,6,18,16,0,  "팀모임", GROUP_ID, "hong", LOC_BAEKSEOK));
                break;
        }
        return list;
    }

    // 특정 날짜의 일정만 필터 (개인 일정만)
    public static List<Schedule> getSchedulesForDate(String userId, int year, int month, int day) {
        List<Schedule> result = new ArrayList<>();
        for (Schedule sc : getSchedules(userId)) {
            if (sc.getStartTime() == null) continue;
            Calendar c = Calendar.getInstance();
            c.setTime(sc.getStartTime().toDate());
            if (c.get(Calendar.YEAR) == year
                    && c.get(Calendar.MONTH) + 1 == month
                    && c.get(Calendar.DAY_OF_MONTH) == day) {
                result.add(sc);
            }
        }
        return result;
    }

    // 내가 참여한 모든 일정(개인+그룹) — participants 기준, 중복 제거
    public static List<Schedule> getAllSchedulesForUser(String userId) {
        List<Schedule> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String uid : USERS.keySet()) {
            for (Schedule sc : getSchedules(uid)) {
                if (sc.getParticipants() == null
                        || !sc.getParticipants().contains(userId)) continue;
                String key = sc.getTitle() + "|"
                        + (sc.getStartTime() != null
                            ? sc.getStartTime().toDate().getTime() : "0");
                if (seen.add(key)) result.add(sc);
            }
        }
        // 런타임 생성 일정 포함
        for (Schedule sc : runtimeSchedules) {
            if (sc.getParticipants() == null
                    || !sc.getParticipants().contains(userId)) continue;
            String key = sc.getTitle() + "|"
                    + (sc.getStartTime() != null
                        ? sc.getStartTime().toDate().getTime() : "0");
            if (seen.add(key)) result.add(sc);
        }
        return result;
    }

    // 특정 날짜의 개인+그룹 통합 일정
    public static List<Schedule> getAllSchedulesForUserForDate(
            String userId, int year, int month, int day) {
        List<Schedule> result = new ArrayList<>();
        for (Schedule sc : getAllSchedulesForUser(userId)) {
            if (sc.getStartTime() == null) continue;
            Calendar c = Calendar.getInstance();
            c.setTime(sc.getStartTime().toDate());
            if (c.get(Calendar.YEAR) == year
                    && c.get(Calendar.MONTH) + 1 == month
                    && c.get(Calendar.DAY_OF_MONTH) == day) {
                result.add(sc);
            }
        }
        return result;
    }

    // 특정 그룹의 일정만 필터 (runtime 그룹 포함)
    public static List<Schedule> getGroupSchedules(String groupId) {
        List<Schedule> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String uid : USERS.keySet()) {
            for (Schedule sc : getSchedules(uid)) {
                if (!groupId.equals(sc.getGroupId())) continue;
                String key = sc.getTitle() + "|"
                        + (sc.getStartTime() != null ? sc.getStartTime().toDate().getTime() : "");
                if (seen.add(key)) result.add(sc);
            }
        }
        for (Schedule sc : runtimeSchedules) {
            if (!groupId.equals(sc.getGroupId())) continue;
            String key = sc.getTitle() + "|"
                    + (sc.getStartTime() != null ? sc.getStartTime().toDate().getTime() : "");
            if (seen.add(key)) result.add(sc);
        }
        return result;
    }

    // 그룹 캘린더용: 그룹 일정만, 제목+시작시각 기준 중복 제거
    public static List<Schedule> getGroupSchedules() {
        List<Schedule> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String uid : USERS.keySet()) {
            for (Schedule sc : getSchedules(uid)) {
                if (sc.getGroupId() == null) continue;
                String key = sc.getTitle() + "|"
                        + (sc.getStartTime() != null ? sc.getStartTime().toDate().getTime() : "");
                if (seen.add(key)) result.add(sc);
            }
        }
        // 런타임 생성 그룹 일정 포함
        for (Schedule sc : runtimeSchedules) {
            if (sc.getGroupId() == null) continue;
            String key = sc.getTitle() + "|"
                    + (sc.getStartTime() != null ? sc.getStartTime().toDate().getTime() : "");
            if (seen.add(key)) result.add(sc);
        }
        return result;
    }

    // ── 헬퍼 ──────────────────────────────────────────────────
    private static Schedule s(String id, String title,
            int sy, int sm, int sd, int sh, int smin,
            int ey, int em, int ed, int eh, int emin,
            String category, String groupId, String createdBy, String location) {
        Schedule sc = new Schedule();
        sc.setScheduleId(id);
        sc.setTitle(title);
        sc.setCategory(category);
        sc.setGroupId(groupId);
        sc.setCreatedBy(createdBy);
        sc.setLocation(location);

        Calendar start = Calendar.getInstance();
        start.set(sy, sm - 1, sd, sh, smin, 0);
        start.set(Calendar.MILLISECOND, 0);
        sc.setStartTime(new Timestamp(start.getTime()));

        Calendar end = Calendar.getInstance();
        end.set(ey, em - 1, ed, eh, emin, 0);
        end.set(Calendar.MILLISECOND, 0);
        sc.setEndTime(new Timestamp(end.getTime()));

        List<String> participants = new ArrayList<>();
        participants.add(createdBy);
        if (groupId != null) {
            for (String uid : USERS.keySet()) {
                if (!uid.equals(createdBy)) participants.add(uid);
            }
        }
        sc.setParticipants(participants);
        return sc;
    }
}
