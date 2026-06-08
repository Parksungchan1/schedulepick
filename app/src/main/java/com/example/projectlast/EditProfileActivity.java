package com.example.projectlast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private EditText etNickname, etAddress, etOrg;
    private TextView btnSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isTestLogin = false;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        ivProfile  = findViewById(R.id.iv_profile);
        etNickname = findViewById(R.id.et_nickname);
        etAddress  = findViewById(R.id.et_address);
        etOrg      = findViewById(R.id.et_org);
        btnSave    = findViewById(R.id.btn_save);

        setupImagePicker();

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_change_photo).setOnClickListener(v -> openGallery());
        ivProfile.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveProfile());
        findViewById(R.id.tv_logout).setOnClickListener(v -> logout());

        loadCurrentProfile();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    ivProfile.setImageURI(uri);
                }
            }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadCurrentProfile() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        isTestLogin = prefs.getBoolean("test_login", false);

        if (isTestLogin) {
            etNickname.setText(prefs.getString("test_nickname", ""));
            etAddress.setText(prefs.getString("test_address", ""));
            etOrg.setText("");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etNickname.setText(doc.getString("nickname"));
                        etAddress.setText(doc.getString("address"));
                        etOrg.setText(doc.getString("org"));
                    }
                });
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();
        String address  = etAddress.getText().toString().trim();
        String org      = etOrg.getText().toString().trim();

        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isTestLogin) {
            getSharedPreferences("auth", MODE_PRIVATE).edit()
                .putString("test_nickname", nickname)
                .putString("test_address", address)
                .apply();
            Toast.makeText(this, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        btnSave.setEnabled(false);
        btnSave.setText("저장 중...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("nickname", nickname);
        updates.put("address", address);
        updates.put("org", org);

        db.collection("users").document(user.getUid()).update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("저장");
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit().putBoolean("test_login", false).apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
