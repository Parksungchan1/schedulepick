package com.example.projectlast;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdate;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelManager;

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

public class MapFragment extends Fragment {

    private static final String KAKAO_REST_API_KEY = "a5feba87e934244e4860bfa4ac42107c";

    private LinearLayout layoutLocationInputs;
    private FrameLayout  flMapContainer;
    private TextView     tvMapPlaceholder;
    private LinearLayout layoutCenterInfo;
    private TextView     tvCenterAddress;
    private LinearLayout layoutCafesSection;
    private TextView     tvCafeCount;
    private RecyclerView rvCafes;

    private MapView  mapView;
    private KakaoMap kakaoMap;

    private final List<EditText> locationInputList = new ArrayList<>();
    private final List<CafeItem> cafeList = new ArrayList<>();
    private CafeAdapter cafeAdapter;

    private double centerLat = 0, centerLng = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutLocationInputs = view.findViewById(R.id.layout_location_inputs);
        flMapContainer       = view.findViewById(R.id.fl_map_container);
        tvMapPlaceholder     = view.findViewById(R.id.tv_map_placeholder);
        layoutCenterInfo     = view.findViewById(R.id.layout_center_info);
        tvCenterAddress      = view.findViewById(R.id.tv_center_address);
        layoutCafesSection   = view.findViewById(R.id.layout_cafes_section);
        tvCafeCount          = view.findViewById(R.id.tv_cafe_count);
        rvCafes              = view.findViewById(R.id.rv_cafes);

        initMapView();

        addLocationInput("");
        addLocationInput("");

        view.findViewById(R.id.btn_add_location).setOnClickListener(v -> addLocationInput(""));
        view.findViewById(R.id.btn_find_place).setOnClickListener(v -> findMeetingPoint());

