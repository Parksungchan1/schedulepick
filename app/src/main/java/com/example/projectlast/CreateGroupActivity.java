package com.example.projectlast;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity {

    private ImageView ivGroupPhoto;
    private TextView tvPhotoHint, tvRepresentative;
    private EditText etGroupName, etAffiliation, etBaseAddress;
    private LinearLayout layoutMembers;
    private TextView chipCompany, chipSchool, chipTeam, chipOther;
    private View colorPink, colorPurple, colorGreen, colorYellow, colorBlue;

    private TextView selectedChip = null;
    private View selectedColorDot = null;
    private String selectedCategory = "";
    private String selectedColorHex = "";
    private Uri selectedPhotoUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId = "";
    private String currentUserNickname = "";
    private boolean isTestLogin = false;

    private final List<String[]> addedMembers = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        ivGroupPhoto     = findViewById(R.id.iv_group_photo);
        tvPhotoHint      = findViewById(R.id.tv_photo_hint);
        tvRepresentative = findViewById(R.id.tv_representative);
        etGroupName      = findViewById(R.id.et_group_name);
        etAffiliation    = findViewById(R.id.et_affiliation);
        etBaseAddress    = findViewById(R.id.et_base_address);
        layoutMembers    = findViewById(R.id.layout_members);
        chipCompany      = findViewById(R.id.chip_company);
        chipSchool       = findViewById(R.id.chip_school);
        chipTeam         = findViewById(R.id.chip_team);
        chipOther        = findViewById(R.id.chip_other);
        colorPink        = findViewById(R.id.color_pink);
        colorPurple      = findViewById(R.id.color_purple_dot);
        colorGreen       = findViewById(R.id.color_green);
        colorYellow      = findViewById(R.id.color_yellow);
        colorBlue        = findViewById(R.id.color_blue);

        setupImagePicker();
        setupListeners();
        loadCurrentUser();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedPhotoUri = result.getData().getData();
                    ivGroupPhoto.setImageURI(selectedPhotoUri);
                    ivGroupPhoto.setVisibility(View.VISIBLE);
                    tvPhotoHint.setVisibility(View.GONE);
                }
            }
        );
    }

    private void setupListeners() {
        // 로고 클릭 → 뒤로가기 (홈)
        View ivLogo = findViewById(R.id.iv_logo);
        View tvLogo = findViewById(R.id.tv_logo);
        if (ivLogo != null) ivLogo.setOnClickListener(v -> finish());
        if (tvLogo != null) tvLogo.setOnClickListener(v -> finish());

        findViewById(R.id.btn_load_photo).setOnClickListener(v -> openGallery());
        findViewById(R.id.fl_photo_area).setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_delete_photo).setOnClickListener(v -> {
            selectedPhotoUri = null;
            ivGroupPhoto.setImageURI(null);
            ivGroupPhoto.setVisibility(View.GONE);
            tvPhotoHint.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btn_search_address).setOnClickListener(v ->
            etBaseAddress.requestFocus()
        );
        findViewById(R.id.btn_add_member).setOnClickListener(v -> showMemberPickerDialog());

        chipCompany.setOnClickListener(v -> selectChip(chipCompany, "회사"));
        chipSchool.setOnClickListener(v  -> selectChip(chipSchool, "학교"));
        chipTeam.setOnClickListener(v    -> selectChip(chipTeam, "팀모임"));
        chipOther.setOnClickListener(v   -> selectChip(chipOther, "기타"));

        colorPink.setOnClickListener(v   -> selectColor(colorPink, "#FFA6A6"));
        colorPurple.setOnClickListener(v -> selectColor(colorPurple, "#E08AFF"));
        colorGreen.setOnClickListener(v  -> selectColor(colorGreen, "#54C05A"));
        colorYellow.setOnClickListener(v -> selectColor(colorYellow, "#FFD151"));
        colorBlue.setOnClickListener(v   -> selectColor(colorBlue, "#4FA1FF"));

        findViewById(R.id.btn_create_group).setOnClickListener(v -> createGroup());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showMemberPickerDialog() {
        if (isTestLogin) {
            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            String myId = prefs.getString("test_id", "");
            showFriendPickerDialog(TestData.getFriends(myId));
        } else {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;
            db.collection("users").document(user.getUid())
                .collection("friends").get()
                .addOnSuccessListener(snap -> {
                    List<Friend> friends = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String uid      = doc.getString("uid");
                        String nickname = doc.getString("nickname");
                        String email    = doc.getString("email");
                        friends.add(new Friend(uid != null ? uid : doc.getId(), nickname, email));
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
        for (String[] m : addedMembers) addedIds.add(m[0]);

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
            .setTitle("멤버 추가")
            .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("추가", (dialog, which) -> {
                for (int i = 0; i < available.size(); i++) {
                    if (checked[i]) {
                        Friend f = available.get(i);
                        addedMembers.add(new String[]{f.getUid(), f.getNickname()});
                        addMemberView(f.getUid(), f.getNickname());
                    }
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void selectChip(TextView chip, String category) {
        if (selectedChip != null) {
            selectedChip.setBackgroundResource(R.drawable.bg_group_category_chip);
            selectedChip.setTextColor(Color.BLACK);
        }
        if (selectedChip == chip) {
            selectedChip = null;
            selectedCategory = "";
            return;
        }
        chip.setBackgroundResource(R.drawable.bg_group_category_chip_selected);
        chip.setTextColor(Color.WHITE);
        selectedChip = chip;
        selectedCategory = category;
    }

    private void selectColor(View dot, String colorHex) {
        if (selectedColorDot != null) {
            selectedColorDot.setPadding(0, 0, 0, 0);
            selectedColorDot.setAlpha(1f);
        }
        if (selectedColorDot == dot) {
            selectedColorDot = null;
            selectedColorHex = "";
            return;
        }
        dot.setAlpha(0.6f);
        selectedColorDot = dot;
        selectedColorHex = colorHex;
    }

    private void loadCurrentUser() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        isTestLogin = prefs.getBoolean("test_login", false);

        if (isTestLogin) {
            currentUserId       = prefs.getString("test_id", "");
            currentUserNickname = prefs.getString("test_nickname", "");
            tvRepresentative.setText(currentUserNickname);
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        currentUserId = user.getUid();

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserNickname = doc.getString("nickname");
                        if (currentUserNickname == null) currentUserNickname = "";
                        tvRepresentative.setText(currentUserNickname);
                    }
                });
    }

    public void addMemberView(String memberId, String memberName) {
        LinearLayout memberItem = new LinearLayout(this);
        memberItem.setOrientation(LinearLayout.VERTICAL);
        memberItem.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().density * 55),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int)(getResources().getDisplayMetrics().density * 6));
        memberItem.setLayoutParams(lp);

        ImageView profileImg = new ImageView(this);
        int sizePx = (int)(getResources().getDisplayMetrics().density * 25);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(sizePx, sizePx);
        profileImg.setLayoutParams(imgLp);
        profileImg.setBackgroundResource(R.drawable.bg_profile_circle);
        profileImg.setScaleType(ImageView.ScaleType.CENTER_CROP);

        TextView tvName = new TextView(this);
        tvName.setText(memberName);
        tvName.setTextSize(10f);
        tvName.setTextColor(Color.BLACK);
        tvName.setGravity(Gravity.CENTER);
        tvName.setPadding(0, 4, 0, 0);

        memberItem.addView(profileImg);
        memberItem.addView(tvName);
        memberItem.setTag(memberId);

        int addBtnIndex = layoutMembers.indexOfChild(
                layoutMembers.findViewById(R.id.btn_add_member));
        layoutMembers.addView(memberItem, addBtnIndex);
    }

    private void createGroup() {
        String name        = etGroupName.getText().toString().trim();
        String affiliation = etAffiliation.getText().toString().trim();
        String address     = etBaseAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "그룹 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "그룹 분류를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "로그인 정보를 확인해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isTestLogin) {
            String runtimeGroupId = "runtime_group_" + System.currentTimeMillis();
            int memberCount = 1 + addedMembers.size();
            Group newGroup = new Group(runtimeGroupId, name, currentUserNickname, memberCount, "2026.06");
            TestData.runtimeGroups.add(newGroup);
            // 멤버 UIDs 저장
            List<String> memberUids = new ArrayList<>();
            memberUids.add(currentUserId);
            for (String[] m : addedMembers) memberUids.add(m[0]);
            TestData.runtimeGroupMembers.put(runtimeGroupId, memberUids);
            Toast.makeText(this, "그룹이 생성되었습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView btnCreate = findViewById(R.id.btn_create_group);
        btnCreate.setEnabled(false);

        List<String> members = new ArrayList<>();
        members.add(currentUserId);
        for (String[] m : addedMembers) members.add(m[0]);

        Map<String, Object> group = new HashMap<>();
        group.put("groupName", name);
        group.put("affiliation", affiliation);
        group.put("baseAddress", address);
        group.put("category", selectedCategory);
        group.put("color", selectedColorHex.isEmpty() ? "#827EFF" : selectedColorHex);
        group.put("leaderId", currentUserId);
        group.put("members", members);
        group.put("createdAt", FieldValue.serverTimestamp());

        db.collection("groups").add(group)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "그룹이 생성되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
