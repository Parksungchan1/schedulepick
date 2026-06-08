package com.example.projectlast;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etPasswordConfirm, etAddress;
    private TextView btnCheckEmail, btnSignup;
    private FirebaseAuth     mAuth;
    private FirebaseFirestore db;

    private boolean isEmailChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        etEmail           = findViewById(R.id.et_id);
        etPassword        = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        etAddress         = findViewById(R.id.et_address);
        btnCheckEmail     = findViewById(R.id.btn_check_id);
        btnSignup         = findViewById(R.id.btn_signup);

        // 이메일 형식 확인 (중복 확인 버튼)
        btnCheckEmail.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "올바른 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            isEmailChecked = true;
            Toast.makeText(this, "이메일 확인 완료.", Toast.LENGTH_SHORT).show();
        });

        // 주소 검색 (카카오 주소 API - 추후 연동)
        findViewById(R.id.btn_search_address).setOnClickListener(v ->
                Toast.makeText(this, "주소를 직접 입력해주세요.", Toast.LENGTH_SHORT).show());

        // 회원가입
        btnSignup.setOnClickListener(v -> signup());
    }

    private void signup() {
        String email     = etEmail.getText().toString().trim();
        String pw        = etPassword.getText().toString().trim();
        String pwConfirm = etPasswordConfirm.getText().toString().trim();
        String address   = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "올바른 이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isEmailChecked) {
            Toast.makeText(this, "이메일 확인 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pw.length() < 8) {
            Toast.makeText(this, "8자리 이상 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pw.equals(pwConfirm)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignup.setEnabled(false);
        btnSignup.setText("가입 중...");

        mAuth.createUserWithEmailAndPassword(email, pw)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) return;
                    saveUserToFirestore(user.getUid(), email, address);
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    btnSignup.setText("회원가입");
                    Toast.makeText(this, "가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToFirestore(String uid, String email, String address) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("address", address);
        user.put("nickname", email.split("@")[0]);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                    btnSignup.setText("회원가입");
                    Toast.makeText(this, "정보 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
