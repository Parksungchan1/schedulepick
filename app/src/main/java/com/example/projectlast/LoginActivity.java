package com.example.projectlast;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        printKeyHash();

        etEmail    = findViewById(R.id.et_id);
        etPassword = findViewById(R.id.et_password);
        btnLogin   = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> login());

        findViewById(R.id.tv_signup).setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));

        findViewById(R.id.tv_find_pw).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "이메일을 먼저 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(u ->
                            Toast.makeText(this, "비밀번호 재설정 이메일을 발송했습니다.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "발송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void printKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature sig : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(sig.toByteArray());
                String hash = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim();
                Log.d("KeyHash", "KeyHash: " + hash);
                Toast.makeText(this, "키해시: " + hash, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("KeyHash", "Error: " + e);
        }
    }

    private void login() {
        String input = etEmail.getText().toString().trim();
        String pw    = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "이메일을 입력해주세요!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pw)) {
            Toast.makeText(this, "비밀번호를 입력해주세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 테스트 계정: Firebase 없이 로컬에서 바로 통과
        String testNick = null, testAddr = null, testEmail = null;
        if (pw.equals("1234")) {
            // TestData에 등록된 계정인지 확인 (getNickname이 id 그대로 반환하면 미등록)
            String nick = TestData.getNickname(input);
            if (!nick.equals(input)) {
                testNick  = nick;
                testAddr  = TestData.getAddress(input);
                testEmail = TestData.getEmail(input);
            }
        }
        if (testNick != null) {
            getSharedPreferences("auth", MODE_PRIVATE).edit()
                    .putBoolean("test_login", true)
                    .putString("test_id", input)
                    .putString("test_nickname", testNick)
                    .putString("test_address", testAddr)
                    .putString("test_email", testEmail)
                    .apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("로그인 중...");

        mAuth.signInWithEmailAndPassword(input, pw)
                .addOnSuccessListener(result -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("로그인");
                    Toast.makeText(this, "로그인 실패: 이메일 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
                });
    }
}
