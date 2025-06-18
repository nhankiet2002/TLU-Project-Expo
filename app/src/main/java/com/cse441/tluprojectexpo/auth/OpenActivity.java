package com.cse441.tluprojectexpo.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // Import TextView

import androidx.appcompat.app.AppCompatActivity;

import com.cse441.tluprojectexpo.MainActivity;
import com.cse441.tluprojectexpo.R;
import com.cse441.tluprojectexpo.auth.LoginActivity;
import com.cse441.tluprojectexpo.auth.RegisterActivity;
import com.cse441.tluprojectexpo.utils.GuestModeHandler; // Import lớp tiện ích
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OpenActivity extends AppCompatActivity {

    private Button btnLogIn, btnSignUp;
    private TextView txtGuestMode; // Khai báo TextView cho chế độ khách
    private FirebaseAuth mAuth;

    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_REMEMBER_LOGIN = "remember_login";
    private static final String KEY_IS_GUEST_MODE = "isGuestMode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);

        mAuth = FirebaseAuth.getInstance();

        btnLogIn = findViewById(R.id.btnLogIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtGuestMode = findViewById(R.id.txtGuestMode); // Ánh xạ TextView từ activity_open.xml

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

        // Xử lý sự kiện cho txtGuestMode trên màn hình OpenActivity
        if (txtGuestMode != null) { // Đảm bảo View này tồn tại trong layout
            txtGuestMode.setOnClickListener(v -> {
                GuestModeHandler.enterGuestMode(OpenActivity.this, mAuth);
                finish(); // Đóng OpenActivity sau khi chuyển hướng
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_LOGIN, false);
        boolean isGuestMode = prefs.getBoolean(KEY_IS_GUEST_MODE, false);

        // Ưu tiên kiểm tra chế độ khách
        if (isGuestMode) {
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