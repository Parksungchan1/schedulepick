package com.example.projectlast;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MapActivity extends AppCompatActivity {

    private static final String KAKAO_REST_API_KEY = "a5feba87e934244e4860bfa4ac42107c";

    private String groupId, scheduleId;

    private LinearLayout layoutMembers;
    private LinearLayout layoutMapArea;
    private FrameLayout  flMapContainer;
    private TextView     tvCenterAddress;
    private LinearLayout layoutCafesSection;
    private RecyclerView rvCafes;
    private LinearLayout layoutDirectInput;
    private EditText     etDirectSearch;

    private MapView  mapView;
    private KakaoMap kakaoMap;

    private final List<EditText>  memberAddressInputs = new ArrayList<>();
    private final List<PlaceItem> placeList           = new ArrayList<>();
    private PlaceAdapter placeAdapter;
    private int    selectedPlaceIndex = -1;
    private String selectedPlaceName  = "";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        groupId    = getIntent().getStringExtra("groupId");
        scheduleId = getIntent().getStringExtra("scheduleId");
        db         = FirebaseFirestore.getInstance();

        layoutMembers      = findViewById(R.id.layout_members);
        layoutMapArea      = findViewById(R.id.layout_map_area);
        flMapContainer     = findViewById(R.id.fl_map_container);
        tvCenterAddress    = findViewById(R.id.tv_center_address);
        layoutCafesSection = findViewById(R.id.layout_cafes_section);
        rvCafes            = findViewById(R.id.rv_cafes);
        layoutDirectInput  = findViewById(R.id.layout_direct_input);
        etDirectSearch     = findViewById(R.id.et_direct_search);

        placeAdapter = new PlaceAdapter(placeList, idx -> {
            selectedPlaceIndex = idx;
            selectedPlaceName  = (idx >= 0 && idx < placeList.size()) ? placeList.get(idx).name : "";
        });
        rvCafes.setLayoutManager(new LinearLayoutManager(this));
        rvCafes.setAdapter(placeAdapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_find_center).setOnClickListener(v -> {
            layoutDirectInput.setVisibility(View.GONE);
            findMeetingPoint();
        });

        findViewById(R.id.btn_direct_input).setOnClickListener(v ->
                layoutDirectInput.setVisibility(View.VISIBLE));

        findViewById(R.id.btn_direct_search).setOnClickListener(v -> {
            String query = etDirectSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "장소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            searchDirectPlace(query);
        });

        findViewById(R.id.btn_map_complete).setOnClickListener(v -> completeSelection());

        initMapView();
        loadGroupMembers();
    }

    // ── 카카오맵 초기화 ──────────────────────────────────────────────────────

    private void initMapView() {
        if (!MyApplication.kakaoMapAvailable) {
            // 에뮬레이터(x86_64)에서는 지도 대신 안내 문구 표시
            android.widget.TextView tvMapNotice = new android.widget.TextView(this);
            tvMapNotice.setText("지도는 실제 Android 기기에서 지원됩니다");
            tvMapNotice.setTextSize(13f);
            tvMapNotice.setTextColor(android.graphics.Color.parseColor("#888888"));
            tvMapNotice.setGravity(android.view.Gravity.CENTER);
            tvMapNotice.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            flMapContainer.addView(tvMapNotice);
            return;
        }

        mapView = new MapView(this);
        mapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        flMapContainer.addView(mapView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mapView.start(new MapLifeCycleCallback() {
            @Override public void onMapDestroy() {}
            @Override public void onMapError(Exception e) {
                android.util.Log.e("MapActivity", "KakaoMap error: " + e);
                runOnUiThread(() -> {
                    android.widget.TextView tvErr = new android.widget.TextView(MapActivity.this);
                    tvErr.setText("지도 오류: " + e.getMessage());
                    tvErr.setTextSize(11f);
                    tvErr.setTextColor(android.graphics.Color.parseColor("#FF5858"));
                    tvErr.setGravity(android.view.Gravity.CENTER);
                    tvErr.setPadding(16, 16, 16, 16);
                    tvErr.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
                    flMapContainer.removeAllViews();
                    flMapContainer.addView(tvErr);
                });
            }
        }, new KakaoMapReadyCallback() {
            @Override public void onMapReady(@NonNull KakaoMap map) { kakaoMap = map; }
        });
    }

    // ── 그룹 멤버 로드 ──────────────────────────────────────────────────────

    private String[] getParticipantUids() {
        String[] uids = getIntent().getStringArrayExtra("participantUids");
        if (uids != null && uids.length > 0) return uids;
        // fallback: 전체 그룹 멤버
        List<Friend> all = TestData.getGroupMembers();
        String[] result = new String[all.size()];
        for (int i = 0; i < all.size(); i++) result[i] = all.get(i).getUid();
        return result;
    }

    private void loadGroupMembers() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            for (String uid : getParticipantUids()) {
                addMemberRow(TestData.getNickname(uid), TestData.getAddress(uid));
            }
            return;
        }

        if (groupId == null) return;

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<String> uids = (List<String>) doc.get("members");
                    if (uids == null) return;
                    for (String uid : uids) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name    = userDoc.getString("nickname");
                                    String address = userDoc.getString("address");
                                    addMemberRow(
                                            name    != null ? name    : "",
                                            address != null ? address : "");
                                });
                    }
                });
    }

    private void addMemberRow(String name, String prefillAddress) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(dp(70),
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tvName.setText(name);
        tvName.setTextSize(13f);
        tvName.setTextColor(Color.parseColor("#000000"));
        tvName.setTypeface(null, Typeface.BOLD);

        EditText etAddr = new EditText(this);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etAddr.setLayoutParams(etLp);
        etAddr.setText(prefillAddress);
        etAddr.setTextSize(12f);
        etAddr.setTextColor(Color.parseColor("#000000"));
        etAddr.setHintTextColor(Color.parseColor("#BABABA"));
        etAddr.setHint("출발지 입력");
        etAddr.setBackgroundResource(R.drawable.bg_search_bar);
        etAddr.setPadding(dp(10), dp(6), dp(10), dp(6));
        etAddr.setSingleLine(true);

        row.addView(tvName);
        row.addView(etAddr);
        layoutMembers.addView(row);
        memberAddressInputs.add(etAddr);
    }

    // ── 중간지점 찾기 ────────────────────────────────────────────────────────

    // ── 테스트 모드 전용: API 없이 하드코딩 좌표 사용 ────────────────────────

    private void findMeetingPointTestMode() {
        double latSum = 0, lngSum = 0;
        int count = 0;
        for (String uid : getParticipantUids()) {
            double lat = TestData.getLat(uid);
            double lng = TestData.getLng(uid);
            if (lat != 0) { latSum += lat; lngSum += lng; count++; }
        }
        if (count == 0) return;

        double avgLat = latSum / count;   // ≈ 36.8194
        double avgLng = lngSum / count;   // ≈ 127.1354

        // 지도 표시
        showOnMap(avgLat, avgLng);

        // 중간지점 주소 텍스트
        tvCenterAddress.setText("중간 지점: 충남 천안시 동남구 신부동 일대");
        layoutMapArea.setVisibility(android.view.View.VISIBLE);

        // 목업 카페 목록 표시
        placeList.clear();
        for (TestData.MockCafe cafe : TestData.getMockCafes()) {
            PlaceItem p = new PlaceItem();
            p.name     = cafe.name;
            p.address  = cafe.address;
            p.lat      = cafe.lat;
            p.lng      = cafe.lng;
            p.distance = cafe.distance;
            placeList.add(p);
        }
        placeAdapter.notifyDataSetChanged();
        layoutCafesSection.setVisibility(android.view.View.VISIBLE);

        Toast.makeText(this, "중간지점을 찾았습니다!", Toast.LENGTH_SHORT).show();
    }

    private void findMeetingPoint() {
        // ── 테스트 모드: API 없이 하드코딩 좌표로 바로 처리 ──
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            findMeetingPointTestMode();
            return;
        }

        List<String> addresses = new ArrayList<>();
        for (EditText et : memberAddressInputs) {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) addresses.add(text);
        }
        if (addresses.size() < 2) {
            Toast.makeText(this, "최소 2개 위치를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 버튼 비활성화로 진행 중 표시
        TextView btnFind = findViewById(R.id.btn_find_center);
        btnFind.setEnabled(false);
        btnFind.setText("검색 중...");
        Toast.makeText(this, "위치를 검색 중입니다...", Toast.LENGTH_SHORT).show();

        final double[] latSum = {0};
        final double[] lngSum = {0};
        final int[]    found  = {0};
        AtomicInteger remaining = new AtomicInteger(addresses.size());

        for (String address : addresses) {
            geocodeAddress(address, (lat, lng) -> {
                synchronized (latSum) {
                    if (!Double.isNaN(lat)) {
                        latSum[0] += lat;
                        lngSum[0] += lng;
                        found[0]++;
                    }
                }
                if (remaining.decrementAndGet() == 0) {
                    runOnUiThread(() -> {
                        btnFind.setEnabled(true);
                        btnFind.setText("중간지점 찾기");
                    });
                    if (found[0] == 0) {
                        runOnUiThread(() ->
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("주소 검색 실패")
                                .setMessage(
                                    "입력한 주소의 좌표를 찾을 수 없습니다.\n\n" +
                                    "가능한 원인:\n" +
                                    "• 인터넷 연결 없음\n" +
                                    "• 카카오 REST API 키 오류\n" +
                                    "• 주소 형식 오류\n\n" +
                                    "※ 에뮬레이터는 인터넷 연결이 필요합니다.\n" +
                                    "  실제 기기에서 실행하면 정상 동작합니다.")
                                .setPositiveButton("확인", null)
                                .show());
                        return;
                    }
                    double avgLat = latSum[0] / found[0];
                    double avgLng = lngSum[0] / found[0];
                    runOnUiThread(() -> {
                        showOnMap(avgLat, avgLng);
                        searchNearbyPlaces(avgLat, avgLng);
                        reverseGeocode(avgLat, avgLng);
                        layoutMapArea.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
    }

    // ── 직접 장소 입력 ───────────────────────────────────────────────────────

    private void searchDirectPlace(String query) {
        Toast.makeText(this, "검색 중...", Toast.LENGTH_SHORT).show();
        geocodeAddress(query, (lat, lng) -> {
            if (Double.isNaN(lat)) {
                runOnUiThread(() ->
                        Toast.makeText(this, "장소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show());
                return;
            }
            runOnUiThread(() -> {
                PlaceItem directItem = new PlaceItem();
                directItem.name     = query;
                directItem.address  = query;
                directItem.lat      = lat;
                directItem.lng      = lng;
                directItem.distance = 0;

                // 직접 입력 항목을 목록 맨 앞에 추가
                placeList.removeIf(p -> p.distance == 0);
                placeList.add(0, directItem);
                placeAdapter.notifyDataSetChanged();

                showOnMap(lat, lng);
                searchNearbyPlaces(lat, lng);
                tvCenterAddress.setText("선택 장소: " + query);
                layoutMapArea.setVisibility(View.VISIBLE);
                layoutCafesSection.setVisibility(View.VISIBLE);
            });
        });
    }

    // ── 카카오맵 마커 표시 ───────────────────────────────────────────────────

    private void showOnMap(double lat, double lng) {
        if (kakaoMap == null) return;
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), 15));
        com.kakao.vectormap.label.LabelManager lm = kakaoMap.getLabelManager();
        if (lm != null) {
            LabelLayer layer = lm.getLayer();
            if (layer != null) {
                layer.removeAll();
                LabelStyles styles = lm.addLabelStyles(
                        LabelStyles.from(LabelStyle.from(R.drawable.ic_add)));
                layer.addLabel(LabelOptions.from("center", LatLng.from(lat, lng)).setStyles(styles));
            }
        }
    }

    // ── 주변 카페 검색 ───────────────────────────────────────────────────────

    private void searchNearbyPlaces(double lat, double lng) {
        new Thread(() -> {
            try {
                String url = "https://dapi.kakao.com/v2/local/search/keyword.json"
                        + "?query=" + URLEncoder.encode("카페", "UTF-8")
                        + "&y=" + lat + "&x=" + lng
                        + "&radius=1000&sort=distance&size=15";

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("Authorization", "KakaoAK " + KAKAO_REST_API_KEY);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray docs = new JSONObject(sb.toString()).getJSONArray("documents");
                List<PlaceItem> results = new ArrayList<>();
                for (int i = 0; i < docs.length(); i++) {
                    JSONObject d = docs.getJSONObject(i);
                    PlaceItem p  = new PlaceItem();
                    p.name     = d.optString("place_name");
                    p.address  = d.optString("address_name");
                    p.lat      = Double.parseDouble(d.optString("y", "0"));
                    p.lng      = Double.parseDouble(d.optString("x", "0"));
                    p.distance = Double.parseDouble(d.optString("distance", "0"));
                    results.add(p);
                }

                runOnUiThread(() -> {
                    // 직접 입력 항목은 유지
                    List<PlaceItem> directItems = new ArrayList<>();
                    for (PlaceItem p : placeList) {
                        if (p.distance == 0) directItems.add(p);
                    }
                    placeList.clear();
                    placeList.addAll(directItems);
                    placeList.addAll(results);
                    placeAdapter.notifyDataSetChanged();
                    layoutCafesSection.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "장소 검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── 역지오코딩 ───────────────────────────────────────────────────────────

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=" + lng + "&y=" + lat;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("Authorization", "KakaoAK " + KAKAO_REST_API_KEY);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray docs = new JSONObject(sb.toString()).getJSONArray("documents");
                if (docs.length() > 0) {
                    JSONObject addr = docs.getJSONObject(0).optJSONObject("address");
                    String addressName = addr != null ? addr.optString("address_name", "") : "";
                    runOnUiThread(() -> tvCenterAddress.setText("중간 지점: " + addressName));
                }
            } catch (Exception e) {
                runOnUiThread(() -> tvCenterAddress.setText(""));
            }
        }).start();
    }

    // ── 좌표 변환 ────────────────────────────────────────────────────────────

    interface CoordCallback {
        void onResult(double lat, double lng);
    }

    private void geocodeAddress(String address, CoordCallback callback) {
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(address, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection)
                        new URL("https://dapi.kakao.com/v2/local/search/keyword.json?query=" + encoded)
                                .openConnection();
                conn.setRequestProperty("Authorization", "KakaoAK " + KAKAO_REST_API_KEY);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                int httpCode = conn.getResponseCode();
                android.util.Log.d("MapActivity", "geocode [" + address + "] HTTP=" + httpCode);

                if (httpCode != 200) {
                    // 에러 응답 본문 읽기 (디버그용)
                    java.io.InputStream errStream = conn.getErrorStream();
                    if (errStream != null) {
                        BufferedReader er = new BufferedReader(
                                new InputStreamReader(errStream, "UTF-8"));
                        StringBuilder errSb = new StringBuilder();
                        String l;
                        while ((l = er.readLine()) != null) errSb.append(l);
                        er.close();
                        android.util.Log.e("MapActivity",
                                "API error " + httpCode + ": " + errSb);
                    }
                    callback.onResult(Double.NaN, Double.NaN);
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray docs = new JSONObject(sb.toString()).getJSONArray("documents");
                android.util.Log.d("MapActivity",
                        "geocode [" + address + "] results=" + docs.length());

                if (docs.length() > 0) {
                    JSONObject first = docs.getJSONObject(0);
                    double lat = Double.parseDouble(first.getString("y"));
                    double lng = Double.parseDouble(first.getString("x"));
                    android.util.Log.d("MapActivity",
                            "geocode [" + address + "] → lat=" + lat + " lng=" + lng);
                    callback.onResult(lat, lng);
                } else {
                    android.util.Log.w("MapActivity", "geocode [" + address + "] → 결과 없음");
                    callback.onResult(Double.NaN, Double.NaN);
                }
            } catch (Exception e) {
                android.util.Log.e("MapActivity", "geocode 예외 [" + address + "]: " + e);
                callback.onResult(Double.NaN, Double.NaN);
            }
        }).start();
    }

    // ── 완료 처리 ────────────────────────────────────────────────────────────

    private void completeSelection() {
        if (selectedPlaceIndex < 0 || selectedPlaceName.isEmpty()) {
            Toast.makeText(this, "장소를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 장소명 + 주소 결합
        String locationStr = selectedPlaceName;
        if (selectedPlaceIndex >= 0 && selectedPlaceIndex < placeList.size()) {
            PlaceItem place = placeList.get(selectedPlaceIndex);
            if (place.address != null && !place.address.isEmpty()
                    && !place.address.equals(place.name)) {
                locationStr = place.name + " (" + place.address + ")";
            }
        }

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        if (prefs.getBoolean("test_login", false)) {
            // 런타임 일정의 location 필드 업데이트
            if (scheduleId != null) {
                for (Schedule sc : TestData.runtimeSchedules) {
                    if (scheduleId.equals(sc.getScheduleId())) {
                        sc.setLocation(locationStr);
                        break;
                    }
                }
            }
            Toast.makeText(this, "약속 장소: " + selectedPlaceName, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (scheduleId == null) {
            Toast.makeText(this, "일정 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalLocation = locationStr;
        db.collection("schedules").document(scheduleId)
                .update("location", finalLocation)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "약속 장소: " + selectedPlaceName, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    @Override protected void onResume()  { super.onResume();  if (mapView != null) mapView.resume(); }
    @Override protected void onPause()   { super.onPause();   if (mapView != null) mapView.pause();  }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) { mapView.finish(); mapView = null; }
        kakaoMap = null;
    }

    // ── 데이터 모델 ──────────────────────────────────────────────────────────

    static class PlaceItem {
        String name, address;
        double lat, lng, distance;
    }

    // ── 장소 선택 어댑터 ─────────────────────────────────────────────────────

    private static class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.VH> {

        interface OnSelectListener { void onSelect(int index); }

        private final List<PlaceItem>  items;
        private final OnSelectListener listener;
        private int selectedIndex = -1;

        PlaceAdapter(List<PlaceItem> items, OnSelectListener listener) {
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cafe_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            PlaceItem item = items.get(position);
            h.tvName.setText(item.name);
            h.tvAddress.setText(item.address);

            if (item.distance > 0) {
                h.tvDistance.setText(item.distance < 1000
                        ? (int) item.distance + "m"
                        : String.format("%.1fkm", item.distance / 1000.0));
                h.tvDistance.setVisibility(View.VISIBLE);
            } else {
                h.tvDistance.setText("직접 입력");
                h.tvDistance.setVisibility(View.VISIBLE);
            }

            h.tvCategory.setVisibility(View.GONE);

            boolean selected = (position == selectedIndex);
            h.itemView.setBackgroundColor(selected
                    ? Color.parseColor("#EAE9FF")
                    : Color.WHITE);

            h.itemView.setOnClickListener(v -> {
                int prev = selectedIndex;
                selectedIndex = position;
                if (prev >= 0) notifyItemChanged(prev);
                notifyItemChanged(position);
                if (listener != null) listener.onSelect(position);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDistance, tvCategory, tvAddress;
            VH(@NonNull View v) {
                super(v);
                tvName     = v.findViewById(R.id.tv_cafe_name);
                tvDistance = v.findViewById(R.id.tv_cafe_distance);
                tvCategory = v.findViewById(R.id.tv_cafe_category);
                tvAddress  = v.findViewById(R.id.tv_cafe_address);
            }
        }
    }
}