        cafeAdapter = new CafeAdapter(cafeList);
        rvCafes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCafes.setAdapter(cafeAdapter);
    }

    // ── 카카오맵 초기화 ──────────────────────────────────────────────────────

    private void initMapView() {
        if (!MyApplication.kakaoMapAvailable) {
            tvMapPlaceholder.setText("지도는 실제 기기에서 사용 가능합니다");
            return;
        }

        mapView = new MapView(requireActivity());

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

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        flMapContainer.addView(mapView, lp);

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {}

            @Override
            public void onMapError(Exception error) {
                if (getContext() != null) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                            "지도 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map;
            }
        });
    }

    // ── 위치 입력 행 동적 추가 ───────────────────────────────────────────────

    private void addLocationInput(String prefill) {
        LinearLayout row = new LinearLayout(getContext());
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvNum = new TextView(getContext());
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        numLp.setMarginEnd(dp(8));
        tvNum.setLayoutParams(numLp);
        tvNum.setGravity(Gravity.CENTER);
        tvNum.setTextSize(11f);
        tvNum.setTextColor(Color.WHITE);
        tvNum.setBackgroundResource(R.drawable.bg_today_circle);
        tvNum.setText(String.valueOf(locationInputList.size() + 1));

        EditText et = new EditText(getContext());
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        et.setLayoutParams(etLp);
        et.setHint("장소 또는 주소 입력");
        et.setHintTextColor(Color.parseColor("#BABABA"));
        et.setTextColor(Color.parseColor("#000000"));
        et.setTextSize(12f);
        et.setBackgroundResource(R.drawable.bg_search_bar);
        et.setPadding(dp(10), dp(6), dp(10), dp(6));
        et.setText(prefill);
        et.setSingleLine(true);

        TextView tvDel = new TextView(getContext());
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        delLp.setMarginStart(dp(6));
        tvDel.setLayoutParams(delLp);
        tvDel.setGravity(Gravity.CENTER);
        tvDel.setText("✕");
        tvDel.setTextSize(12f);
        tvDel.setTextColor(Color.parseColor("#888888"));
        tvDel.setVisibility(locationInputList.size() >= 1 ? View.VISIBLE : View.INVISIBLE);
        tvDel.setOnClickListener(v -> {
            if (locationInputList.size() <= 2) {
                Toast.makeText(getContext(), "최소 2개 위치가 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            locationInputList.remove(et);
            layoutLocationInputs.removeView(row);
            refreshInputNumbers();
        });

        row.addView(tvNum);
        row.addView(et);
        row.addView(tvDel);
        layoutLocationInputs.addView(row);
        locationInputList.add(et);

        if (locationInputList.size() == 2 && layoutLocationInputs.getChildCount() > 0) {
            View firstRow = layoutLocationInputs.getChildAt(0);
            if (firstRow instanceof LinearLayout) {
                View delBtn = ((LinearLayout) firstRow).getChildAt(2);
                delBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void refreshInputNumbers() {
        for (int i = 0; i < layoutLocationInputs.getChildCount(); i++) {
            View row = layoutLocationInputs.getChildAt(i);
            if (row instanceof LinearLayout) {
                View numView = ((LinearLayout) row).getChildAt(0);
                if (numView instanceof TextView) {
                    ((TextView) numView).setText(String.valueOf(i + 1));
                }
            }
        }
    }

    // ── 약속장소 찾기 메인 로직 ──────────────────────────────────────────────

    private void findMeetingPoint() {
        List<String> addresses = new ArrayList<>();
        for (EditText et : locationInputList) {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) addresses.add(text);
        }
        if (addresses.size() < 2) {
            Toast.makeText(getContext(), "최소 2개 위치를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "위치를 검색 중입니다...", Toast.LENGTH_SHORT).show();

        double[] latSum = {0};
        double[] lngSum = {0};
        int[]    found  = {0};
        AtomicInteger remaining = new AtomicInteger(addresses.size());

        for (String address : addresses) {
            geocodeAddress(address, (lat, lng) -> {
                if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                    latSum[0] += lat;
                    lngSum[0] += lng;
                    found[0]++;
                }
                if (remaining.decrementAndGet() == 0) {
                    if (found[0] == 0) {
                        requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                "위치를 찾을 수 없습니다. 검색어를 확인해주세요.",
                                Toast.LENGTH_SHORT).show());
                        return;
                    }
                    double avgLat = latSum[0] / found[0];
                    double avgLng = lngSum[0] / found[0];
                    requireActivity().runOnUiThread(() -> {
                        showCenterOnMap(avgLat, avgLng);
                        searchNearbyCafes(avgLat, avgLng);
                        reverseGeocode(avgLat, avgLng);
                    });
                }
            });
        }
    }

    // ── Kakao Local API: 키워드 → 좌표 ──────────────────────────────────────

    interface CoordCallback {
        void onResult(double lat, double lng);
    }

    private void geocodeAddress(String address, CoordCallback callback) {
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(address, "UTF-8");
                URL url = new URL(
                    "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "KakaoAK " + KAKAO_REST_API_KEY);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray documents = json.getJSONArray("documents");
                if (documents.length() > 0) {
                    JSONObject first = documents.getJSONObject(0);
                    double lat = Double.parseDouble(first.getString("y"));
                    double lng = Double.parseDouble(first.getString("x"));
                    callback.onResult(lat, lng);
                } else {
                    callback.onResult(Double.NaN, Double.NaN);
                }
            } catch (Exception e) {
                callback.onResult(Double.NaN, Double.NaN);
            }
        }).start();
    }

    // ── 카카오맵: 중간지점 핀 표시 ──────────────────────────────────────────

    private void showCenterOnMap(double lat, double lng) {
        this.centerLat = lat;
        this.centerLng = lng;
        tvMapPlaceholder.setVisibility(View.GONE);

        if (kakaoMap == null) return;

        // 카메라 이동
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCenterPosition(
                LatLng.from(lat, lng), 15);
        kakaoMap.moveCamera(cameraUpdate);

        // 중간지점 마커
        LabelManager lm = kakaoMap.getLabelManager();
        if (lm != null) {
            LabelLayer layer = lm.getLayer();
            if (layer != null) {
                layer.removeAll();
                LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_add)));
                layer.addLabel(LabelOptions.from("center", LatLng.from(lat, lng))
                        .setStyles(styles));
            }
        }
    }

    // ── Kakao Local API: 주변 카페 검색 ─────────────────────────────────────

    private void searchNearbyCafes(double lat, double lng) {
        new Thread(() -> {
            try {
                String url = "https://dapi.kakao.com/v2/local/search/keyword.json"
                        + "?query=" + URLEncoder.encode("카페", "UTF-8")
                        + "&y=" + lat
                        + "&x=" + lng
                        + "&radius=1000"
                        + "&sort=distance"
                        + "&size=15";

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

                JSONObject json = new JSONObject(sb.toString());
                JSONArray documents = json.getJSONArray("documents");

                List<CafeItem> results = new ArrayList<>();
                for (int i = 0; i < documents.length(); i++) {
                    JSONObject d = documents.getJSONObject(i);
                    CafeItem cafe = new CafeItem();
                    cafe.id       = d.optString("id");
                    cafe.name     = d.optString("place_name");
                    cafe.address  = d.optString("address_name");
                    cafe.category = simplifyCategory(d.optString("category_name"));
                    cafe.phone    = d.optString("phone");
                    cafe.lat      = Double.parseDouble(d.optString("y", "0"));
                    cafe.lng      = Double.parseDouble(d.optString("x", "0"));
                    cafe.distance = Double.parseDouble(d.optString("distance", "0"));
                    results.add(cafe);
                }

                requireActivity().runOnUiThread(() -> {
                    cafeList.clear();
                    cafeList.addAll(results);
                    cafeAdapter.notifyDataSetChanged();
                    tvCafeCount.setText(results.size() + "개");
                    layoutCafesSection.setVisibility(View.VISIBLE);
                    if (results.isEmpty()) {
                        Toast.makeText(getContext(), "주변 1km 내 카페를 찾을 수 없습니다.",
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "카페 검색 중 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Kakao Local API: 좌표 → 주소 변환 ───────────────────────────────────

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json"
                        + "?x=" + lng + "&y=" + lat;
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

                JSONObject json = new JSONObject(sb.toString());
                JSONArray documents = json.getJSONArray("documents");
                if (documents.length() > 0) {
                    JSONObject address = documents.getJSONObject(0).optJSONObject("address");
                    String addressName = address != null
                            ? address.optString("address_name", "알 수 없음") : "알 수 없음";
                    requireActivity().runOnUiThread(() -> {
                        tvCenterAddress.setText("중간 지점: " + addressName);
                        layoutCenterInfo.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvCenterAddress.setText("중간 지점 좌표: " +
                            String.format("%.4f, %.4f", lat, lng));
                    layoutCenterInfo.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    // ── 카카오맵 앱으로 장소 열기 ────────────────────────────────────────────

    private void openKakaoMapPlace(String placeId, String placeName) {
        Intent kakaoIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("kakaomap://place?id=" + placeId));
        kakaoIntent.setPackage("net.daum.android.map");
        if (requireActivity().getPackageManager().resolveActivity(kakaoIntent, 0) != null) {
            startActivity(kakaoIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://place.map.kakao.com/" + placeId)));
        }
    }

    private String simplifyCategory(String full) {
        if (full == null || full.isEmpty()) return "";
        String[] parts = full.split(">");
        return parts[parts.length - 1].trim();
    }

    private String formatDistance(double meters) {
        return meters < 1000 ? (int) meters + "m" : String.format("%.1fkm", meters / 1000.0);
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // ── MapView 라이프사이클 ──────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.finish();
            flMapContainer.removeView(mapView);
            mapView = null;
        }
        kakaoMap = null;
        locationInputList.clear();
    }

    // ── 데이터 클래스 ─────────────────────────────────────────────────────────

    static class CafeItem {
        String id, name, address, category, phone;
        double lat, lng, distance;
    }

    // ── 카페 RecyclerView 어댑터 ──────────────────────────────────────────────

    private class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.VH> {

        private final List<CafeItem> items;

        CafeAdapter(List<CafeItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cafe_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CafeItem cafe = items.get(position);
            holder.tvName.setText(cafe.name);
            holder.tvDistance.setText(formatDistance(cafe.distance));
            holder.tvCategory.setText(cafe.category);
            holder.tvAddress.setText(cafe.address);
            holder.itemView.setOnClickListener(v -> openKakaoMapPlace(cafe.id, cafe.name));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView  tvName, tvDistance, tvCategory, tvAddress;

            VH(@NonNull View itemView) {
                super(itemView);
                ivThumb    = itemView.findViewById(R.id.iv_cafe_thumb);
                tvName     = itemView.findViewById(R.id.tv_cafe_name);
                tvDistance = itemView.findViewById(R.id.tv_cafe_distance);
                tvCategory = itemView.findViewById(R.id.tv_cafe_category);
                tvAddress  = itemView.findViewById(R.id.tv_cafe_address);
            }
        }
    }
}
