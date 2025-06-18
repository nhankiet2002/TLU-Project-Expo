package com.cse441.tluprojectexpo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.auth.LoginActivity;
import com.cse441.tluprojectexpo.auth.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OpenActivity extends AppCompatActivity {

    private Button btnLogIn, btnSignUp;
    private FirebaseAuth mAuth;

    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_IS_GUEST_MODE = "isGuestMode"; // Khai báo KEY_IS_GUEST_MODE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);

        mAuth = FirebaseAuth.getInstance();

        btnLogIn = findViewById(R.id.btnLogIn);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_LOGIN, false);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false); // Đọc trạng thái khách

        // Ưu tiên kiểm tra chế độ khách
        if (isGuestMode) {
            // Nếu đang ở chế độ khách, vào MainActivity luôn
            Intent intent = new Intent(OpenActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (currentUser != null && rememberMe) {
            // Nếu không phải khách, và đã đăng nhập + nhớ tài khoản
            Intent intent = new Intent(OpenActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        // Nếu không có cả hai điều kiện trên, sẽ ở lại màn hình open.xml
    }
}